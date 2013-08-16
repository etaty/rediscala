package redis.commands

import akka.util.ByteString
import redis.{RedisValueConverter, Request}
import scala.concurrent.Future
import redis.protocol.Integer

trait Publish extends Request {
  def publish[A](channel: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send("PUBLISH", Seq(ByteString(channel), convert.from(value))).mapTo[Integer].map(_.toLong)
}
