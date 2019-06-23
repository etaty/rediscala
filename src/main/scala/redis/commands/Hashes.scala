package redis.commands

import redis.{Cursor, ByteStringDeserializer, ByteStringSerializer, Request}
import scala.concurrent.Future
import redis.api.hashes._

trait Hashes extends Request {

  def hdel(key: String, fields: String*): Future[Long] =
    send(Hdel(key, fields))

  def hexists(key: String, field: String): Future[Boolean] =
    send(Hexists(key, field))

  def hget[R: ByteStringDeserializer](key: String, field: String): Future[Option[R]] =
    send(Hget(key, field))

  def hgetall[R: ByteStringDeserializer](key: String): Future[Map[String, R]] =
    send(Hgetall(key))

  def hincrby(key: String, fields: String, increment: Long): Future[Long] =
    send(Hincrby(key, fields, increment))

  def hincrbyfloat(key: String, fields: String, increment: Double): Future[Double] =
    send(Hincrbyfloat(key, fields, increment))

  def hkeys(key: String): Future[Seq[String]] =
    send(Hkeys(key))

  def hlen(key: String): Future[Long] =
    send(Hlen(key))

  def hmget[R: ByteStringDeserializer](key: String, fields: String*): Future[Seq[Option[R]]] =
    send(Hmget(key, fields))

  def hmset[V: ByteStringSerializer](key: String, keysValues: Map[String, V]): Future[Boolean] =
    send(Hmset(key, keysValues))

  def hset[V: ByteStringSerializer](key: String, field: String, value: V): Future[Boolean] =
    send(Hset(key, field, value))

  def hsetnx[V: ByteStringSerializer](key: String, field: String, value: V): Future[Boolean] =
    send(Hsetnx(key, field, value))

  def hvals[R: ByteStringDeserializer](key: String): Future[Seq[R]] =
    send(Hvals(key))

  def hscan[R: ByteStringDeserializer](key: String, cursor: Int = 0, count: Option[Int] = None, matchGlob: Option[String] = None): Future[Cursor[Map[String, R]]] =
    send(HScan(key, cursor, count, matchGlob))

}
