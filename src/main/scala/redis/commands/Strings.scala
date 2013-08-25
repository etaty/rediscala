package redis.commands

import akka.util.ByteString
import redis._
import scala.concurrent.Future
import redis.api.strings._
import redis.api._

trait Strings extends Request {

  def append[K: ByteStringSerializer, V: ByteStringSerializer](key: K, value: V): Future[Long] =
    send(Append(key, value))

  def bitcount[K: ByteStringSerializer](key: K): Future[Long] =
    send(Bitcount(key))

  def bitcount[K: ByteStringSerializer](key: K, start: Long, end: Long): Future[Long] =
    send(BitcountRange(key, start, end))

  def bitopAND[K: ByteStringSerializer, KK: ByteStringSerializer](destkey: K, keys: KK*): Future[Long] =
    bitop(AND, destkey, keys: _*)

  def bitopOR[K: ByteStringSerializer, KK: ByteStringSerializer](destkey: K, keys: KK*): Future[Long] =
    bitop(OR, destkey, keys: _*)

  def bitopXOR[K: ByteStringSerializer, KK: ByteStringSerializer](destkey: K, keys: KK*): Future[Long] =
    bitop(XOR, destkey, keys: _*)

  def bitopNOT[K: ByteStringSerializer, KK: ByteStringSerializer](destkey: K, key: KK): Future[Long] =
    bitop(NOT, destkey, key)

  def bitop[K: ByteStringSerializer, KK: ByteStringSerializer](operation: BitOperator, destkey: K, keys: KK*): Future[Long] =
    send(Bitop(operation, destkey, keys))

  def decr[K: ByteStringSerializer](key: K): Future[Long] =
    send(Decr(key))

  def decrby[K: ByteStringSerializer](key: K, decrement: Long): Future[Long] =
    send(Decrby(key, decrement))

  def get[K: ByteStringSerializer](key: K): Future[Option[ByteString]] =
    send(Get(key))

  def getbit[K: ByteStringSerializer](key: K, offset: Long): Future[Boolean] =
    send(Getbit(key, offset))

  def getrange[K: ByteStringSerializer](key: K, start: Long, end: Long): Future[Option[ByteString]] =
    send(Getrange(key, start, end))

  def getset[K: ByteStringSerializer, V: ByteStringSerializer](key: K, value: V): Future[Option[ByteString]] =
    send(Getset(key, value))

  def incr[K: ByteStringSerializer](key: K): Future[Long] =
    send(Incr(key))

  def incrby[K: ByteStringSerializer](key: K, increment: Long): Future[Long] =
    send(Incrby(key, increment))

  def incrbyfloat[K: ByteStringSerializer](key: K, increment: Double): Future[Option[Double]] =
    send(Incrbyfloat(key, increment))

  def mget[K: ByteStringSerializer](keys: K*): Future[Seq[Option[ByteString]]] =
    send(Mget(keys))

  def mset[K: ByteStringSerializer, V: ByteStringSerializer](keysValues: Map[K, V]): Future[Boolean] =
    send(Mset(keysValues))

  def msetnx[K: ByteStringSerializer, V: ByteStringSerializer](keysValues: Map[K, V]): Future[Boolean] =
    send(Msetnx(keysValues))

  def psetex[K: ByteStringSerializer, V: ByteStringSerializer](key: K, milliseconds: Long, value: V): Future[Boolean] =
    send(Psetex(key, milliseconds, value))

  def set[K: ByteStringSerializer, V: ByteStringSerializer](key: K, value: V,
                                                           exSeconds: Option[Long] = None,
                                                           pxMilliseconds: Option[Long] = None,
                                                           NX: Boolean = false,
                                                           XX: Boolean = false): Future[Boolean] = {
    send(Set(key, value, exSeconds, pxMilliseconds, NX, XX))
  }

  def setbit[K: ByteStringSerializer](key: K, offset: Long, value: Boolean): Future[Boolean] =
    send(Setbit(key, offset, value))

  def setex[K: ByteStringSerializer, V: ByteStringSerializer](key: K, seconds: Long, value: V): Future[Boolean] =
    send(Setex(key, seconds, value))

  def setnx[K: ByteStringSerializer, V: ByteStringSerializer](key: K, value: V): Future[Boolean] =
    send(Setnx(key, value))

  def setrange[K: ByteStringSerializer, V: ByteStringSerializer](key: K, offset: Long, value: V): Future[Long] =
    send(Setrange(key, offset, value))

  def strlen[K: ByteStringSerializer](key: K): Future[Long] =
    send(Strlen(key))

}



