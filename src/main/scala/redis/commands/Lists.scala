package redis.commands

import redis.{RedisValueConverter, Request}
import akka.util.ByteString
import scala.concurrent.Future
import redis.api.lists._
import redis.api.{AFTER, BEFORE, ListPivot}

trait Lists extends Request {

  def lindex(key: String, index: Long): Future[Option[ByteString]] =
    send(Lindex(key, index))

  def linsertAfter[A](key: String, pivot: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Long] =
    linsert(key, AFTER, pivot, value)

  def linsertBefore[A](key: String, pivot: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Long] =
    linsert(key, BEFORE, pivot, value)

  def linsert[A](key: String, beforeAfter: ListPivot, pivot: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Linsert(key, beforeAfter, pivot, value))

  def llen(key: String): Future[Long] =
    send(Llen(key))

  def lpop(key: String): Future[Option[ByteString]] =
    send(Lpop(key))

  def lpush[A](key: String, values: A*)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Lpush(key, values))

  def lpushx[A](key: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Lpushx(key, value))

  def lrange(key: String, start: Long, stop: Long): Future[Seq[ByteString]] =
    send(Lrange(key, start, stop))

  def lrem[A](key: String, count: Long, value: A)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Lrem(key, count, value))

  def lset[A](key: String, index: Long, value: A)(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send(Lset(key, index, value))

  def ltrim(key: String, start: Long, stop: Long): Future[Boolean] =
    send(Ltrim(key, start, stop))

  def rpop(key: String): Future[Option[ByteString]] =
    send(Rpop(key))

  def rpoplpush(source: String, destination: String): Future[Option[ByteString]] =
    send(Rpoplpush(source, destination))

  def rpush[A](key: String, values: A*)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Rpush(key, values))

  def rpushx[A](key: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Rpushx(key, value))

}