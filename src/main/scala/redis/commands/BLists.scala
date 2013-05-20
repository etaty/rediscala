package redis.commands

import redis.{MultiBulkConverter, Request}
import akka.util.ByteString
import scala.concurrent.{Future, ExecutionContext}
import redis.protocol.{MultiBulk, Bulk}
import scala.util.Try
import scala.concurrent.duration._

/**
 * Blocking commands on the Lists
 */
trait BLists extends Request {

  def blpop(keys: Seq[String], timeout: FiniteDuration = Duration.Zero)(implicit convert: MultiBulkConverter[Option[(String, ByteString)]], ec: ExecutionContext): Future[Try[Option[(String, ByteString)]]] =
    _bXpop("BLPOP", keys, timeout)

  private def _bXpop(command: String, keys: Seq[String], timeout: FiniteDuration)(implicit convert: MultiBulkConverter[Option[(String, ByteString)]], ec: ExecutionContext): Future[Try[Option[(String, ByteString)]]] =
    send(command, keys.map(ByteString.apply).seq ++ Seq(ByteString(timeout.toSeconds.toString))).mapTo[MultiBulk].map(_.asTry[Option[(String, ByteString)]])

  def brpop(keys: Seq[String], timeout: FiniteDuration = Duration.Zero)(implicit convert: MultiBulkConverter[Option[(String, ByteString)]], ec: ExecutionContext): Future[Try[Option[(String, ByteString)]]] =
    _bXpop("BRPOP", keys, timeout)

  def brpopplush(source: String, destination: String, timeout: FiniteDuration = Duration.Zero)(implicit ec: ExecutionContext): Future[Option[ByteString]] =
    send("BRPOPLPUSH", Seq(ByteString(source), ByteString(destination), ByteString(timeout.toSeconds.toString))).map({
      case b: Bulk => b.asOptByteString
      case _ => None
    })

}