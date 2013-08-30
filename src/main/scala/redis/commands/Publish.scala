package redis.commands

import redis.{RedisValueConverter, Request}
import scala.concurrent.Future

trait Publish extends Request {
  def publish[A](channel: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(redis.api.publish.Publish(channel, value))
}
