package redis

import akka.util.ByteString
import redis.protocol._
import scala.util.Try

trait RedisValue

trait RedisValueConverter[A] {
  def from(a: A): ByteString
}

trait MultiBulkConverter[A] {
  def to(redisReply: MultiBulk): Try[A]
}

object Converter {

  implicit object StringConverter extends RedisValueConverter[String] {
    def from(s: String): ByteString = ByteString(s)
  }

  implicit object ByteStringConverter extends RedisValueConverter[ByteString] {
    def from(bs: ByteString): ByteString = bs
  }

  implicit object SeqStringMultiBulkConverter extends MultiBulkConverter[Seq[String]] {
    def to(reply: MultiBulk): Try[Seq[String]] = Try(reply.responses.map(r => {
      r.map(_.toString)
    }).get)
  }

}
