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
import redis.protocol.RedisReply

trait Request {
  implicit val executionContext: ExecutionContext

  def redisConnection: ActorRef

  def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    val promise = Promise[T]()
    redisConnection ! Operation(redisCommand, promise)
    promise.future
  }
}

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

case class RedisClient(host: String = "localhost",
                       port: Int = 6379,
                       name: String = "RedisClient")
                      (implicit system: ActorSystem) extends RedisCommands with Transactions {
  implicit val executionContext = system.dispatcher

  val redisConnection: ActorRef = system.actorOf(
    Props(classOf[RedisClientActor], new InetSocketAddress(host, port))
      .withDispatcher(Redis.dispatcher),
    name + '-' + Redis.tempName()
  )

  // will disconnect from the server
  def disconnect() {
    system stop redisConnection
  }

}

case class RedisBlockingClient(host: String = "localhost",
                               port: Int = 6379,
                               name: String = "RedisBlockingClient")
                              (implicit system: ActorSystem) extends BLists {
  implicit val executionContext = system.dispatcher

  val redisConnection: ActorRef = system.actorOf(
    Props(classOf[RedisClientActor], new InetSocketAddress(host, port))
      .withDispatcher(Redis.dispatcher),
    name + '-' + Redis.tempName()
  )

  // will disconnect from the server
  def disconnect() {
    system stop redisConnection
  }

}

case class RedisPubSub(
                        host: String = "localhost",
                        port: Int = 6379,
                        channels: Seq[String],
                        patterns: Seq[String],
                        onMessage: Message => Unit = _ => {},
                        onPMessage: PMessage => Unit = _ => {},
                        name: String = "RedisPubSub"
                        )(implicit system: ActorSystem) {

  val redisConnection: ActorRef = system.actorOf(
    Props(classOf[RedisSubscriberActorWithCallback],
      new InetSocketAddress(host, port), channels, patterns, onMessage, onPMessage)
      .withDispatcher(Redis.dispatcher),
    name + '-' + Redis.tempName()
  )

  // will disconnect from the server
  def disconnect() {
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

case class SentinelClient(host: String = "localhost",
                          port: Int = 26379,
                          onMasterChange: (String, Int) => Unit = (ip: String, port: Int) => {},
                          name: String = "SentinelClient")
                         (implicit system: ActorSystem) extends SentinelCommands {

  implicit val executionContext = system.dispatcher

  val log = Logging.getLogger(system, this)

  val redisConnection: ActorRef = system.actorOf(
    Props(classOf[RedisClientActor], new InetSocketAddress(host, port))
      .withDispatcher(Redis.dispatcher),
    name + '-' + Redis.tempName()
  )

  val channels = Seq("+switch-master")

  val onMessage = (message: Message) => {
    log.debug(s"SentinelClient.onMessage: message received: $message")
    if (message.data != null) {
      message.data.split(" ") match {
        case Array(master, oldip, oldport, newip, newport) =>
          onMasterChange(newip, newport.toInt)
        case _ => {}
      }
    }
  }

  val redisPubSubConnection: ActorRef = system.actorOf(
    Props(classOf[RedisSubscriberActorWithCallback],
      new InetSocketAddress(host, port), channels, Seq(), onMessage, (pmessage: PMessage) => {})
      .withDispatcher(Redis.dispatcher),
    name + '-' + Redis.tempName()
  )

  // will disconnect from the server
  def disconnect() {
    system stop redisConnection
    system stop redisPubSubConnection
  }

}

case class SentinelMonitoredRedisClient(
                           sentinelHost: String = "localhost",
                           sentinelPort: Int = 26379,
                           master: String)
                          (implicit system: ActorSystem) extends RedisCommands with Transactions {
  import scala.concurrent.duration._

  implicit val executionContext = system.dispatcher

  private val onMasterChange =
          (ip: String, port: Int) => {
            val oldrc = rc
            rc = new RedisClient(ip, port, "SMRedisClient")
            oldrc.disconnect()
          }

  private val sc = new SentinelClient(sentinelHost, sentinelPort, onMasterChange, "SMSentinelClient")

  private var rc: RedisClient = {
    val f = sc.getMasterAddr(master) map {
      case Some((ip: String, port: Int)) => new RedisClient(ip, port, "SMRedisClient")
      case _ => throw new Exception(s"No such master '$master'")
    }
    Await.result(f, 15 seconds)
  }

  def redisClient() = rc

  def redisConnection() = redisClient().redisConnection

  def disconnect() = redisClient().disconnect()

}

private[redis] object Redis {

  val dispatcher = "rediscala.rediscala-client-worker-dispatcher"

  val tempNumber = new AtomicLong

  def tempName() = Helpers.base64(tempNumber.getAndIncrement())

}