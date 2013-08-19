package redis.api.connection

import redis._
import akka.util.ByteString
import redis.protocol.Status

case class Auth[A](value: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandStatus[Status] {
  val encodedRequest: ByteString = encode("AUTH", Seq(convert.from(value)))

  def decodeReply(s: Status) = s
}

case class Echo[A](value: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("ECHO", Seq(convert.from(value)))
}

case object Ping extends RedisCommandStatus[String] {
  val encodedRequest: ByteString = encode("PING")

  def decodeReply(s: Status) = s.toString
}

case object Quit extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("QUIT")
}

case class Select(index: Int) extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("SELECT", Seq(ByteString(index.toString)))
}