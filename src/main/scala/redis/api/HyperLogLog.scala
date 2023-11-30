package redis.api.hyperloglog

import org.apache.pekko.util.ByteString
import redis.{RedisCommandIntegerLong, RedisCommandStatusBoolean, ByteStringSerializer}

case class Pfadd[K, V](key: K, values: Seq[V])(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("PFADD", redisKey.serialize(key) +: values.map(convert.serialize))
}

case class Pfcount[K](keys: Seq[K])(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("PFCOUNT", keys.map(redisKey.serialize))
}

case class Pfmerge[K](destKey: K, sourceKeys: Seq[K])(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("PFMERGE", redisKey.serialize(destKey) +: sourceKeys.map(redisKey.serialize))
}