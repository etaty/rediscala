package redis.commands

import akka.util.ByteString
import redis._
import scala.concurrent.Future
import scala.concurrent.duration._
import redis.api.{Order, LimitOffsetCount}
import redis.api.keys._

trait Keys extends Request {

  def del[K: ByteStringSerializer](keys: K*): Future[Long] =
    send(Del(keys))

  def dump[K: ByteStringSerializer](key: K): Future[Option[ByteString]] =
    send(Dump(key))

  def exists[K: ByteStringSerializer](key: K): Future[Boolean] =
    send(Exists(key))

  def expire[K: ByteStringSerializer](key: K, seconds: Long): Future[Boolean] =
    send(Expire(key, seconds))

  def expireat[K: ByteStringSerializer](key: K, seconds: Long): Future[Boolean] =
    send(Expireat(key, seconds))

  def keys(pattern: String): Future[Seq[String]] =
    send(Keys(pattern))

  def migrate[K: ByteStringSerializer](host: String, port: Int, key: K, destinationDB: Int, timeout: FiniteDuration): Future[Boolean] = {
    send(Migrate(host, port, key, destinationDB, timeout))
  }

  def move[K: ByteStringSerializer](key: K, db: Int): Future[Boolean] =
    send(Move(key, db))

  def objectRefcount[K: ByteStringSerializer](key: K): Future[Option[Long]] =
    send(ObjectRefcount(key))

  def objectIdletime[K: ByteStringSerializer](key: K): Future[Option[Long]] =
    send(ObjectIdletime(key))

  def objectEncoding[K: ByteStringSerializer](key: K): Future[Option[String]] =
    send(ObjectEncoding(key))

  def persist[K: ByteStringSerializer](key: K): Future[Boolean] =
    send(Persist(key))

  def pexpire[K: ByteStringSerializer](key: K, milliseconds: Long): Future[Boolean] =
    send(Pexpire(key, milliseconds))

  def pexpireat[K: ByteStringSerializer](key: K, millisecondsTimestamp: Long): Future[Boolean] =
    send(Pexpireat(key, millisecondsTimestamp))

  def pttl[K: ByteStringSerializer](key: K): Future[Long] =
    send(Pttl(key))

  def randomkey(): Future[Option[ByteString]] =
    send(Randomkey)

  def rename[K: ByteStringSerializer, NK: ByteStringSerializer](key: K, newkey: NK): Future[Boolean] =
    send(Rename(key, newkey))

  def renamenx[K: ByteStringSerializer, NK: ByteStringSerializer](key: K, newkey: NK): Future[Boolean] =
    send(Renamex(key, newkey))

  def restore[K: ByteStringSerializer, V: ByteStringSerializer](key: K, ttl: Long = 0, serializedValue: V): Future[Boolean] =
    send(Restore(key, ttl, serializedValue))

  def sort[K: ByteStringSerializer](key: K,
                                    byPattern: Option[String] = None,
                                    limit: Option[LimitOffsetCount] = None,
                                    getPatterns: Seq[String] = Seq(),
                                    order: Option[Order] = None,
                                    alpha: Boolean = false): Future[Seq[ByteString]] = {
    send(Sort(key, byPattern, limit, getPatterns, order, alpha))
  }

  def sortStore[K: ByteStringSerializer, KS: ByteStringSerializer](key: K,
                                                                   byPattern: Option[String] = None,
                                                                   limit: Option[LimitOffsetCount] = None,
                                                                   getPatterns: Seq[String] = Seq(),
                                                                   order: Option[Order] = None,
                                                                   alpha: Boolean = false,
                                                                   store: KS): Future[Long] = {
    send(SortStore(key, byPattern, limit, getPatterns, order, alpha, store))
  }

  def ttl[K: ByteStringSerializer](key: K): Future[Long] =
    send(Ttl(key))

  def `type`[K: ByteStringSerializer](key: K): Future[String] =
    send(Type(key))

}
