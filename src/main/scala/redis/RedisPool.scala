package redis

import akka.actor.{Props, ActorRef, ActorSystem}

import scala.concurrent.stm._
import redis.actors.RedisClientActor
import java.net.InetSocketAddress
import scala.concurrent.{Future, ExecutionContext}
import redis.protocol.RedisReply
import redis.commands.Transactions


case class RedisServer(host: String = "localhost",
                       port: Int = 6379,
                       password: Option[String] = None,
                       db: Option[Int] = None,
                       active: Ref[Boolean] = Ref(false))

abstract class RedisClientPoolBaseLike(system: ActorSystem) extends RoundRobinPoolRequest {
  val redisServers: Seq[RedisServer]
  val name: String
  implicit val executionContext = system.dispatcher

  val redisConnectionPoolAll: Seq[ActorRef]

  def getConnectionsActive: Seq[ActorRef] = {
    val redisConnectionZip = redisServers zip redisConnectionPoolAll
    redisConnectionZip.collect {
      case (server, actorRef) if server.active.single.get => actorRef
    }
  }

  lazy val redisConnectionRef: Ref[Seq[ActorRef]] = Ref(getConnectionsActive)

  def redisConnectionPool: Seq[ActorRef] = {
    redisConnectionRef.single.get
  }

  def onConnect(redis: RedisCommands, server: RedisServer): Unit = {
    server.password.foreach(redis.auth(_)) // TODO log on auth failure
    server.db.foreach(redis.select)
  }

  def onConnectStatus(server: RedisServer): (Boolean) => Unit = (status: Boolean) => {
    if (server.active.single.compareAndSet(!status, status)) {
      redisConnectionRef.single.set(getConnectionsActive)
    }
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
    redisConnectionPool.foreach(redisConnection => {
      system stop redisConnection
    })
  }
}

abstract class RedisClientPoolLike(system: ActorSystem) extends RedisClientPoolBaseLike(system) {

  val redisConnectionPoolAll: Seq[ActorRef] = redisServers.map(server => {
    system.actorOf(
      Props(classOf[RedisClientActor], new InetSocketAddress(server.host, server.port), getConnectOperations(server),onConnectStatus(server))
        .withDispatcher(Redis.dispatcher),
      name + '-' + Redis.tempName()
    )
  })

}


abstract class RedisClientMutablePoolLike(system: ActorSystem) extends RedisClientPoolBaseLike(system) {

  val redisClients =
    collection.mutable.Map(
      redisServers.map(server =>
        (makeRedisClientActorKey(server), makeRedisClientActor(server))
      ):_*
  )

  def addServer(server: RedisServer) {
    val k = makeRedisClientActorKey(server)
    if (!redisClients.contains(k)) {
      redisClients.synchronized {
        if (!redisClients.contains(k)) {
          redisClients += k -> makeRedisClientActor(server)
        }
      }
    }
  }

  def removeServer(server: RedisServer) {
      val k = makeRedisClientActorKey(server)
    if (redisClients.contains(k)) {
      redisClients.synchronized {
        if (redisClients.contains(k)) {
          system stop redisClients(k)
          redisClients -= k
        }
      }
    }
  }

  override def redisConnectionPool: Seq[ActorRef] = redisClients.values.toSeq

  def makeRedisClientActorKey(server: RedisServer) = s"${server.host}:${server.port}"

  def makeRedisClientActor(server: RedisServer): ActorRef = {
    system.actorOf(
      Props(classOf[RedisClientActor], new InetSocketAddress(server.host, server.port), getConnectOperations(server),onConnectStatus(server))
        .withDispatcher(Redis.dispatcher),
      name + '-' + Redis.tempName()
    )
  }

}


case class RedisClientMutablePool(redisServers: Seq[RedisServer],
                           name: String = "RedisClientPool")
                          (implicit _system: ActorSystem) extends RedisClientMutablePoolLike(_system) with RedisCommands{

  val redisConnectionPoolAll: Seq[ActorRef] = redisServers.map(makeRedisClientActor)

}

case class RedisClientPool(redisServers: Seq[RedisServer],
                           name: String = "RedisClientPool")
                          (implicit _system: ActorSystem) extends RedisClientPoolLike(_system) with RedisCommands {
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
                                  (implicit _system:ActorSystem)
  extends SentinelMonitored(_system) with ActorRequest  with  RedisCommands with Transactions {

  val masterClient: RedisClient = withMasterAddr((ip, port) => {
    new RedisClient(ip, port, name = "SMRedisClient")
  })

  val slavesClients:RedisClientMutablePool = withSlavesAddr( slavesHostPort => {
    val slaves =  slavesHostPort.map{
      case (ip, port) =>
        new RedisServer(ip, port)
    }
    new RedisClientMutablePool(slaves, name = "SMRedisClient")
  })


  val onNewSlave = (ip: String, port: Int)  => {
    log.info(s"onNewSlave ${ip}:${port}")
    slavesClients.addServer(RedisServer(ip,port))
  }

  val onSlaveDown = (ip: String, port: Int)  => {
    log.info(s"onSlaveDown ${ip}:${port}")
    slavesClients.removeServer(RedisServer(ip,port))
  }

  val onMasterChange = (ip: String, port: Int) => {
    log.info(s"onMasterChange ${ip}:${port}")
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
    if (redisCommand.isMasterOnly || !slavesClients.redisServers.exists(_.active.single.get)) {
      masterClient.send(redisCommand)
    } else {
      slavesClients.send(redisCommand)
    }
  }
}
