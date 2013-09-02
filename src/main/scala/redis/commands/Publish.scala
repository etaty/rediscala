package redis.commands

import redis.{ByteStringSerializer, Request}
import scala.concurrent.Future
import redis.api.publish.Publish

trait Publish extends Request {
  def publish[V: ByteStringSerializer](channel: String, value: V): Future[Long] =
    send(Publish(channel, value))
}
