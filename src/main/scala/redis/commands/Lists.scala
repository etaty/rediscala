package redis.commands

import redis.{ByteStringSerializer, Request}
import akka.util.ByteString
import scala.concurrent.Future
import redis.api.lists._
import redis.api.{AFTER, BEFORE, ListPivot}

trait Lists extends Request {

  def lindex(key: String, index: Long): Future[Option[ByteString]] =
    send(Lindex(key, index))

  def linsertAfter[V: ByteStringSerializer](key: String, pivot: String, value: V): Future[Long] =
    linsert(key, AFTER, pivot, value)

  def linsertBefore[V: ByteStringSerializer](key: String, pivot: String, value: V): Future[Long] =
    linsert(key, BEFORE, pivot, value)

  def linsert[V: ByteStringSerializer](key: String, beforeAfter: ListPivot, pivot: String, value: V): Future[Long] =
    send(Linsert(key, beforeAfter, pivot, value))

  def llen(key: String): Future[Long] =
    send(Llen(key))

  def lpop(key: String): Future[Option[ByteString]] =
    send(Lpop(key))

  def lpush[V: ByteStringSerializer](key: String, values: V*): Future[Long] =
    send(Lpush(key, values))

  def lpushx[V: ByteStringSerializer](key: String, value: V): Future[Long] =
    send(Lpushx(key, value))

  def lrange(key: String, start: Long, stop: Long): Future[Seq[ByteString]] =
    send(Lrange(key, start, stop))

  def lrem[V: ByteStringSerializer](key: String, count: Long, value: V): Future[Long] =
    send(Lrem(key, count, value))

  def lset[V: ByteStringSerializer](key: String, index: Long, value: V): Future[Boolean] =
    send(Lset(key, index, value))

  def ltrim(key: String, start: Long, stop: Long): Future[Boolean] =
    send(Ltrim(key, start, stop))

  def rpop(key: String): Future[Option[ByteString]] =
    send(Rpop(key))

  def rpoplpush(source: String, destination: String): Future[Option[ByteString]] =
    send(Rpoplpush(source, destination))

  def rpush[V: ByteStringSerializer](key: String, values: V*): Future[Long] =
    send(Rpush(key, values))

  def rpushx[V: ByteStringSerializer](key: String, value: V): Future[Long] =
    send(Rpushx(key, value))

}