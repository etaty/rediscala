package redis.commands

import akka.util.Timeout
import redis.{Status, RedisClient}
import scala.concurrent.Future

trait Connection {
  self: RedisClient =>

  def ping()(implicit timeout: Timeout): Future[Status] =
    send("PING").mapTo[Status]

}
