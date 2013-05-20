package redis.commands

import akka.util.{ByteString, Timeout}
import redis.{RedisValueConverter, Request}
import scala.concurrent.{ExecutionContext, Future}
import redis.protocol.Integer

trait PubSub extends Request {
  def publish[A](channel: String, value: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Long] =
    send("PUBLISH", Seq(ByteString(channel), convert.from(value))).mapTo[Integer].map(_.toLong)
}
