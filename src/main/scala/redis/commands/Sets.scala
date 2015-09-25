package redis.commands

import redis.{ByteStringDeserializer, ByteStringSerializer, Request}
import scala.concurrent.Future
import redis.api.sets._

trait Sets extends Request {

  def sadd[V: ByteStringSerializer](key: String, members: V*): Future[Long] =
    send(Sadd(key, members))

  def scard(key: String): Future[Long] =
    send(Scard(key))

  def sdiff[R: ByteStringDeserializer](key: String, keys: String*): Future[Seq[R]] =
    send(Sdiff(key, keys))

  def sdiffstore(destination: String, key: String, keys: String*): Future[Long] =
    send(Sdiffstore(destination, key, keys))

  def sinter[R: ByteStringDeserializer](key: String, keys: String*): Future[Seq[R]] =
    send(Sinter(key, keys))

  def sinterstore(destination: String, key: String, keys: String*): Future[Long] =
    send(Sinterstore(destination, key, keys))

  def sismember[V: ByteStringSerializer](key: String, member: V): Future[Boolean] =
    send(Sismember(key, member))

  def smembers[R: ByteStringDeserializer](key: String): Future[Seq[R]] =
    send(Smembers(key))

  def smove[V: ByteStringSerializer](source: String, destination: String, member: V): Future[Boolean] =
    send(Smove(source, destination, member))

  def spop[R: ByteStringDeserializer](key: String): Future[Option[R]] =
    send(Spop(key))

  def srandmember[R: ByteStringDeserializer](key: String): Future[Option[R]] =
    send(Srandmember(key))

  def srandmember[R: ByteStringDeserializer](key: String, count: Long): Future[Seq[R]] =
    send(Srandmembers(key, count))

  def srem[V: ByteStringSerializer](key: String, members: V*): Future[Long] =
    send(Srem(key, members))

  def sunion[R: ByteStringDeserializer](key: String, keys: String*): Future[Seq[R]] =
    send(Sunion(key, keys))

  def sunionstore(destination: String, key: String, keys: String*): Future[Long] =
    send(Sunionstore(destination, key, keys))

  def sscan[R: ByteStringDeserializer](key: String, cursor: Int = 0, count: Option[Int] = None, matchGlob: Option[String] = None): Future[(Int, Seq[R])] =
    send(Sscan(key, cursor, count, matchGlob))
}
