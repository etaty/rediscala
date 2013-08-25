package redis.api.sets

import redis._
import akka.util.ByteString

case class Sadd[K, V](key: K, members: Seq[V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SADD", redisKey.serialize(key) +: members.map(v => convert.serialize(v)))
}

case class Scard[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SCARD", Seq(redisKey.serialize(key)))
}

case class Sdiff[K, KK](key: K, keys: Seq[KK])(implicit redisKey: ByteStringSerializer[K], redisKeys: ByteStringSerializer[KK]) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("SDIFF", redisKey.serialize(key) +: keys.map(redisKeys.serialize))
}

case class Sdiffstore[KD, K, KK](destination: KD, key: K, keys: Seq[KK])
                                (implicit redisDest: ByteStringSerializer[KD], redisKey: ByteStringSerializer[K], redisKeys: ByteStringSerializer[KK])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SDIFFSTORE", redisDest.serialize(destination) +: redisKey.serialize(key) +: keys.map(redisKeys.serialize))
}

case class Sinter[K, KK](key: K, keys: Seq[KK])(implicit redisKey: ByteStringSerializer[K], redisKeys: ByteStringSerializer[KK]) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("SINTER", redisKey.serialize(key) +: keys.map(redisKeys.serialize))
}

case class Sinterstore[KD, K, KK](destination: KD, key: K, keys: Seq[KK])
                                 (implicit redisDest: ByteStringSerializer[KD], redisKey: ByteStringSerializer[K], redisKeys: ByteStringSerializer[KK])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SINTERSTORE", redisDest.serialize(destination) +: redisKey.serialize(key) +: keys.map(redisKeys.serialize))
}

case class Sismember[K, V](key: K, member: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("SISMEMBER", Seq(redisKey.serialize(key), convert.serialize(member)))
}

case class Smembers[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("SMEMBERS", Seq(redisKey.serialize(key)))
}

case class Smove[KS, KD, V](source: KS, destination: KD, member: V)(implicit redisSource: ByteStringSerializer[KS], redisDest: ByteStringSerializer[KD], convert: ByteStringSerializer[V])
  extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("SMOVE", Seq(redisSource.serialize(source), redisDest.serialize(destination), convert.serialize(member)))
}

case class Spop[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("SPOP", Seq(redisKey.serialize(key)))
}

case class Srandmember[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("SRANDMEMBER", Seq(redisKey.serialize(key)))
}

case class Srandmembers[K](key: K, count: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("SRANDMEMBER", Seq(redisKey.serialize(key), ByteString(count.toString)))
}

case class Srem[K, V](key: K, members: Seq[V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SREM", redisKey.serialize(key) +: members.map(v => convert.serialize(v)))
}

case class Sunion[K, KK](key: K, keys: Seq[KK])(implicit redisKey: ByteStringSerializer[K], redisKeys: ByteStringSerializer[KK]) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("SUNION", redisKey.serialize(key) +: keys.map(redisKeys.serialize))
}


case class Sunionstore[KD, K, KK](destination: KD, key: K, keys: Seq[KK])
                                 (implicit redisDest: ByteStringSerializer[KD], redisKey: ByteStringSerializer[K], redisKeys: ByteStringSerializer[KK])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SUNIONSTORE", redisDest.serialize(destination) +: redisKey.serialize(key) +: keys.map(redisKeys.serialize))
}