package redis.commands

import redis._
import scala.concurrent.Future
import redis.api.strings._
import redis.api._

trait Strings extends Request {

  def append[V: ByteStringSerializer](key: String, value: V): Future[Long] =
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

  def get[R: ByteStringDeserializer](key: String): Future[Option[R]] =
    send(Get(key))

  def getbit(key: String, offset: Long): Future[Boolean] =
    send(Getbit(key, offset))

  def getrange[R: ByteStringDeserializer](key: String, start: Long, end: Long): Future[Option[R]] =
    send(Getrange(key, start, end))

  def getset[V: ByteStringSerializer, R: ByteStringDeserializer](key: String, value: V): Future[Option[R]] =
    send(Getset(key, value))

  def incr(key: String): Future[Long] =
    send(Incr(key))

  def incrby(key: String, increment: Long): Future[Long] =
    send(Incrby(key, increment))

  def incrbyfloat(key: String, increment: Double): Future[Option[Double]] =
    send(Incrbyfloat(key, increment))

  def mget[R: ByteStringDeserializer](keys: String*): Future[Seq[Option[R]]] =
    send(Mget(keys))

  def mset[V: ByteStringSerializer](keysValues: Map[String, V]): Future[Boolean] =
    send(Mset(keysValues))

  def msetnx[V: ByteStringSerializer](keysValues: Map[String, V]): Future[Boolean] =
    send(Msetnx(keysValues))

  def psetex[V: ByteStringSerializer](key: String, milliseconds: Long, value: V): Future[Boolean] =
    send(Psetex(key, milliseconds, value))

  def set[V: ByteStringSerializer](key: String, value: V,
                                   exSeconds: Option[Long] = None,
                                   pxMilliseconds: Option[Long] = None,
                                   NX: Boolean = false,
                                   XX: Boolean = false): Future[Boolean] = {
    send(Set(key, value, exSeconds, pxMilliseconds, NX, XX))
  }

  def setbit(key: String, offset: Long, value: Boolean): Future[Boolean] =
    send(Setbit(key, offset, value))

  def setex[V: ByteStringSerializer](key: String, seconds: Long, value: V): Future[Boolean] =
    send(Setex(key, seconds, value))

  def setnx[V: ByteStringSerializer](key: String, value: V): Future[Boolean] =
    send(Setnx(key, value))

  def setrange[V: ByteStringSerializer](key: String, offset: Long, value: V): Future[Long] =
    send(Setrange(key, offset, value))

  def strlen(key: String): Future[Long] =
    send(Strlen(key))

}



