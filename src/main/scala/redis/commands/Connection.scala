package redis.commands

import akka.util.Timeout
import redis.{RedisClient}
import scala.concurrent.{ExecutionContext, Future}
import redis.protocol.Status

trait Connection {
  self: RedisClient =>

  def ping()(implicit timeout: Timeout, ec: ExecutionContext): Future[String] =
    send("PING").mapTo[Status].map(_.toString)

}
