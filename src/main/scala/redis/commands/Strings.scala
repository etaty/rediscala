package redis.commands

import akka.util.{ByteString, Timeout}
import redis._
import scala.concurrent.{ExecutionContext, Future}
import redis.protocol.{RedisReply, Bulk, Integer, Status}

trait Strings extends Request {

  def append(key: String, value: RedisReply)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    ??? //send("APPEND", Seq(ByteString(key), value)).mapTo[Integer].map(_.toLong)

  def bitcount(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("BITCOUNT", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def bitcount(key: String, start: Long, end: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("BITCOUNT", Seq(ByteString(key), ByteString(start.toString), ByteString(end.toString))).mapTo[Integer].map(_.toLong)

  // TODO "AND OR XOR NOT"
  def bitop(operation: String, destkey: String, keys: String*)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("BITOP", (Seq(ByteString(operation), ByteString(destkey)) ++ keys.map(ByteString.apply)).toSeq).mapTo[Integer].map(_.toLong)

  def decr(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("DECR", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def decrby(key: String, decrement: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("DECRBY", Seq(ByteString(key), ByteString(decrement.toString))).mapTo[Integer].map(_.toLong)

  def get(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Bulk] =
    send("GET", Seq(ByteString(key))).mapTo[Bulk]

  def getbit(key: String, offset: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("GETBIT", Seq(ByteString(key), ByteString(offset.toString))).mapTo[Integer].map(_.toLong)

  def getrange(key: String, start: Long, end: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[ByteString]] =
    send("GETRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(end.toString))).mapTo[Bulk].map(_.response)

  def getset(key: String, value: RedisReply)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[ByteString]] =
    send("GETSET", Seq(ByteString(key), ByteString(value.toString))).mapTo[Bulk].map(_.response)

  def incr(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("INCR", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def incrby(key: String, increment: Long)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("INCRBY", Seq(ByteString(key), ByteString(increment))).mapTo[Integer].map(_.toLong)

  def incrbyfloat(key: String, increment: Double)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[ByteString]] =
    send("INCRBYFLOAT", Seq(ByteString(key), ByteString(increment.toString))).mapTo[Bulk].map(_.response)

  def mget(keys: String*)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("MGET", keys.map(ByteString.apply).toSeq).mapTo[Integer].map(_.toLong)

  def mset[A](keysValues: Map[String, A])(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = ??? // TODO tuple or map ?

  def msetnx = ??? // TODO

  def psetex(key: String, milliseconds: Long, value: RedisReply)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    ??? //send("PSETEX", Seq(ByteString(key), value)).mapTo[Status].map(_.toBoolean)

  // TODO finish implementation : expire time && [NX|XX]
  def set[A](key: String, value: A)(implicit convert: RedisValueConvert[A], timeout: Timeout, ec: ExecutionContext): Future[Status] =
    send("SET", Seq(ByteString(key), convert.from(value))).mapTo[Status]

  def setbit(key: String, offset: Long, value: RedisReply)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    ??? //send("SETBIT", Seq(ByteString(key), ByteString(offset), value)).mapTo[Integer].map(_.toLong)

  def setex(key: String, seconds: Long, value: RedisReply)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    ??? //send("SETEX", Seq(ByteString(key), ByteString(seconds), value)).mapTo[Status].map(_.toBoolean)

  def setnx(key: String, seconds: Long, value: RedisReply)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    ??? //send("SETNX", Seq(ByteString(key), ByteString(seconds), value)).mapTo[Integer].map(_.toBoolean)

  def setrange(key: String, offset: Long, value: RedisReply)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    ??? //send("SETNX", Seq(ByteString(key), ByteString(offset), value)).mapTo[Integer].map(_.toBoolean)

  def strlen(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("STRLEN", Seq(ByteString(key))).mapTo[Integer].map(_.toBoolean)

}



