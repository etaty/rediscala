package redis

import akka.actor._
import akka.util.{Helpers, ByteString}
import redis.commands._
import scala.concurrent._
import redis.protocol._
import java.net.InetSocketAddress
import redis.actors.{RedisSubscriberActorWithCallback, RedisClientActor}
import redis.api.pubsub._
import java.util.concurrent.atomic.AtomicLong


trait Request {
  def redisConnection: ActorRef

  def send(request: ByteString): Future[Any]

  def send(command: String, args: Seq[ByteString]): Future[Any] = {
    send(RedisProtocolRequest.multiBulk(command, args))
  }

  def send(command: String): Future[Any] = {
    send(RedisProtocolRequest.inline(command))
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
  with Connection
  with Server

case class RedisClient(host: String = "localhost", port: Int = 6379, name: String = "RedisClient")(implicit system: ActorSystem) extends RedisCommands with Transactions {

  val redisConnection: ActorRef = system.actorOf(
    Props(classOf[RedisClientActor], new InetSocketAddress(host, port))
      .withDispatcher("rediscala.rediscala-client-worker-dispatcher"),
    name + '-' + Redis.tempName()
  )

  def send(request: ByteString): Future[Any] = {
    val promise = Promise[RedisReply]()
    redisConnection ! Operation(request, promise)
    promise.future
  }

  // will disconnect from the server
  def disconnect() {
    system stop redisConnection
  }

}

case class RedisBlockingClient(host: String = "localhost", port: Int = 6379, name: String = "RedisBlockingClient")(implicit system: ActorSystem) extends BLists {

  val redisConnection: ActorRef = system.actorOf(
    Props(classOf[RedisClientActor], new InetSocketAddress(host, port))
      .withDispatcher("rediscala.rediscala-client-worker-dispatcher"),
    name + '-' + Redis.tempName()
  )

  def send(request: ByteString): Future[Any] = {
    val promise = Promise[RedisReply]()
    redisConnection ! Operation(request, promise)
    promise.future
  }

  // will disconnect from the server
  def disconnect() {
    system stop redisConnection
  }

}

case class Operation(request: ByteString, promise: Promise[RedisReply])

case class Transaction(commands: Seq[Operation])

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
    Props(classOf[RedisSubscriberActorWithCallback], new InetSocketAddress(host, port), channels, patterns, onMessage, onPMessage)
      .withDispatcher("rediscala.rediscala-client-worker-dispatcher"),
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

private[redis] object Redis {
  val tempNumber = new AtomicLong

  def tempName() = Helpers.base64(tempNumber.getAndIncrement())
}