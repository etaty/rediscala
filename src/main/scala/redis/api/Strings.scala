package redis.api.strings

import redis._
import org.apache.pekko.util.ByteString
import redis.protocol.{RedisReply, MultiBulk, Status, RedisProtocolRequest}
import redis.api.BitOperator

case class Append[K, V](key: K, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("APPEND", Seq(keyAsString, convert.serialize(value)))
}

case class Bitcount[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("BITCOUNT", Seq(keyAsString))
}

case class BitcountRange[K](key: K, start: Long, end: Long)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("BITCOUNT", Seq(keyAsString, ByteString(start.toString), ByteString(end.toString)))
}

case class Bitop[K, KK](operation: BitOperator, destkey: K, keys: Seq[KK])(implicit redisKey: ByteStringSerializer[K], redisKeys: ByteStringSerializer[KK])
  extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("BITOP", Seq(ByteString(operation.toString), redisKey.serialize(destkey)) ++ keys.map(redisKeys.serialize))
}

case class Bitpos[K](key: K, bit: Long, start: Long = 0, end: Long = -1)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("BITPOS", Seq(keyAsString, ByteString(bit.toString), ByteString(start.toString), ByteString(end.toString)))
}

case class Decr[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("DECR", Seq(keyAsString))
}

case class Decrby[K](key: K, decrement: Long)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("DECRBY", Seq(keyAsString, ByteString(decrement.toString)))
}

case class Get[K, R](key: K)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R]) extends SimpleClusterKey[K] with RedisCommandBulkOptionByteString[R]{
  val isMasterOnly = false
  val encodedRequest: ByteString = RedisProtocolRequest.multiBulk("GET", Seq(keyAsString))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Getbit[K](key: K, offset: Long)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GETBIT", Seq(keyAsString, ByteString(offset.toString)))
}

case class Getrange[K, R](key: K, start: Long, end: Long)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R])
  extends SimpleClusterKey[K] with RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GETRANGE", Seq(keyAsString, ByteString(start.toString), ByteString(end.toString)))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Getset[K, V, R](key: K, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V], deserializerR: ByteStringDeserializer[R])
  extends SimpleClusterKey[K] with RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GETSET", Seq(keyAsString, convert.serialize(value)))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Incr[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("INCR", Seq(keyAsString))
}

case class Incrby[K](key: K, increment: Long)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("INCRBY", Seq(keyAsString, ByteString(increment.toString)))
}

case class Incrbyfloat[K](key: K, increment: Double)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandBulkOptionDouble {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("INCRBYFLOAT", Seq(keyAsString, ByteString(increment.toString)))
}

case class Mget[K, R](keys: Seq[K])(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R])
  extends MultiClusterKey[K] with RedisCommandMultiBulk[Seq[Option[R]]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("MGET", keys.map(redisKey.serialize))

  def decodeReply(mb: MultiBulk) = mb.responses.map(res => {
    res.map(_.asOptByteString.map(deserializerR.deserialize))
  }).get
}

case class Mset[K, V](keysValues: Map[K, V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends  ClusterKey with RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("MSET",
    keysValues.foldLeft(Seq[ByteString]())({
      case (acc, e) => redisKey.serialize(e._1) +: convert.serialize(e._2) +: acc
    }))

  override def getSlot(): Int = MultiClusterKey.getHeadSlot(redisKey,keysValues.keys.toSeq)
}

case class Msetnx[K, V](keysValues: Map[K, V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends  RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("MSETNX", keysValues.foldLeft(Seq[ByteString]())({
    case (acc, e) => redisKey.serialize(e._1) +: convert.serialize(e._2) +: acc
  }))
}

case class Psetex[K, V](key: K, milliseconds: Long, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends SimpleClusterKey[K] with RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("PSETEX", Seq(keyAsString, ByteString(milliseconds.toString), convert.serialize(value)))
}

case class Set[K, V](key: K, value: V, exSeconds: Option[Long] = None, pxMilliseconds: Option[Long] = None,
                     NX: Boolean = false, XX: Boolean = false)
                    (implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends SimpleClusterKey[K] with RedisCommandRedisReply[Boolean] {
  val isMasterOnly = true
  val encodedRequest: ByteString = {
    val builder = Seq.newBuilder[ByteString]

    builder.+=(redisKey.serialize(key))
    builder.+=(convert.serialize(value))

    if (NX)
      builder += ByteString("NX")
    else if (XX)
      builder += ByteString("XX")

    if(exSeconds.isDefined) {
      builder += ByteString("EX")
      builder += ByteString(exSeconds.get.toString)
    } else if(pxMilliseconds.isDefined) {
      builder += ByteString("PX")
      builder += ByteString(pxMilliseconds.get.toString)
    }

    RedisProtocolRequest.multiBulk("SET", builder.result())
  }

  def decodeReply(redisReply: RedisReply) = redisReply match {
    case s: Status => s.toBoolean
    case _ => false
  }
}

case class Setbit[K](key: K, offset: Long, value: Boolean)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SETBIT", Seq(keyAsString, ByteString(offset.toString), ByteString(if (value) "1" else "0")))
}

case class Setex[K, V](key: K, seconds: Long, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends SimpleClusterKey[K] with RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SETEX", Seq(keyAsString, ByteString(seconds.toString), convert.serialize(value)))
}

case class Setnx[K, V](key: K, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SETNX", Seq(keyAsString, convert.serialize(value)))
}

case class Setrange[K, V](key: K, offset: Long, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SETRANGE", Seq(keyAsString, ByteString(offset.toString), convert.serialize(value)))
}

case class Strlen[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("STRLEN", Seq(keyAsString))
}
