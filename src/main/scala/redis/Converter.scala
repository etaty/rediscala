package redis

import akka.util.ByteString
import redis.protocol._
import scala.util.Try
import redis.protocol.Error
import redis.protocol.Integer
import scala.util.Failure
import redis.protocol.Status
import scala.util.Success
import redis.protocol.Bulk

object Converter {

  implicit object StringConvert extends RedisValueConverter[String] {
    def from(s: String): ByteString = ByteString(s)
  }

  implicit object StringReplyConvert extends RedisReplyConverter[String] {
    def to(reply: RedisReply): Try[String] = reply match {
      case i: Integer => Success(i.toString)
      case s: Status => Success(s.toString)
      case e: Error => Success(e.toString)
      case Bulk(b) => b.map(x => Success(x.utf8String)).getOrElse(Failure(new NoSuchElementException()))
      case MultiBulk(mb) => Failure(new NoSuchElementException()) // TODO find better ?
    }
  }

}
