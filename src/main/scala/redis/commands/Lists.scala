package redis.commands

import redis.{ByteStringSerializer, Request}
import akka.util.ByteString
import scala.concurrent.Future
import redis.api.lists._
import redis.api.{AFTER, BEFORE, ListPivot}

trait Lists extends Request {

  def lindex[K: ByteStringSerializer](key: K, index: Long): Future[Option[ByteString]] =
    send(Lindex(key, index))

  def linsertAfter[K: ByteStringSerializer, KP: ByteStringSerializer, V: ByteStringSerializer](key: K, pivot: KP, value: V): Future[Long] =
    linsert(key, AFTER, pivot, value)

  def linsertBefore[K: ByteStringSerializer, KP: ByteStringSerializer, V: ByteStringSerializer](key: K, pivot: KP, value: V): Future[Long] =
    linsert(key, BEFORE, pivot, value)

  def linsert[K: ByteStringSerializer, KP: ByteStringSerializer, V: ByteStringSerializer](key: K, beforeAfter: ListPivot, pivot: KP, value: V): Future[Long] =
    send(Linsert(key, beforeAfter, pivot, value))

  def llen[K: ByteStringSerializer](key: K): Future[Long] =
    send(Llen(key))

  def lpop[K: ByteStringSerializer](key: K): Future[Option[ByteString]] =
    send(Lpop(key))

  def lpush[K: ByteStringSerializer, V: ByteStringSerializer](key: K, values: V*): Future[Long] =
    send(Lpush(key, values))

  def lpushx[K: ByteStringSerializer, V: ByteStringSerializer](key: K, value: V): Future[Long] =
    send(Lpushx(key, value))

  def lrange[K: ByteStringSerializer](key: K, start: Long, stop: Long): Future[Seq[ByteString]] =
    send(Lrange(key, start, stop))

  def lrem[K: ByteStringSerializer, V: ByteStringSerializer](key: K, count: Long, value: V): Future[Long] =
    send(Lrem(key, count, value))

  def lset[K: ByteStringSerializer, V: ByteStringSerializer](key: K, index: Long, value: V): Future[Boolean] =
    send(Lset(key, index, value))

  def ltrim[K: ByteStringSerializer](key: K, start: Long, stop: Long): Future[Boolean] =
    send(Ltrim(key, start, stop))

  def rpop[K: ByteStringSerializer](key: K): Future[Option[ByteString]] =
    send(Rpop(key))

  def rpoplpush[KS: ByteStringSerializer, KD: ByteStringSerializer](source: KS, destination: KD): Future[Option[ByteString]] =
    send(Rpoplpush(source, destination))

  def rpush[K: ByteStringSerializer, V: ByteStringSerializer](key: K, values: V*): Future[Long] =
    send(Rpush(key, values))

  def rpushx[K: ByteStringSerializer, V: ByteStringSerializer](key: K, value: V): Future[Long] =
    send(Rpushx(key, value))

}