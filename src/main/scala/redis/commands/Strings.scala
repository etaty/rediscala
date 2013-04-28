package redis.commands

import akka.util.{ByteString, Timeout}
import redis._
import scala.concurrent.{ExecutionContext, Future}
import redis.protocol._
import redis.protocol.MultiBulk
import redis.protocol.Integer
import redis.protocol.Status
import redis.protocol.Bulk

trait Strings extends Request {

  def append[A](key: String, value: A)(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("APPEND", Seq(ByteString(key), convert.from(value))).mapTo[Integer].map(_.toLong)

  def bitcount(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("BITCOUNT", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def bitcount(key: String, start: Long, end: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("BITCOUNT", Seq(ByteString(key), ByteString(start.toString), ByteString(end.toString))).mapTo[Integer].map(_.toLong)

  def bitopAND(destkey: String, keys: String*)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    bitop("AND", destkey, keys: _*)

  def bitopOR(destkey: String, keys: String*)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    bitop("OR", destkey, keys: _*)

  def bitopXOR(destkey: String, keys: String*)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    bitop("XOR", destkey, keys: _*)

  def bitopNOT(destkey: String, key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    bitop("NOT", destkey, key)

  def bitop(operation: String, destkey: String, keys: String*)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("BITOP", (Seq(ByteString(operation), ByteString(destkey)) ++ keys.map(ByteString.apply))).mapTo[Integer].map(_.toLong)

  def decr(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("DECR", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def decrby(key: String, decrement: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("DECRBY", Seq(ByteString(key), ByteString(decrement.toString))).mapTo[Integer].map(_.toLong)

  def get(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[ByteString]] =
    send("GET", Seq(ByteString(key))).mapTo[Bulk].map(_.response)

  def getbit(key: String, offset: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("GETBIT", Seq(ByteString(key), ByteString(offset.toString))).mapTo[Integer].map(_.toBoolean)

  def getrange(key: String, start: Long, end: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[ByteString]] =
    send("GETRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(end.toString))).mapTo[Bulk].map(_.response)

  def getset[A](key: String, value: A)(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Option[ByteString]] =
    send("GETSET", Seq(ByteString(key), ByteString(value.toString))).mapTo[Bulk].map(_.response)

  def incr(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("INCR", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def incrby(key: String, increment: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("INCRBY", Seq(ByteString(key), ByteString(increment.toString))).mapTo[Integer].map(_.toLong)

  def incrbyfloat(key: String, increment: Double)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[Double]] =
    send("INCRBYFLOAT", Seq(ByteString(key), ByteString(increment.toString))).mapTo[Bulk].map(_.response.map(v => java.lang.Double.valueOf(v.utf8String)))

  def mget(keys: String*)(implicit timeout: Timeout, ec: ExecutionContext): Future[MultiBulk] =
    send("MGET", keys.map(ByteString.apply)).mapTo[MultiBulk]

  def mset[A](keysValues: Map[String, A])(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("MSET", keysValues.foldLeft(Seq[ByteString]())({
      case (acc, e) => ByteString(e._1) +: convert.from(e._2) +: acc
    })).mapTo[Status].map(_.toBoolean)

  def msetnx[A](keysValues: Map[String, A])(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("MSETNX", keysValues.foldLeft(Seq[ByteString]())({
      case (acc, e) => ByteString(e._1) +: convert.from(e._2) +: acc
    })).mapTo[Integer].map(_.toBoolean)

  def psetex[A](key: String, milliseconds: Long, value: A)(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("PSETEX", Seq(ByteString(key), ByteString(milliseconds.toString), convert.from(value))).mapTo[Status].map(_.toBoolean)

  def set[A](key: String, value: A, exSeconds: Option[Long] = None, pxMilliseconds: Option[Long] = None, NX: Boolean = false, XX: Boolean = false)
            (implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {
    val seq = if (NX) Seq(ByteString("NX")) else if (XX) Seq(ByteString("XX")) else Seq.empty[ByteString]
    val options: Seq[ByteString] = exSeconds.map(t => Seq(ByteString("EX"), ByteString(t.toString)))
      .orElse(pxMilliseconds.map(t => Seq(ByteString("PX"), ByteString(t.toString))))
      .getOrElse(seq)
    val args = ByteString(key) +: convert.from(value) +: options
    send("SET", args).mapTo[RedisReply].map({
      case s: Status => s.toBoolean
      case _ => false
    })
  }

  def setbit(key: String, offset: Long, value: Boolean)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("SETBIT", Seq(ByteString(key), ByteString(offset.toString), ByteString(if (value) "1" else "0"))).mapTo[Integer].map(_.toBoolean)

  def setex[A](key: String, seconds: Long, value: A)(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("SETEX", Seq(ByteString(key), ByteString(seconds.toString), convert.from(value))).mapTo[Status].map(_.toBoolean)

  def setnx[A](key: String, value: A)(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("SETNX", Seq(ByteString(key), convert.from(value))).mapTo[Integer].map(_.toBoolean)

  def setrange[A](key: String, offset: Long, value: A)(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("SETRANGE", Seq(ByteString(key), ByteString(offset.toString), convert.from(value))).mapTo[Integer].map(_.toLong)

  def strlen(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("STRLEN", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

}



