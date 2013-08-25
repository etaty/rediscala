package redis.api.publish

import redis.{RedisCommandIntegerLong, ByteStringSerializer}
import akka.util.ByteString

case class Publish[A](channel: String, value: A)(implicit convert: ByteStringSerializer[A]) extends RedisCommandIntegerLong{
  val encodedRequest: ByteString = encode("PUBLISH", Seq(ByteString(channel), convert.serialize(value)))
}
