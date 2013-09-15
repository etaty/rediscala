package redis.api.strings

import redis._
import akka.util.ByteString
import redis.protocol.{RedisReply, MultiBulk, Status, RedisProtocolRequest}
import redis.api.BitOperator

case class Append[K, V](key: K, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("APPEND", Seq(redisKey.serialize(key), convert.serialize(value)))
}

case class Bitcount[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("BITCOUNT", Seq(redisKey.serialize(key)))
}

case class BitcountRange[K](key: K, start: Long, end: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("BITCOUNT", Seq(redisKey.serialize(key), ByteString(start.toString), ByteString(end.toString)))
}

case class Bitop[K, KK](operation: BitOperator, destkey: K, keys: Seq[KK])(implicit redisKey: ByteStringSerializer[K], redisKeys: ByteStringSerializer[KK])
  extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("BITOP", Seq(ByteString(operation.toString), redisKey.serialize(destkey)) ++ keys.map(redisKeys.serialize))
}

case class Decr[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("DECR", Seq(redisKey.serialize(key)))
}

case class Decrby[K](key: K, decrement: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("DECRBY", Seq(redisKey.serialize(key), ByteString(decrement.toString)))
}

case class Get[K, R](key: K)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R]) extends RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = RedisProtocolRequest.multiBulk("GET", Seq(redisKey.serialize(key)))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Getbit[K](key: K, offset: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GETBIT", Seq(redisKey.serialize(key), ByteString(offset.toString)))
}

case class Getrange[K, R](key: K, start: Long, end: Long)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R])
  extends RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GETRANGE", Seq(redisKey.serialize(key), ByteString(start.toString), ByteString(end.toString)))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Getset[K, V, R](key: K, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V], deserializerR: ByteStringDeserializer[R])
  extends RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GETSET", Seq(redisKey.serialize(key), convert.serialize(value)))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Incr[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("INCR", Seq(redisKey.serialize(key)))
}

case class Incrby[K](key: K, increment: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("INCRBY", Seq(redisKey.serialize(key), ByteString(increment.toString)))
}

case class Incrbyfloat[K](key: K, increment: Double)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandBulkOptionDouble {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("INCRBYFLOAT", Seq(redisKey.serialize(key), ByteString(increment.toString)))
}

case class Mget[K, R](keys: Seq[K])(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R])
  extends RedisCommandMultiBulk[Seq[Option[R]]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("MGET", keys.map(redisKey.serialize))

  def decodeReply(mb: MultiBulk) = mb.responses.map(res => {
    res.map(_.asOptByteString.map(deserializerR.deserialize))
  }).get
}

case class Mset[K, V](keysValues: Map[K, V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("MSET",
    keysValues.foldLeft(Seq[ByteString]())({
      case (acc, e) => redisKey.serialize(e._1) +: convert.serialize(e._2) +: acc
    }))
}

case class Msetnx[K, V](keysValues: Map[K, V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("MSETNX", keysValues.foldLeft(Seq[ByteString]())({
    case (acc, e) => redisKey.serialize(e._1) +: convert.serialize(e._2) +: acc
  }))
}

case class Psetex[K, V](key: K, milliseconds: Long, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("PSETEX", Seq(redisKey.serialize(key), ByteString(milliseconds.toString), convert.serialize(value)))
}

case class Set[K, V](key: K, value: V, exSeconds: Option[Long] = None, pxMilliseconds: Option[Long] = None,
                     NX: Boolean = false, XX: Boolean = false)
                    (implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandRedisReply[Boolean] {
  val isMasterOnly = true
  val encodedRequest: ByteString = {
    val seq = if (NX) Seq(ByteString("NX")) else if (XX) Seq(ByteString("XX")) else Seq.empty[ByteString]
    val options: Seq[ByteString] = exSeconds.map(t => Seq(ByteString("EX"), ByteString(t.toString)))
      .orElse(pxMilliseconds.map(t => Seq(ByteString("PX"), ByteString(t.toString))))
      .getOrElse(seq)
    val args = redisKey.serialize(key) +: convert.serialize(value) +: options
    RedisProtocolRequest.multiBulk("SET", args)
  }

  def decodeReply(redisReply: RedisReply) = redisReply match {
    case s: Status => s.toBoolean
    case _ => false
  }
}

case class Setbit[K](key: K, offset: Long, value: Boolean)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SETBIT", Seq(redisKey.serialize(key), ByteString(offset.toString), ByteString(if (value) "1" else "0")))
}

case class Setex[K, V](key: K, seconds: Long, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SETEX", Seq(redisKey.serialize(key), ByteString(seconds.toString), convert.serialize(value)))
}

case class Setnx[K, V](key: K, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SETNX", Seq(redisKey.serialize(key), convert.serialize(value)))
}

case class Setrange[K, V](key: K, offset: Long, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SETRANGE", Seq(redisKey.serialize(key), ByteString(offset.toString), convert.serialize(value)))
}

case class Strlen[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("STRLEN", Seq(redisKey.serialize(key)))
}