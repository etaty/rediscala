package redis.commands

import redis.Request
import akka.util.ByteString
import scala.concurrent.Future
import scala.concurrent.duration._
import redis.api.blists._

/**
 * Blocking commands on the Lists
 */
trait BLists extends Request {

  // TODO Future[Option[(KK, ByteString)]]
  def blpop(keys: Seq[String], timeout: FiniteDuration = Duration.Zero): Future[Option[(String, ByteString)]] =
    send(Blpop(keys, timeout))

  def brpop(keys: Seq[String], timeout: FiniteDuration = Duration.Zero): Future[Option[(String, ByteString)]] =
    send(Brpop(keys, timeout))

  def brpopplush(source: String, destination: String, timeout: FiniteDuration = Duration.Zero): Future[Option[ByteString]] =
    send(Brpopplush(source, destination, timeout))
}