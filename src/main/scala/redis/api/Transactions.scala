package redis.api.transactions

import redis.{RedisCommandMultiBulk, RedisCommandStatusBoolean}
import akka.util.ByteString
import redis.protocol.MultiBulk

case object Multi extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("MULTI")
}

case object Exec extends RedisCommandMultiBulk[MultiBulk] {
  val encodedRequest: ByteString = encode("EXEC")

  def decodeReply(r: MultiBulk): MultiBulk = r
}

case class Watch(keys: Set[String]) extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("WATCH", keys.map(ByteString.apply).toSeq)
}