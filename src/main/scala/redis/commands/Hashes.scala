package redis.commands

import redis.{RedisValueConverter, Request}
import akka.util.ByteString
import scala.concurrent.Future
import redis.api.hashes._

trait Hashes extends Request {

  def hdel(key: String, fields: String*): Future[Long] =
    send(Hdel(key, fields))

  def hexists(key: String, field: String): Future[Boolean] =
    send(Hexists(key, field))

  def hget(key: String, field: String): Future[Option[ByteString]] =
    send(Hget(key, field))

  def hgetall(key: String): Future[Map[String, ByteString]] =
    send(Hgetall(key))

  def hincrby(key: String, fields: String, increment: Long): Future[Long] =
    send(Hincrby(key, fields, increment))

  def hincrbyfloat(key: String, fields: String, increment: Double): Future[Double] =
    send(Hincrbyfloat(key, fields, increment))

  def hkeys(key: String): Future[Seq[String]] =
    send(Hkeys(key))

  def hlen(key: String): Future[Long] =
    send(Hlen(key))

  def hmget(key: String, fields: String*): Future[Seq[Option[ByteString]]] =
    send(Hmget(key, fields))

  def hmset[A](key: String, keysValues: Map[String, A])(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send(Hmset(key, keysValues))

  def hset[A](key: String, field: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send(Hset(key, field, value))

  def hsetnx[A](key: String, field: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send(Hsetnx(key, field, value))

  def hvals(key: String): Future[Seq[ByteString]] =
    send(Hvals(key))

}
