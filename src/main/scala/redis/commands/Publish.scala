package redis.commands

import redis.{ByteStringSerializer, Request}
import scala.concurrent.Future
import redis.api.publish.{Publish => PublishCommand}

trait Publish extends Request {
  def publish[V: ByteStringSerializer](channel: String, value: V): Future[Long] =
    send(PublishCommand(channel, value))
}
