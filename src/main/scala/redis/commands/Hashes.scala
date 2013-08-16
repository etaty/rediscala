package redis.commands

import redis.{RedisValueConverter, MultiBulkConverter, Request}
import akka.util.ByteString
import scala.concurrent.Future
import redis.protocol.{Status, MultiBulk, Bulk, Integer}
import scala.util.Try

trait Hashes extends Request {

  def hdel(key: String, fields: String*): Future[Long] =
    send("HDEL", ByteString(key) +: fields.map(ByteString.apply)).mapTo[Integer].map(_.toLong)

  def hexists(key: String, field: String): Future[Boolean] =
    send("HEXISTS", Seq(ByteString(key), ByteString(field))).mapTo[Integer].map(_.toBoolean)

  def hget(key: String, field: String): Future[Option[ByteString]] =
    send("HGET", Seq(ByteString(key), ByteString(field))).mapTo[Bulk].map(_.response)

  def hgetall(key: String)(implicit convert: MultiBulkConverter[Map[String, ByteString]]): Future[Try[Map[String, ByteString]]] =
    send("HGETALL", Seq(ByteString(key))).mapTo[MultiBulk].map(_.asTry[Map[String, ByteString]])

  def hincrby(key: String, fields: String, increment: Long): Future[Long] =
    send("HINCRBY", Seq(ByteString(key), ByteString(fields), ByteString(increment.toString))).mapTo[Integer].map(_.toLong)

  def hincrbyfloat(key: String, fields: String, increment: Double): Future[Double] =
    send("HINCRBYFLOAT", Seq(ByteString(key), ByteString(fields), ByteString(increment.toString))).mapTo[Bulk].map(_.response.map(v => java.lang.Double.valueOf(v.utf8String)).get)

  def hkeys(key: String)(implicit convert: MultiBulkConverter[Seq[String]]): Future[Try[Seq[String]]] =
    send("HKEYS", Seq(ByteString(key))).mapTo[MultiBulk].map(_.asTry[Seq[String]])

  def hlen(key: String)(implicit convert: MultiBulkConverter[Seq[String]]): Future[Long] =
    send("HLEN", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def hmget(key: String, fields: String*)(implicit convert: MultiBulkConverter[Seq[Option[ByteString]]]): Future[Try[Seq[Option[ByteString]]]] =
    send("HMGET", ByteString(key) +: fields.map(ByteString.apply)).mapTo[MultiBulk].map(_.asTry[Seq[Option[ByteString]]])

  def hmset[A](key: String, keysValues: Map[String, A])(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send("HMSET", ByteString(key) +: keysValues.foldLeft(Seq.empty[ByteString])({
      case (acc, e) => ByteString(e._1) +: convert.from(e._2) +: acc
    })).mapTo[Status].map(_.toBoolean)

  def hset[A](key: String, field: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send("HSET", Seq(ByteString(key), ByteString(field), convert.from(value))).mapTo[Integer].map(_.toBoolean)

  def hsetnx[A](key: String, field: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send("HSETNX", Seq(ByteString(key), ByteString(field), convert.from(value))).mapTo[Integer].map(_.toBoolean)

  def hvals[A](key: String)(implicit convert: MultiBulkConverter[Seq[ByteString]]): Future[Try[Seq[ByteString]]] =
    send("HVALS", Seq(ByteString(key))).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])

}
