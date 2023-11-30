package redis.api.transactions

import redis.{RedisCommandMultiBulk, RedisCommandStatusBoolean}
import org.apache.pekko.util.ByteString
import redis.protocol.MultiBulk

case object Multi extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("MULTI")
}

case object Exec extends RedisCommandMultiBulk[MultiBulk] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("EXEC")

  def decodeReply(r: MultiBulk): MultiBulk = r
}

case class Watch(keys: Set[String]) extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("WATCH", keys.map(ByteString.apply).toSeq)
}