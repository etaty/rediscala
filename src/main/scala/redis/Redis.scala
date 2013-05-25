package redis

import akka.actor._
import akka.util.ByteString
import redis.commands._
import scala.concurrent._
import redis.protocol._
import java.net.InetSocketAddress
import redis.actors.RedisClientActor


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

trait RedisCommands extends Keys with Strings with Hashes with Lists with Sets with SortedSets with PubSub with Connection with Server

/**
 *
 * @param host
 * @param port
 * @param system
 */
case class RedisClient(host: String = "localhost", port: Int = 6379)(implicit system: ActorSystem) extends RedisCommands with Transactions {

  val redisConnection: ActorRef = system.actorOf(Props(classOf[RedisClientActor], new InetSocketAddress(host, port)).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

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

case class RedisBlockingClient(host: String = "localhost", port: Int = 6379)(implicit system: ActorSystem) extends BLists {

  val redisConnection: ActorRef = system.actorOf(Props(classOf[RedisClientActor], new InetSocketAddress(host, port)).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

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
