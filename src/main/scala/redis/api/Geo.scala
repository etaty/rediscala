package redis.api.geo

import akka.util.ByteString
import redis._
import redis.protocol.MultiBulk

case class GeoAdd[K](key: K, lat: Double, lng: Double, loc: String)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GEOADD", Seq(redisKey.serialize(key), ByteString(lng.toString), ByteString(lat.toString), ByteString(loc)))
}

case class GeoRadius[K](key: K, lat: Double, lng: Double, radius: Double, dim: String)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandMultiBulk[Seq[String]] {
    val isMasterOnly = false
    val encodedRequest: ByteString = encode("GEORADIUS", Seq(redisKey.serialize(key), ByteString(lng.toString), ByteString(lat.toString), ByteString(radius.toString), ByteString(dim)))

    def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqString(mb)
}
