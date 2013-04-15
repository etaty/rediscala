package redis.commands

import akka.util.{ByteString, Timeout}
import redis.{Status, Bulk, RedisClient}
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

trait Strings {
  self: RedisClient =>

  implicit def asBoolean(s: String): Boolean =
    s match {
      case "OK" => true
      case _ => false
    }

  def set(key: String, value: String)(implicit timeout: Timeout): Future[Boolean] =
    send("SET", Seq(ByteString(key), ByteString(value))).mapTo[Status].map(x => asBoolean(x.status))

  def get(key: String)(implicit timeout: Timeout): Future[Bulk] =
    send("GET", Seq(ByteString(key))).mapTo[Bulk]

}



