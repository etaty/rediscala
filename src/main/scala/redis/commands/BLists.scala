package redis.commands

import redis.{ByteStringSerializer, Request}
import akka.util.ByteString
import scala.concurrent.Future
import scala.concurrent.duration._
import redis.api.blists._

/**
 * Blocking commands on the Lists
 */
trait BLists extends Request {

  // TODO Future[Option[(KK, ByteString)]]
  def blpop[KK: ByteStringSerializer](keys: Seq[KK], timeout: FiniteDuration = Duration.Zero): Future[Option[(String, ByteString)]] =
    send(Blpop(keys, timeout))

  def brpop[KK: ByteStringSerializer](keys: Seq[KK], timeout: FiniteDuration = Duration.Zero): Future[Option[(String, ByteString)]] =
    send(Brpop(keys, timeout))

  def brpopplush[KS: ByteStringSerializer, KD: ByteStringSerializer](source: KS, destination: KD, timeout: FiniteDuration = Duration.Zero): Future[Option[ByteString]] =
    send(Brpopplush(source, destination, timeout))
}