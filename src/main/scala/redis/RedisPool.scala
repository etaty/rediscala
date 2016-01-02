package redis

import java.net.InetSocketAddress

import akka.actor.{Props, ActorRef, ActorSystem}

import scala.concurrent.stm._
import redis.actors.RedisClientActor
import scala.concurrent.{Future, ExecutionContext}
import redis.protocol.RedisReply
import redis.commands.Transactions

case class RedisServer(host: String = "localhost",
                       port: Int = 6379,
                       password: Option[String] = None,
                       db: Option[Int] = None)


case class RedisConnection(actor: ActorRef, active: Ref[Boolean] = Ref(false))

abstract class RedisClientPoolLike(system: ActorSystem) extends RoundRobinPoolRequest {

  def redisServerConnections: scala.collection.Map[RedisServer, RedisConnection]

  val name: String
  implicit val executionContext = system.dispatcher

  val redisConnectionRef: Ref[Seq[ActorRef]] = Ref(Seq.empty)

  def getConnectionsActive: Seq[ActorRef] = {
    redisServerConnections.collect {
      case (redisServer, redisConnection) if redisConnection.active.single.get => redisConnection.actor
    }.toVector
  }

  def redisConnectionPool: Seq[ActorRef] = {
    redisConnectionRef.single.get
  }

  def onConnect(redis: RedisCommands, server: RedisServer): Unit = {
    server.password.foreach(redis.auth(_)) // TODO log on auth failure
    server.db.foreach(redis.select)
  }

  def onConnectStatus(server: RedisServer, active: Ref[Boolean]): (Boolean) => Unit = (status: Boolean) => {
    if (active.single.compareAndSet(!status, status)) {
      refreshConnections()
    }
  }

  def refreshConnections() = {
    val actives = getConnectionsActive
    redisConnectionRef.single.set(actives)
  }

  def getConnectOperations(server: RedisServer): () => Seq[Operation[_, _]] = () => {
    val self = this
    val redis = new BufferedRequest with RedisCommands {
      implicit val executionContext: ExecutionContext = self.executionContext
    }
    onConnect(redis, server)
    redis.operations.result()
  }

  /**
   * Disconnect from the server (stop the actor)
   */
  def stop() {
    redisConnectionPool.foreach { redisConnection =>
      system stop redisConnection
    }
  }

  def makeRedisConnection(server: RedisServer, defaultActive: Boolean = false) = {
    val active = Ref(defaultActive)
    (server, RedisConnection(makeRedisClientActor(server, active), active))
  }

  def makeRedisClientActor(server: RedisServer, active: Ref[Boolean]): ActorRef = {
    system.actorOf(
      Props(classOf[RedisClientActor], new InetSocketAddress(server.host, server.port), getConnectOperations(server), onConnectStatus(server, active))
        .withDispatcher(Redis.dispatcher),
      name + '-' + Redis.tempName()
    )
  }

}


case class RedisClientMutablePool(redisServers: Seq[RedisServer],
                                  name: String = "RedisClientPool")
                                 (implicit system: ActorSystem) extends RedisClientPoolLike(system) with RedisCommands {

  override val redisServerConnections = {
    val m = redisServers map { server => makeRedisConnection(server) }
    collection.mutable.Map(m: _*)
  }

  def addServer(server: RedisServer) {
    if (!redisServerConnections.contains(server)) {
      redisServerConnections.synchronized {
        if (!redisServerConnections.contains(server)) {
          redisServerConnections += makeRedisConnection(server)
        }
      }
    }
  }

  def removeServer(askServer: RedisServer) {
    if (redisServerConnections.contains(askServer)) {
      redisServerConnections.synchronized {
        redisServerConnections.get(askServer).foreach { redisServerConnection =>
          system stop redisServerConnection.actor
        }
        redisServerConnections.remove(askServer)
        refreshConnections()
      }
    }
  }


}

case class RedisClientPool(redisServers: Seq[RedisServer],
                           name: String = "RedisClientPool")
                          (implicit _system: ActorSystem) extends RedisClientPoolLike(_system) with RedisCommands {

  override val redisServerConnections = {
    redisServers.map { server =>
      makeRedisConnection(server, defaultActive = true)
    } toMap
  }

  refreshConnections()

}

case class RedisClientMasterSlaves(master: RedisServer,
                                   slaves: Seq[RedisServer])
                                  (implicit _system: ActorSystem) extends RedisCommands with Transactions {
  implicit val executionContext = _system.dispatcher

  val masterClient = RedisClient(master.host, master.port, master.password, master.db)

  val slavesClients = RedisClientPool(slaves)

  override def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    if (redisCommand.isMasterOnly || slaves.isEmpty) {
      masterClient.send(redisCommand)
    } else {
      slavesClients.send(redisCommand)
    }
  }

  def redisConnection: ActorRef = masterClient.redisConnection
}


case class SentinelMonitoredRedisClientMasterSlaves(sentinels: Seq[(String, Int)] = Seq(("localhost", 26379)),
                                                    master: String)
                                                   (implicit _system: ActorSystem)
  extends SentinelMonitored(_system) with ActorRequest with RedisCommands with Transactions {

  val masterClient: RedisClient = withMasterAddr(
    (ip, port) => {
      new RedisClient(ip, port, name = "SMRedisClient")
    })

  val slavesClients: RedisClientMutablePool = withSlavesAddr(
    slavesHostPort => {
      val slaves = slavesHostPort.map {
        case (ip, port) =>
          new RedisServer(ip, port)
      }
      new RedisClientMutablePool(slaves, name = "SMRedisClient")
    })


  val onNewSlave = (ip: String, port: Int) => {
    log.info(s"onNewSlave $ip:$port")
    slavesClients.addServer(RedisServer(ip, port))
  }

  val onSlaveDown = (ip: String, port: Int) => {
    log.info(s"onSlaveDown $ip:$port")
    slavesClients.removeServer(RedisServer(ip, port))
  }

  val onMasterChange = (ip: String, port: Int) => {
    log.info(s"onMasterChange $ip:$port")
    masterClient.reconnect(ip, port)
  }

  /**
   * Disconnect from the server (stop the actors)
   */
  def stop() = {
    masterClient.stop()
    slavesClients.stop()
    sentinelClients.values.foreach(_.stop())
  }

  def redisConnection: ActorRef = masterClient.redisConnection

  override def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    if (redisCommand.isMasterOnly || slavesClients.redisConnectionPool.isEmpty) {
      masterClient.send(redisCommand)
    } else {
      slavesClients.send(redisCommand)
    }
  }
}
