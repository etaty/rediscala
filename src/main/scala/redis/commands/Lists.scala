package redis.commands

import redis.{RedisValueConverter, MultiBulkConverter, Request}
import akka.util.{ByteString, Timeout}
import scala.concurrent.{Future, ExecutionContext}
import redis.protocol.{Status, MultiBulk, Bulk, Integer}
import scala.util.Try

trait Lists extends Request {

  def lindex(key: String, index: Long)(implicit ec: ExecutionContext): Future[Option[ByteString]] =
    send("LINDEX", Seq(ByteString(key), ByteString(index.toString))).mapTo[Bulk].map(_.response)

  def linsertAfter[A](key: String, pivot: String, value: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Long] =
    linsert(key, "AFTER", pivot, value)

  def linsertBefore[A](key: String, pivot: String, value: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Long] =
    linsert(key, "BEFORE", pivot, value)

  def linsert[A](key: String, beforeAfter: String, pivot: String, value: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Long] =
    send("LINSERT", Seq(ByteString(key), ByteString(beforeAfter), ByteString(pivot), convert.from(value))).mapTo[Integer].map(_.toLong)

  def llen(key: String)(implicit ec: ExecutionContext): Future[Long] =
    send("llen", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def lpop(key: String)(implicit ec: ExecutionContext): Future[Option[ByteString]] =
    send("LPOP", Seq(ByteString(key))).mapTo[Bulk].map(_.response)

  def lpush[A](key: String, values: A*)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Long] =
    send("LPUSH", ByteString(key) +: values.map(v => convert.from(v))).mapTo[Integer].map(_.toLong)

  def lpushx[A](key: String, value: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Long] =
    send("LPUSHX", Seq(ByteString(key), convert.from(value))).mapTo[Integer].map(_.toLong)

  def lrange(key: String, start: Long, stop: Long)(implicit convert: MultiBulkConverter[Seq[ByteString]], ec: ExecutionContext): Future[Try[Seq[ByteString]]] =
    send("LRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString))).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])

  def lrem[A](key: String, count: Long, value: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Long] =
    send("LREM", Seq(ByteString(key), ByteString(count.toString), convert.from(value))).mapTo[Integer].map(_.toLong)

  def lset[A](key: String, index: Long, value: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Boolean] =
    send("LSET", Seq(ByteString(key), ByteString(index.toString), convert.from(value))).mapTo[Status].map(_.toBoolean)

  def ltrim(key: String, start: Long, stop: Long)(implicit ec: ExecutionContext): Future[Boolean] =
    send("LTRIM", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString))).mapTo[Status].map(_.toBoolean)

  def rpop(key: String)(implicit ec: ExecutionContext): Future[Option[ByteString]] =
    send("RPOP", Seq(ByteString(key))).mapTo[Bulk].map(_.response)

  def rpoplpush(source: String, destination: String)(implicit ec: ExecutionContext): Future[Option[ByteString]] =
    send("RPOPLPUSH", Seq(ByteString(source), ByteString(destination))).mapTo[Bulk].map(_.response)

  def rpush[A](key: String, values: A*)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Long] =
    send("RPUSH", ByteString(key) +: values.map(v => convert.from(v))).mapTo[Integer].map(_.toLong)

  def rpushx[A](key: String, value: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Long] =
    send("RPUSHX", Seq(ByteString(key), convert.from(value))).mapTo[Integer].map(_.toLong)

}