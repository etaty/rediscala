package redis

import akka.actor._
import akka.util.{ByteString, Timeout}
import redis.commands._
import akka.pattern.ask
import scala.concurrent._
import scala.util.Try
import akka.routing.RoundRobinRouter
import redis.protocol._
import scala.util.Failure
import scala.util.Success
import java.net.InetSocketAddress


trait Request {
  def redisConnection: ActorRef

  def send(command: String, args: Seq[ByteString])(implicit timeout: Timeout): Future[Any] = {
    redisConnection ? RedisProtocolRequest.multiBulk(command, args)
  }

  def send(command: String)(implicit timeout: Timeout): Future[Any] = {
    redisConnection ? RedisProtocolRequest.inline(command)
  }
}

trait RedisCommands extends Strings with Connection with Keys

case class RedisClient(host: String = "localhost", port: Int = 6379, connections: Int = 4)(implicit system: ActorSystem) extends RedisCommands {

  val address = new InetSocketAddress(host, port)

  val redisConnection: ActorRef = system.actorOf(Props(classOf[RedisClientActor], address).withDispatcher("rediscala.rediscala-client-worker-dispatcher").withRouter(RoundRobinRouter(1)))

  def disconnect() {
    system stop redisConnection
  }

}

trait RedisValue

trait RedisValueConvert[A] {
  def from(a: A): ByteString
}

trait RedisReplyConvert[A] {
  def to(redisReply: RedisReply): Try[A]
}

object Redis {

  object Convert {

    implicit object StringConvert extends RedisValueConvert[String] {
      def from(s: String): ByteString = ByteString(s)
    }

    implicit object StringReplyConvert extends RedisReplyConvert[String] {
      def to(reply: RedisReply): Try[String] = reply match {
        case i: Integer => Success(i.toString)
        case s: Status => Success(s.toString)
        case e: Error => Success(e.toString)
        case Bulk(b) => b.map(x => Success(x.utf8String)).getOrElse(Failure(new NoSuchElementException()))
        case MultiBulk(mb) => Failure(new NoSuchElementException()) // TODO find better ?
      }
    }

  }

}

