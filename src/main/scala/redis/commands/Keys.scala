package redis.commands

import akka.util.{ByteString, Timeout}
import redis._
import scala.concurrent.{ExecutionContext, Future}
import redis.protocol._
import redis.protocol.Integer
import redis.protocol.Status
import redis.protocol.Bulk

trait Keys extends Request {

  def del(keys: String*)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("DEL", keys.map(ByteString.apply).toSeq).mapTo[Integer].map(_.toLong)

  def dump(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[ByteString]] =
    send("DUMP", Seq(ByteString(key))).mapTo[Bulk].map(_.response)

  def exists(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("EXISTS", Seq(ByteString(key))).mapTo[Integer].map(_.toBoolean)

  def expire(key: String, seconds: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("EXPIRE", Seq(ByteString(key), ByteString(seconds.toString))).mapTo[Integer].map(_.toBoolean)

  def expireat(key: String, seconds: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("EXPIREAT", Seq(ByteString(key), ByteString(seconds.toString))).mapTo[Integer].map(_.toBoolean)

  def keys(pattern: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[Seq[RedisReply]]] =
    send("KEYS", Seq(ByteString(pattern))).mapTo[MultiBulk].map(_.responses)

  def migrate(host: String, port: String, key: String, destinationDB: String, timeOut: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[String] =
    send("MIGRATE", Seq(ByteString(host), ByteString(port), ByteString(key), ByteString(destinationDB), ByteString(timeOut.toString))).mapTo[Status].map(_.toString)

  def move(key: String, db: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("MOVE", Seq(ByteString(key), ByteString(db))).mapTo[Integer].map(_.toBoolean)

  def `object`() = ??? // TODO

  def persist(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("PERSIST", Seq(ByteString(key))).mapTo[Integer].map(_.toBoolean)

  def pexpire(key: String, milliseconds: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("PEXPIRE", Seq(ByteString(key), ByteString(milliseconds.toString))).mapTo[Integer].map(_.toBoolean)

  def pexpireat(key: String, millisecondsTimestamp: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("PEXPIREAT", Seq(ByteString(key), ByteString(millisecondsTimestamp.toString))).mapTo[Integer].map(_.toBoolean)

  def pttl(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("PTTL", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def randomkey()(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[ByteString]] =
    send("RANDOMKEY").mapTo[Bulk].map(_.response)

  def rename(key: String, newkey: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[String] =
    send("RENAME", Seq(ByteString(key), ByteString(newkey))).mapTo[Status].map(_.toString)

  def renamenx(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("RENAMENX", Seq(ByteString(key))).mapTo[Integer].map(_.toBoolean)

  def restore(key: String, ttl: Long, serializedValue: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("RESTORE", Seq(ByteString(key), ByteString(ttl.toString), ByteString(serializedValue))).mapTo[Status].map(_.toBoolean)

  def sort = ??? // TODO

  def ttl(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("TTL", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def `type`(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[String] =
    send("TYPE", Seq(ByteString(key))).mapTo[Status].map(_.toString)

}
