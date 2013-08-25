package redis.commands

import redis.{ByteStringSerializer, Request}
import akka.util.ByteString
import scala.concurrent.Future
import redis.api.hashes._

trait Hashes extends Request {

  def hdel[K: ByteStringSerializer, KK:ByteStringSerializer](key: K, fields: KK*): Future[Long] =
    send(Hdel(key, fields))

  def hexists[K: ByteStringSerializer, KK:ByteStringSerializer](key: K, field: KK): Future[Boolean] =
    send(Hexists(key, field))

  def hget[K: ByteStringSerializer, KK:ByteStringSerializer](key: K, field: KK): Future[Option[ByteString]] =
    send(Hget(key, field))

  def hgetall[K: ByteStringSerializer](key: K): Future[Map[String, ByteString]] =
    send(Hgetall(key))

  def hincrby[K: ByteStringSerializer, KK:ByteStringSerializer](key: K, fields: KK, increment: Long): Future[Long] =
    send(Hincrby(key, fields, increment))

  def hincrbyfloat[K: ByteStringSerializer, KK:ByteStringSerializer](key: K, fields: KK, increment: Double): Future[Double] =
    send(Hincrbyfloat(key, fields, increment))

  def hkeys[K: ByteStringSerializer](key: K): Future[Seq[String]] =
    send(Hkeys(key))

  def hlen[K: ByteStringSerializer](key: K): Future[Long] =
    send(Hlen(key))

  def hmget[K: ByteStringSerializer, KK:ByteStringSerializer](key: K, fields: KK*): Future[Seq[Option[ByteString]]] =
    send(Hmget(key, fields))

  def hmset[K: ByteStringSerializer, KK:ByteStringSerializer, V: ByteStringSerializer](key: K, keysValues: Map[KK, V]): Future[Boolean] =
    send(Hmset(key, keysValues))

  def hset[K: ByteStringSerializer, KK:ByteStringSerializer, V: ByteStringSerializer](key: K, field: KK, value: V): Future[Boolean] =
    send(Hset(key, field, value))

  def hsetnx[K: ByteStringSerializer, KK:ByteStringSerializer, V: ByteStringSerializer](key: K, field: KK, value: V): Future[Boolean] =
    send(Hsetnx(key, field, value))

  def hvals[K: ByteStringSerializer](key: K): Future[Seq[ByteString]] =
    send(Hvals(key))

}
