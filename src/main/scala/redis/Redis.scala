package redis

import akka.actor._
import akka.util.{ByteString, Timeout}
import redis.commands._
import akka.pattern.ask
import scala.concurrent._
import scala.util.Try
import akka.routing.{Broadcast, RoundRobinRouter}
import redis.protocol._
import java.net.InetSocketAddress


trait Request {
  def redisConnection: ActorRef

  def send(command: String, args: Seq[ByteString])(implicit timeout: Timeout): Future[Any] = {
    redisConnection ? RedisProtocolRequest.multiBulk(command, args)
  }

  def send(command: String)(implicit timeout: Timeout): Future[Any] = {
    redisConnection ? RedisProtocolRequest.inline(command)
  }

  // TODO build an aggregate actor to hold the replies and compare them (should fail if all replies are not the same)
  def sendBroadcast(command: String, args: Seq[ByteString])(implicit timeout: Timeout): Future[Any] = {
    redisConnection ? Broadcast(RedisProtocolRequest.multiBulk(command, args))
  }

  def sendBroadcast(command: String)(implicit timeout: Timeout): Future[Any] = {
    redisConnection ? Broadcast(RedisProtocolRequest.inline(command))
  }
}

trait RedisCommands extends Strings with Connection with Keys

/**
 *
 * @param host
 * @param port
 * @param connections number of connections opened to Redis (1 is a perfect number as Redis server is fully asynchronous)
 * @param system
 */
case class RedisClient(host: String = "localhost", port: Int = 6379, connections: Int = 4)(implicit system: ActorSystem) extends RedisCommands {

  val redisConnection: ActorRef = system.actorOf(Props(classOf[RedisClientActor]).withDispatcher("rediscala.rediscala-client-worker-dispatcher").withRouter(RoundRobinRouter(connections)))

  connect(host, port)

  def stop() {
    system stop redisConnection
  }

  def connect(host: String = "localhost", port: Int = 6379) {
    connect(new InetSocketAddress(host, port))
  }

  def connect(address: InetSocketAddress) {
    redisConnection ! Broadcast(address)
  }

  def disconnect() {
    redisConnection ! Broadcast("disconnect")
  }

}


