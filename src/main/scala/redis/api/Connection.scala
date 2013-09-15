package redis.api.connection

import redis._
import akka.util.ByteString
import redis.protocol.Status

case class Auth[V](value: V)(implicit convert: ByteStringSerializer[V]) extends RedisCommandStatus[Status] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("AUTH", Seq(convert.serialize(value)))

  def decodeReply(s: Status) = s
}

case class Echo[V, R](value: V)(implicit convert: ByteStringSerializer[V], deserializerR : ByteStringDeserializer[R]) extends RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("ECHO", Seq(convert.serialize(value)))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case object Ping extends RedisCommandStatus[String] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("PING")

  def decodeReply(s: Status) = s.toString
}

case object Quit extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("QUIT")
}

case class Select(index: Int) extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SELECT", Seq(ByteString(index.toString)))
}