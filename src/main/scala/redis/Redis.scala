package redis

import akka.actor._
import akka.util.Helpers
import redis.commands._
import scala.concurrent._
import java.net.InetSocketAddress
import redis.actors.{RedisSubscriberActorWithCallback, RedisClientActor}
import redis.api.pubsub._
import java.util.concurrent.atomic.AtomicLong
import akka.event.Logging

trait RedisCommands
  extends Keys
  with Strings
  with Hashes
  with Lists
  with Sets
  with SortedSets
  with Publish
  with Scripting
  with Connection
  with Server

abstract class RedisClientActorLike(system: ActorSystem) extends ActorRequest {
  var host: String
  var port: Int
  val name: String
  val password: Option[String] = None
  val db: Option[Int] = None
  implicit val executionContext = system.dispatcher

  val redisConnection: ActorRef = system.actorOf(
    Props(classOf[RedisClientActor], new InetSocketAddress(host, port), getConnectOperations)
      .withDispatcher(Redis.dispatcher),
    name + '-' + Redis.tempName()
  )

  def reconnect(host: String = host, port: Int = port) = {
    if (this.host != host || this.port != port) {
      this.host = host
      this.port = port
      redisConnection ! new InetSocketAddress(host, port)
    }
  }

  def onConnect(redis: RedisCommands): Unit = {
    password.foreach(redis.auth(_)) // TODO log on auth failure
    db.foreach(redis.select(_))
  }

  def getConnectOperations: () => Seq[Operation[_, _]] = () => {
    val self = this
    val redis = new BufferedRequest with RedisCommands {
      implicit val executionContext: ExecutionContext = self.executionContext
    }
    onConnect(redis)
    redis.operations.result()
  }

  /**
   * Disconnect from the server (stop the actor)
   */
  def stop() {
    system stop redisConnection
  }
}

case class RedisClient(var host: String = "localhost",
                       var port: Int = 6379,
                       name: String = "RedisClient",
                       override val password: Option[String] = None,
                       override val db: Option[Int] = None)
                      (implicit _system: ActorSystem) extends RedisClientActorLike(_system) with RedisCommands with Transactions {

}

case class RedisBlockingClient(var host: String = "localhost",
                               var port: Int = 6379,
                               name: String = "RedisBlockingClient",
                               override val password: Option[String] = None,
                               override val db: Option[Int] = None)
                              (implicit _system: ActorSystem) extends RedisClientActorLike(_system) with BLists {
}

case class RedisPubSub(
                        host: String = "localhost",
                        port: Int = 6379,
                        channels: Seq[String],
                        patterns: Seq[String],
                        onMessage: Message => Unit = _ => {},
                        onPMessage: PMessage => Unit = _ => {},
                        authPassword: Option[String] = None,
                        name: String = "RedisPubSub"
                        )(implicit system: ActorSystem) {

  val redisConnection: ActorRef = system.actorOf(
    Props(classOf[RedisSubscriberActorWithCallback],
      new InetSocketAddress(host, port), channels, patterns, onMessage, onPMessage, authPassword)
      .withDispatcher(Redis.dispatcher),
    name + '-' + Redis.tempName()
  )

  /**
   * Disconnect from the server (stop the actor)
   */
  def stop() {
    system stop redisConnection
  }

  def subscribe(channels: String*) {
    redisConnection ! SUBSCRIBE(channels: _*)
  }

  def unsubscribe(channels: String*) {
    redisConnection ! UNSUBSCRIBE(channels: _*)
  }

  def psubscribe(patterns: String*) {
    redisConnection ! PSUBSCRIBE(patterns: _*)
  }

  def punsubscribe(patterns: String*) {
    redisConnection ! PUNSUBSCRIBE(patterns: _*)
  }
}

trait SentinelCommands
  extends Sentinel

case class SentinelClient(var host: String = "localhost",
                          var port: Int = 26379,
                          onMasterChange: (String, String, Int) => Unit = (masterName: String, ip: String, port: Int) => {},
                          name: String = "SentinelClient")
                         (implicit _system: ActorSystem) extends RedisClientActorLike(_system) with SentinelCommands {
  val system: ActorSystem = _system

  val log = Logging.getLogger(system, this)

  val channels = Seq("+switch-master")

  val onMessage = (message: Message) => {
    log.debug(s"SentinelClient.onMessage: message received: $message")
    if (message.data != null) {
      message.data.split(" ") match {
        case Array(master, oldip, oldport, newip, newport) =>
          onMasterChange(master, newip, newport.toInt)
        case _ => {}
      }
    }
  }

  val redisPubSubConnection: ActorRef = system.actorOf(
    Props(classOf[RedisSubscriberActorWithCallback],
      new InetSocketAddress(host, port), channels, Seq(), onMessage, (pmessage: PMessage) => {}, None)
      .withDispatcher(Redis.dispatcher),
    name + '-' + Redis.tempName()
  )

  /**
   * Disconnect from the server (stop the actors)
   */
  override def stop() {
    system stop redisConnection
    system stop redisPubSubConnection
  }

}

abstract class SentinelMonitored(system: ActorSystem) {
  val sentinelHost: String
  val sentinelPort: Int
  val master: String
  val onMasterChange: (String, Int) => Unit

  implicit val executionContext = system.dispatcher

  val sentinelClient = new SentinelClient(sentinelHost, sentinelPort, onSwitchMaster, "SMSentinelClient")(system)

  def onSwitchMaster(masterName: String, ip: String, port: Int) = {
    if (master == masterName)
      onMasterChange(ip, port)
  }

  def withMasterAddr[T](initFunction: (String, Int) => T): T = {
    import scala.concurrent.duration._

    val f = sentinelClient.getMasterAddr(master) map {
      case Some((ip: String, port: Int)) => initFunction(ip, port)
      case _ => throw new Exception(s"No such master '$master'")
    }
    Await.result(f, 15 seconds)
  }
}

abstract class SentinelMonitoredRedisClientLike(system: ActorSystem) extends SentinelMonitored(system) with ActorRequest {
  val redisClient: RedisClientActorLike
  val onMasterChange = (ip: String, port: Int) => {
    redisClient.reconnect(ip, port)
  }

  def redisConnection = redisClient.redisConnection

  /**
   * Disconnect from the server (stop the actors)
   */
  def stop() = {
    redisClient.stop()
    sentinelClient.stop()
  }

}

case class SentinelMonitoredRedisClient(
                                         sentinelHost: String = "localhost",
                                         sentinelPort: Int = 26379,
                                         master: String)
                                       (implicit system: ActorSystem) extends SentinelMonitoredRedisClientLike(system) with RedisCommands with Transactions {

  val redisClient: RedisClient = withMasterAddr((ip, port) => {
    new RedisClient(ip, port, "SMRedisClient")
  })

}

case class SentinelMonitoredRedisBlockingClient(sentinelHost: String = "localhost",
                                                sentinelPort: Int = 26379,
                                                master: String)
                                               (implicit system: ActorSystem) extends SentinelMonitoredRedisClientLike(system) with BLists {
  val redisClient: RedisBlockingClient = withMasterAddr((ip, port) => {
    new RedisBlockingClient(ip, port, "SMRedisClient")
  })
}

private[redis] object Redis {

  val dispatcher = "rediscala.rediscala-client-worker-dispatcher"

  val tempNumber = new AtomicLong

  def tempName() = Helpers.base64(tempNumber.getAndIncrement())

}