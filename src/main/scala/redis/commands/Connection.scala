package redis.commands

import akka.util.Timeout
import redis.{Status, RedisClient}
import scala.concurrent.{ExecutionContext, Future}

trait Connection {
  self: RedisClient =>

  def ping()(implicit timeout: Timeout, ec: ExecutionContext): Future[String] =
    send("PING").mapTo[Status].map(_.toString)

}
