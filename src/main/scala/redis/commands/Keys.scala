package redis.commands

import akka.util.{ByteString, Timeout}
import redis.{Status, RedisClient}
import scala.concurrent.Future

trait Keys {
  self: RedisClient =>

  def del(key: String)(implicit timeout: Timeout): Future[redis.Integer] =
    send("DEL", Seq(ByteString(key))).mapTo[redis.Integer]

}
