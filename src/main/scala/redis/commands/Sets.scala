package redis.commands

import redis.{RedisValueConverter, Request}
import akka.util.ByteString
import scala.concurrent.Future
import redis.api.sets._

trait Sets extends Request {

  def sadd[A](key: String, members: A*)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Sadd(key, members))

  def scard(key: String): Future[Long] =
    send(Scard(key))

  def sdiff(key: String, keys: String*): Future[Seq[ByteString]] =
    send(Sdiff(key, keys))

  def sdiffstore(destination: String, key: String, keys: String*): Future[Long] =
    send(Sdiffstore(destination, key, keys))

  def sinter(key: String, keys: String*): Future[Seq[ByteString]] =
    send(Sinter(key, keys))

  def sinterstore(destination: String, key: String, keys: String*): Future[Long] =
    send(Sinterstore(destination, key, keys))

  def sismember[A](key: String, member: A)(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send(Sismember(key, member))

  def smembers(key: String): Future[Seq[ByteString]] =
    send(Smembers(key))

  def smove[A](source: String, destination: String, member: A)(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send(Smove(source, destination, member))

  def spop(key: String): Future[Option[ByteString]] =
    send(Spop(key))

  def srandmember(key: String): Future[Option[ByteString]] =
    send(Srandmember(key))

  def srandmember(key: String, count: Long): Future[Seq[ByteString]] =
    send(Srandmembers(key, count))

  def srem[A](key: String, members: A*)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Srem(key, members))

  def sunion(key: String, keys: String*): Future[Seq[ByteString]] =
    send(Sunion(key, keys))

  def sunionstore(destination: String, key: String, keys: String*): Future[Long] =
    send(Sunionstore(destination, key, keys))

}
