package redis.api.publish

import redis.{RedisCommandIntegerLong, RedisValueConverter}
import akka.util.ByteString

case class Publish[A](channel: String, value: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerLong{
  val encodedRequest: ByteString = encode("PUBLISH", Seq(ByteString(channel), convert.from(value)))
}
