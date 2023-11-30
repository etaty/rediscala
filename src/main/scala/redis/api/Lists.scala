package redis.api.lists

import redis._
import org.apache.pekko.util.ByteString
import redis.api.ListPivot
import redis.protocol.MultiBulk

case class Lindex[K, R](key: K, index: Long)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R]) extends SimpleClusterKey[K] with RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("LINDEX", Seq(keyAsString, ByteString(index.toString)))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Linsert[K, KP, V](key: K, beforeAfter: ListPivot, pivot: KP, value: V)
                            (implicit redisKey: ByteStringSerializer[K], redisPivot: ByteStringSerializer[KP], convert: ByteStringSerializer[V])
  extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("LINSERT", Seq(keyAsString, ByteString(beforeAfter.toString), redisPivot.serialize(pivot), convert.serialize(value)))
}

case class Llen[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("LLEN", Seq(keyAsString))
}

case class Lpop[K, R](key: K)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R]) extends SimpleClusterKey[K] with RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("LPOP", Seq(keyAsString))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Lpush[K, V](key: K, values: Seq[V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("LPUSH", keyAsString +: values.map(v => convert.serialize(v)))
}


case class Lpushx[K, V](key: K, values: Seq[V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("LPUSHX", keyAsString +: values.map(v => convert.serialize(v)))
}

case class Lrange[K, R](key: K, start: Long, stop: Long)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R]) extends SimpleClusterKey[K] with RedisCommandMultiBulk[Seq[R]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("LRANGE", Seq(keyAsString, ByteString(start.toString), ByteString(stop.toString)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqByteString(mb)
}

case class Lrem[K, V](key: K, count: Long, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("LREM", Seq(keyAsString, ByteString(count.toString), convert.serialize(value)))
}

case class Lset[K, V](key: K, index: Long, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends SimpleClusterKey[K] with RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("LSET", Seq(keyAsString, ByteString(index.toString), convert.serialize(value)))
}

case class Ltrim[K](key: K, start: Long, stop: Long)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("LTRIM", Seq(keyAsString, ByteString(start.toString), ByteString(stop.toString)))
}

case class Rpop[K, R](key: K)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R]) extends SimpleClusterKey[K] with RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("RPOP", Seq(keyAsString))
  val deserializer: ByteStringDeserializer[R] = deserializerR

}

case class Rpoplpush[KS, KD, R](source: KS, destination: KD)(implicit sourceSer: ByteStringSerializer[KS], destSer: ByteStringSerializer[KD], deserializerR: ByteStringDeserializer[R]) extends  RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("RPOPLPUSH", Seq(sourceSer.serialize(source), destSer.serialize(destination)))
  val deserializer: ByteStringDeserializer[R] = deserializerR

}

case class Rpush[K, V](key: K, values: Seq[V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("RPUSH", keyAsString +: values.map(v => convert.serialize(v)))
}

case class Rpushx[K, V](key: K, values: Seq[V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("RPUSHX", keyAsString +: values.map(v => convert.serialize(v)))
}