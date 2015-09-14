package redis

import akka.actor.{Props, ActorRef, ActorRefFactory}
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

abstract class RedisClientPoolLike(system: ActorRefFactory) extends RoundRobinPoolRequest {
  val redisServers: Seq[RedisServer]
  val name: String
  implicit val executionContext = system.dispatcher

  val redisConnectionPoolAll: Seq[ActorRef] = redisServers.map(server => {
    system.actorOf(
      Props(classOf[RedisClientActor], new InetSocketAddress(server.host, server.port), getConnectOperations(server), onConnectStatus(server))
        .withDispatcher(Redis.dispatcher),
      name + '-' + Redis.tempName()
    )
  })

  def getConnectionsActive: Seq[ActorRef] = {
    val redisConnectionZip = redisServers zip redisConnectionPoolAll
    redisConnectionZip.collect {
      case (server, actorRef) if server.active.single.get => actorRef
    }
  }

  val redisConnectionRef: Ref[Seq[ActorRef]] = Ref(getConnectionsActive)

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
    redisConnectionPoolAll.foreach(redisConnection => {
      system stop redisConnection
    })
  }
}

case class RedisClientPool(redisServers: Seq[RedisServer],
                           name: String = "RedisClientPool")
                          (implicit _system: ActorRefFactory) extends RedisClientPoolLike(_system) with RedisCommands

case class RedisClientMasterSlaves(master: RedisServer,
                                   slaves: Seq[RedisServer])
                                  (implicit _system: ActorRefFactory) extends RedisCommands with Transactions {
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
