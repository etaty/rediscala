package redis

import scala.concurrent.stm._
import akka.actor.{Props, ActorRef, ActorSystem}
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

abstract class RedisClientPoolLike(system: ActorSystem) extends RoundRobinPoolRequest {
  val redisServers: Seq[RedisServer]
  val name: String
  implicit val executionContext = system.dispatcher

  val redisConnectionPoolAll: Seq[ActorRef] = redisServers.map(server => {
    system.actorOf(
      Props(classOf[RedisClientActor], new InetSocketAddress(server.host, server.port), getConnectOperations(server),onConnectStatus(server))
        .withDispatcher(Redis.dispatcher),
      name + '-' + Redis.tempName()
    )
  })

  def getConnectionsActive() = {
    val redisConnectionZip = redisServers zip redisConnectionPoolAll
    redisConnectionZip.filter{ s=> 
        s._1.active.single.get
    }.map(_._2)
  }

  val redisConnectionRef:Ref[Seq[ActorRef]] =  Ref(getConnectionsActive())

  def redisConnectionPool(): Seq[ActorRef] = {
    redisConnectionRef.single.get
  }


  def onConnect(redis: RedisCommands, server: RedisServer): Unit = {
    server.password.foreach(redis.auth(_)) // TODO log on auth failure
    server.db.foreach(redis.select(_))
  }

  def onConnectStatus(server: RedisServer): (Boolean) => Unit = (status: Boolean) => {
    if ( server.active.single.compareAndSet(!status,status) ){
      redisConnectionRef.single.swap(getConnectionsActive)
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
                          (implicit _system: ActorSystem) extends RedisClientPoolLike(_system) with RedisCommands

case class RedisClientMasterSlaves(master: RedisServer,
                                   slaves: Seq[RedisServer])
                                  (implicit _system: ActorSystem) extends RedisCommands with Transactions {
  implicit val executionContext = _system.dispatcher

  val masterClient = RedisClient(master.host, master.port, master.password, master.db)

  val slavesClients = RedisClientPool(slaves)

  override def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    if (redisCommand.isMasterOnly) {
      masterClient.send(redisCommand)
    } else {
      slavesClients.send(redisCommand)
    }
  }

  def redisConnection: ActorRef = masterClient.redisConnection
}
