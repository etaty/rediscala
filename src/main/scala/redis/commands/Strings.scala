package redis.commands

import akka.util.ByteString
import redis._
import scala.concurrent.Future
import redis.api.strings._
import redis.api._

trait Strings extends Request {

  def append[A](key: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Append(key, value))

  def bitcount(key: String): Future[Long] =
    send(Bitcount(key))

  def bitcount(key: String, start: Long, end: Long): Future[Long] =
    send(BitcountRange(key, start, end))

  def bitopAND(destkey: String, keys: String*): Future[Long] =
    bitop(AND, destkey, keys: _*)

  def bitopOR(destkey: String, keys: String*): Future[Long] =
    bitop(OR, destkey, keys: _*)

  def bitopXOR(destkey: String, keys: String*): Future[Long] =
    bitop(XOR, destkey, keys: _*)

  def bitopNOT(destkey: String, key: String): Future[Long] =
    bitop(NOT, destkey, key)

  def bitop(operation: BitOperator, destkey: String, keys: String*): Future[Long] =
    send(Bitop(operation, destkey, keys))

  def decr(key: String): Future[Long] =
    send(Decr(key))

  def decrby(key: String, decrement: Long): Future[Long] =
    send(Decrby(key, decrement))

  def getT[T: ByteStringDeserializer](key: String): Future[Option[T]] =
    send(Get(key))

  def get(key: String): Future[Option[ByteString]] =
    send(Get[ByteString](key))

  def getbit(key: String, offset: Long): Future[Boolean] =
    send(Getbit(key, offset))

  def getrange(key: String, start: Long, end: Long): Future[Option[ByteString]] =
    send(Getrange(key, start, end))

  def getset[A](key: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Option[ByteString]] =
    send(Getset(key, value))

  def incr(key: String): Future[Long] =
    send(Incr(key))

  def incrby(key: String, increment: Long): Future[Long] =
    send(Incrby(key, increment))

  def incrbyfloat(key: String, increment: Double): Future[Option[Double]] =
    send(Incrbyfloat(key, increment))

  def mget(keys: String*): Future[Seq[Option[ByteString]]] =
    send(Mget(keys))

  def mset[A](keysValues: Map[String, A])(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send(Mset(keysValues))

  def msetnx[A](keysValues: Map[String, A])(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send(Msetnx(keysValues))

  def psetex[A](key: String, milliseconds: Long, value: A)(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send(Psetex(key, milliseconds, value))

  def set[A](key: String, value: A, exSeconds: Option[Long] = None, pxMilliseconds: Option[Long] = None, NX: Boolean = false, XX: Boolean = false)
            (implicit convert: RedisValueConverter[A]): Future[Boolean] = {
    send(Set(key, value, exSeconds, pxMilliseconds, NX, XX))
  }

  def setbit(key: String, offset: Long, value: Boolean): Future[Boolean] =
    send(Setbit(key, offset, value))

  def setex[A](key: String, seconds: Long, value: A)(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send(Setex(key, seconds, value))

  def setnx[A](key: String, value: A)(implicit convert: RedisValueConverter[A]): Future[Boolean] =
    send(Setnx(key, value))

  def setrange[A](key: String, offset: Long, value: A)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Setrange(key, offset, value))

  def strlen(key: String): Future[Long] =
    send(Strlen(key))

}



