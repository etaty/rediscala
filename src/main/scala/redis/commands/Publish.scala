package redis.commands

import redis.{ByteStringSerializer, Request}
import scala.concurrent.Future
import redis.api.publish.Publish

trait Publish extends Request {
  def publish[A](channel: String, value: A)(implicit convert: ByteStringSerializer[A]): Future[Long] =
    send(Publish(channel, value))
}
