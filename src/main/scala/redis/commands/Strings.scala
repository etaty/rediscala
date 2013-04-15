package redis.commands

import akka.util.{ByteString, Timeout}
import redis.{Status, Bulk, RedisClient}
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

trait Strings {
  self: RedisClient =>

  def set(key: String, value: String)(implicit timeout: Timeout): Future[Boolean] =
    send("SET", Seq(ByteString(key), ByteString(value))).mapTo[Status].map(_.toBoolean)

  def get(key: String)(implicit timeout: Timeout): Future[Bulk] =
    send("GET", Seq(ByteString(key))).mapTo[Bulk]

}



