package redis.commands

import redis.api.hyperloglog._
import redis.{ByteStringSerializer, Request}

import scala.concurrent.Future

trait HyperLogLog extends Request {
  def pfadd[V: ByteStringSerializer](key: String, values: V*): Future[Long] =
    send(Pfadd(key, values))

  def pfcount(keys: String*): Future[Long] =
    send(Pfcount(keys))

  def pfmerge(destKey: String, sourceKeys: String*): Future[Boolean] =
    send(Pfmerge(destKey, sourceKeys))
}
