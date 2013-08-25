package redis.commands

import redis.{ByteStringSerializer, Request}
import akka.util.ByteString
import scala.concurrent.Future
import redis.api.sets._

trait Sets extends Request {

  def sadd[K: ByteStringSerializer, V: ByteStringSerializer](key: K, members: V*): Future[Long] =
    send(Sadd(key, members))

  def scard[K: ByteStringSerializer](key: K): Future[Long] =
    send(Scard(key))

  def sdiff[K: ByteStringSerializer, KK: ByteStringSerializer](key: K, keys: KK*): Future[Seq[ByteString]] =
    send(Sdiff(key, keys))

  def sdiffstore[KD: ByteStringSerializer, K: ByteStringSerializer, KK: ByteStringSerializer](destination: KD, key: K, keys: KK*): Future[Long] =
    send(Sdiffstore(destination, key, keys))

  def sinter[K: ByteStringSerializer, KK: ByteStringSerializer](key: K, keys: KK*): Future[Seq[ByteString]] =
    send(Sinter(key, keys))

  def sinterstore[KD: ByteStringSerializer, K: ByteStringSerializer, KK: ByteStringSerializer](destination: KD, key: K, keys: KK*): Future[Long] =
    send(Sinterstore(destination, key, keys))

  def sismember[K: ByteStringSerializer, V: ByteStringSerializer](key: K, member: V): Future[Boolean] =
    send(Sismember(key, member))

  def smembers[K: ByteStringSerializer](key: K): Future[Seq[ByteString]] =
    send(Smembers(key))

  def smove[KS: ByteStringSerializer, KD: ByteStringSerializer, V: ByteStringSerializer](source: KS, destination: KD, member: V): Future[Boolean] =
    send(Smove(source, destination, member))

  def spop[K: ByteStringSerializer](key: K): Future[Option[ByteString]] =
    send(Spop(key))

  def srandmember[K: ByteStringSerializer](key: K): Future[Option[ByteString]] =
    send(Srandmember(key))

  def srandmember[K: ByteStringSerializer](key: K, count: Long): Future[Seq[ByteString]] =
    send(Srandmembers(key, count))

  def srem[K: ByteStringSerializer, V: ByteStringSerializer](key: K, members: V*): Future[Long] =
    send(Srem(key, members))

  def sunion[K: ByteStringSerializer, KK: ByteStringSerializer](key: K, keys: KK*): Future[Seq[ByteString]] =
    send(Sunion(key, keys))

  def sunionstore[KD: ByteStringSerializer, K: ByteStringSerializer, KK: ByteStringSerializer](destination: KD, key: K, keys: KK*): Future[Long] =
    send(Sunionstore(destination, key, keys))

}
