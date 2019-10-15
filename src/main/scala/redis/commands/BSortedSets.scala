package redis.commands


import redis.{ByteStringDeserializer, Request}
import scala.concurrent.Future
import scala.concurrent.duration._
import redis.api.bsortedsets._

/**
  * Blocking commands on Sorted Sets
  */
trait BSortedSets extends Request {
  def bzpopmax[R: ByteStringDeserializer](keys: Seq[String], timeout: FiniteDuration = Duration.Zero): Future[Option[(String, R, Double)]] =
    send(Bzpopmax(keys, timeout))

  def bzpopmin[R: ByteStringDeserializer](keys: Seq[String], timeout: FiniteDuration = Duration.Zero): Future[Option[(String, R, Double)]] =
    send(Bzpopmin(keys, timeout))
}
