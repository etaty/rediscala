package redis.commands

import redis._
import scala.concurrent.Future
import scala.concurrent.duration._
import redis.api.{Order, LimitOffsetCount}
import redis.api.keys._

trait Keys extends Request {

  def del(keys: String*): Future[Long] =
    send(Del(keys))

  def dump[R: ByteStringDeserializer](key: String): Future[Option[R]] =
    send(Dump(key))

  def exists(key: String): Future[Boolean] =
    send(Exists(key))

  def existsB[K](key: K, prefix: Option[String] = None)(implicit redisKey: ByteStringSerializer[K]): Future[Boolean] =
    send(Exists(key))

  def expire(key: String, seconds: Long): Future[Boolean] =
    send(Expire(key, seconds))

  def expireat(key: String, seconds: Long): Future[Boolean] =
    send(Expireat(key, seconds))

  def keys(pattern: String): Future[Seq[String]] =
    send(Keys(pattern))

  def migrate(host: String, port: Int, key: String, destinationDB: Int, timeout: FiniteDuration): Future[Boolean] = {
    send(Migrate(host, port, key, destinationDB, timeout))
  }

  def move(key: String, db: Int): Future[Boolean] =
    send(Move(key, db))

  def objectRefcount(key: String): Future[Option[Long]] =
    send(ObjectRefcount(key))

  def objectIdletime(key: String): Future[Option[Long]] =
    send(ObjectIdletime(key))

  def objectEncoding(key: String): Future[Option[String]] =
    send(ObjectEncoding(key))

  def persist(key: String): Future[Boolean] =
    send(Persist(key))

  def pexpire(key: String, milliseconds: Long): Future[Boolean] =
    send(Pexpire(key, milliseconds))

  def pexpireat(key: String, millisecondsTimestamp: Long): Future[Boolean] =
    send(Pexpireat(key, millisecondsTimestamp))

  def pttl(key: String): Future[Long] =
    send(Pttl(key))

  def randomkey[R: ByteStringDeserializer](): Future[Option[R]] =
    send(Randomkey())

  def rename(key: String, newkey: String): Future[Boolean] =
    send(Rename(key, newkey))

  def renamenx(key: String, newkey: String): Future[Boolean] =
    send(Renamex(key, newkey))

  def restore[V: ByteStringSerializer](key: String, ttl: Long = 0, serializedValue: V): Future[Boolean] =
    send(Restore(key, ttl, serializedValue))

  def sort[R: ByteStringDeserializer](key: String,
                                    byPattern: Option[String] = None,
                                    limit: Option[LimitOffsetCount] = None,
                                    getPatterns: Seq[String] = Seq(),
                                    order: Option[Order] = None,
                                    alpha: Boolean = false): Future[Seq[R]] = {
    send(Sort(key, byPattern, limit, getPatterns, order, alpha))
  }

  def sortStore(key: String,
                                                                   byPattern: Option[String] = None,
                                                                   limit: Option[LimitOffsetCount] = None,
                                                                   getPatterns: Seq[String] = Seq(),
                                                                   order: Option[Order] = None,
                                                                   alpha: Boolean = false,
                                                                   store: String): Future[Long] = {
    send(SortStore(key, byPattern, limit, getPatterns, order, alpha, store))
  }

  def ttl(key: String): Future[Long] =
    send(Ttl(key))

  def `type`(key: String): Future[String] =
    send(Type(key))

}
