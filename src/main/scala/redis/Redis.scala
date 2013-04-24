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

trait RedisCommand

class RedisClient(val host: String, val port: Int)(implicit actorSystem: ActorSystem) extends RedisCommand
with Strings
with Connection
with Keys {
  val redisClientRouteur: ActorRef = actorSystem.actorOf(Props(classOf[RedisClientActor], host, port).withDispatcher("rediscala.rediscala-client-worker-dispatcher").withRouter(RoundRobinRouter(1)))

  def this()(implicit actorSystem: ActorSystem) = this("localhost", 6379)

  def send(command: String, args: Seq[ByteString])(implicit timeout: Timeout): Future[Any] = {
    redisClientRouteur ? RedisProtocolRequest.multiBulk(command, args)
  }

  def send(command: String)(implicit timeout: Timeout): Future[Any] = {
    redisClientRouteur ? RedisProtocolRequest.inline(command)
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
      def to(reply: RedisReply) = reply match {
        case i: Integer => Success(i.toString)
        case s: Status => Success(s.toString)
        case e: Error => Success(e.toString)
        case Bulk(b) => b.map(x => Success(x.utf8String)).getOrElse(Failure(new NoSuchElementException()))
        case MultiBulk(mb) => Failure(new NoSuchElementException()) // TODO find better ?
      }
    }

  }

}

