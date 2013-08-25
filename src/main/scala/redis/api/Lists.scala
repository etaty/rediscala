package redis.api.lists

import redis._
import akka.util.ByteString
import redis.api.ListPivot
import redis.protocol.MultiBulk

case class Lindex[K](key: K, index: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("LINDEX", Seq(redisKey.serialize(key), ByteString(index.toString)))
}

case class Linsert[K, KP, V](key: K, beforeAfter: ListPivot, pivot: KP, value: V)
                            (implicit redisKey: ByteStringSerializer[K], redisPivot: ByteStringSerializer[KP], convert: ByteStringSerializer[V])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("LINSERT", Seq(redisKey.serialize(key), ByteString(beforeAfter.toString), redisPivot.serialize(pivot), convert.serialize(value)))
}

case class Llen[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("LLEN", Seq(redisKey.serialize(key)))
}

case class Lpop[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("LPOP", Seq(redisKey.serialize(key)))
}

case class Lpush[K, V](key: K, values: Seq[V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("LPUSH", redisKey.serialize(key) +: values.map(v => convert.serialize(v)))
}


case class Lpushx[K, V](key: K, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("LPUSHX", Seq(redisKey.serialize(key), convert.serialize(value)))
}

case class Lrange[K](key: K, start: Long, stop: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandMultiBulk[Seq[ByteString]] {
  val encodedRequest: ByteString = encode("LRANGE", Seq(redisKey.serialize(key), ByteString(start.toString), ByteString(stop.toString)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqByteString(mb)
}

case class Lrem[K, V](key: K, count: Long, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("LREM", Seq(redisKey.serialize(key), ByteString(count.toString), convert.serialize(value)))
}

case class Lset[K, V](key: K, index: Long, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("LSET", Seq(redisKey.serialize(key), ByteString(index.toString), convert.serialize(value)))
}

case class Ltrim[K](key: K, start: Long, stop: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("LTRIM", Seq(redisKey.serialize(key), ByteString(start.toString), ByteString(stop.toString)))
}

case class Rpop[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("RPOP", Seq(redisKey.serialize(key)))
}

case class Rpoplpush[KS, KD](source: KS, destination: KD)(implicit sourceSer: ByteStringSerializer[KS], destSer: ByteStringSerializer[KD]) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("RPOPLPUSH", Seq(sourceSer.serialize(source), destSer.serialize(destination)))
}

case class Rpush[K, V](key: K, values: Seq[V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("RPUSH", redisKey.serialize(key) +: values.map(v => convert.serialize(v)))
}

case class Rpushx[K, V](key: K, value: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("RPUSHX", Seq(redisKey.serialize(key), convert.serialize(value)))
}