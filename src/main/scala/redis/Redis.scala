package redis

import akka.actor._
import akka.util.{ByteString, Timeout}
import redis.commands._
import akka.pattern.ask
import scala.concurrent._
import redis.protocol._
import java.net.InetSocketAddress
import redis.actors.RedisClientActor


trait Request {
  def redisConnection: ActorRef

  def send(request: ByteString)(implicit timeout: Timeout): Future[Any]

  def send(command: String, args: Seq[ByteString])(implicit timeout: Timeout): Future[Any] = {
    send(RedisProtocolRequest.multiBulk(command, args))
  }

  def send(command: String)(implicit timeout: Timeout): Future[Any] = {
    send(RedisProtocolRequest.inline(command))
  }
}

trait RedisCommands extends Keys with Strings with Hashes with Lists with Sets with SortedSets with Connection

/**
 *
 * @param host
 * @param port
 * @param system
 */
case class RedisClient(host: String = "localhost", port: Int = 6379)(implicit system: ActorSystem) extends RedisCommands with Transactions {

  val redisConnection: ActorRef = system.actorOf(Props(classOf[RedisClientActor]).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

  connect(host, port)

  def send(request: ByteString)(implicit timeout: Timeout): Future[Any] =
    redisConnection ? request

  def stop() {
    system stop redisConnection
  }

  def connect(host: String = "localhost", port: Int = 6379) {
    connect(new InetSocketAddress(host, port))
  }

  def connect(address: InetSocketAddress) {
    redisConnection ! address
  }

  def disconnect() {
    redisConnection ! "disconnect"
  }

}


