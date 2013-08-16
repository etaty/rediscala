package redis.commands

import akka.util.ByteString
import redis._
import scala.concurrent.Future
import redis.protocol._
import redis.protocol.Integer
import redis.protocol.Status
import redis.protocol.Bulk
import scala.util.Try
import scala.concurrent.duration._
import redis.api.{Order, LimitOffsetCount}

trait Keys extends Request {

  def del(keys: String*): Future[Long] =
    send("DEL", keys.map(ByteString.apply)).mapTo[Integer].map(_.toLong)

  def dump(key: String): Future[Option[ByteString]] =
    send("DUMP", Seq(ByteString(key))).mapTo[Bulk].map(_.response)

  def exists(key: String): Future[Boolean] =
    send("EXISTS", Seq(ByteString(key))).mapTo[Integer].map(_.toBoolean)

  def expire(key: String, seconds: Long): Future[Boolean] =
    send("EXPIRE", Seq(ByteString(key), ByteString(seconds.toString))).mapTo[Integer].map(_.toBoolean)

  def expireat(key: String, seconds: Long): Future[Boolean] =
    send("EXPIREAT", Seq(ByteString(key), ByteString(seconds.toString))).mapTo[Integer].map(_.toBoolean)

  def keys(pattern: String)(implicit convert: MultiBulkConverter[Seq[String]]): Future[Try[Seq[String]]] =
    send("KEYS", Seq(ByteString(pattern))).mapTo[MultiBulk].map(_.asTry[Seq[String]])

  def migrate(host: String, port: Int, key: String, destinationDB: Int, timeout: FiniteDuration): Future[Boolean] = {
    val seq = Seq(ByteString(host), ByteString(port.toString), ByteString(key), ByteString(destinationDB.toString), ByteString(timeout.toMillis.toString))
    send("MIGRATE", seq).mapTo[Status].map(_.toBoolean)
  }

  def move(key: String, db: Int): Future[Boolean] =
    send("MOVE", Seq(ByteString(key), ByteString(db.toString))).mapTo[Integer].map(_.toBoolean)

  def objectRefcount(key: String): Future[Option[Long]] =
    send("OBJECT", Seq(ByteString("REFCOUNT"), ByteString(key))).mapTo[RedisReply].map({
      case i: Integer => Some(i.toLong)
      case _ => None
    })

  def objectIdletime(key: String): Future[Option[Long]] =
    send("OBJECT", Seq(ByteString("IDLETIME"), ByteString(key))).mapTo[RedisReply].map({
      case i: Integer => Some(i.toLong)
      case _ => None
    })

  def objectEncoding(key: String): Future[Option[String]] =
    send("OBJECT", Seq(ByteString("ENCODING"), ByteString(key))).mapTo[Bulk].map(_.toOptString)

  def persist(key: String): Future[Boolean] =
    send("PERSIST", Seq(ByteString(key))).mapTo[Integer].map(_.toBoolean)

  def pexpire(key: String, milliseconds: Long): Future[Boolean] =
    send("PEXPIRE", Seq(ByteString(key), ByteString(milliseconds.toString))).mapTo[Integer].map(_.toBoolean)

  def pexpireat(key: String, millisecondsTimestamp: Long): Future[Boolean] =
    send("PEXPIREAT", Seq(ByteString(key), ByteString(millisecondsTimestamp.toString))).mapTo[Integer].map(_.toBoolean)

  def pttl(key: String): Future[Long] =
    send("PTTL", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def randomkey(): Future[Option[ByteString]] =
    send("RANDOMKEY").mapTo[Bulk].map(_.response)

  def rename(key: String, newkey: String): Future[Boolean] =
    send("RENAME", Seq(ByteString(key), ByteString(newkey))).mapTo[Status].map(_.toBoolean)

  def renamenx(key: String, newkey: String): Future[Boolean] =
    send("RENAMENX", Seq(ByteString(key), ByteString(newkey))).mapTo[Integer].map(_.toBoolean)

  def restore[A](key: String, ttl: Long = 0, serializedValue: A)(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send("RESTORE", Seq(ByteString(key), ByteString(ttl.toString), convert.from(serializedValue))).mapTo[Status].map(_.toBoolean)

  private def sort(key: String,
                   byPattern: Option[String],
                   limit: Option[LimitOffsetCount],
                   getPatterns: Seq[String],
                   order: Option[Order],
                   alpha: Boolean,
                   store: Option[String]): Future[Any] = {
    var args = store.map(dest => List(ByteString("STORE"), ByteString(dest))).getOrElse(List())
    if (alpha) {
      args = ByteString("ALPHA") :: args
    }
    args = order.map(ord => ByteString(ord.toString) :: args).getOrElse(args)
    args = getPatterns.map(pat => List(ByteString("GET"), ByteString(pat))).toList.flatten ++ args
    args = limit.map(_.toByteString).getOrElse(Seq()).toList ++ args
    args = byPattern.map(ByteString("BY") :: ByteString(_) :: args).getOrElse(args)

    send("SORT", ByteString(key) :: args)
  }

  def sort(key: String,
           byPattern: Option[String] = None,
           limit: Option[LimitOffsetCount] = None,
           getPatterns: Seq[String] = Seq(),
           order: Option[Order] = None,
           alpha: Boolean = false): Future[Try[Seq[ByteString]]] = {
    sort(key, byPattern, limit, getPatterns, order, alpha, None).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])
  }

  def sortStore(key: String,
                byPattern: Option[String] = None,
                limit: Option[LimitOffsetCount] = None,
                getPatterns: Seq[String] = Seq(),
                order: Option[Order] = None,
                alpha: Boolean = false,
                store: Option[String] = None): Future[Long] = {
    sort(key, byPattern, limit, getPatterns, order, alpha, store).mapTo[Integer].map(_.toLong)
  }

  def ttl(key: String): Future[Long] =
    send("TTL", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def `type`(key: String): Future[String] =
    send("TYPE", Seq(ByteString(key))).mapTo[Status].map(_.toString)

}
