package redis.api.blists

import scala.concurrent.duration.{Duration, FiniteDuration}
import redis.{RedisCommandRedisReply, RedisCommandMultiBulk, MultiBulkConverter}
import akka.util.ByteString
import redis.protocol.{RedisReply, MultiBulk, Bulk}

case class Blpop(keys: Seq[String], timeout: FiniteDuration = Duration.Zero)
  extends BXpop("BLPOP")


case class Brpop(keys: Seq[String], timeout: FiniteDuration = Duration.Zero)
  extends BXpop("BRPOP")


private[redis] abstract class BXpop(command: String) extends RedisCommandMultiBulk[Option[(String, ByteString)]] {
  val keys: Seq[String]
  val timeout: FiniteDuration

  val encodedRequest: ByteString = encode(command, keys.map(ByteString.apply).seq ++ Seq(ByteString(timeout.toSeconds.toString)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toOptionStringByteString(mb)
}

case class Brpopplush(source: String, destination: String, timeout: FiniteDuration = Duration.Zero)
  extends RedisCommandRedisReply[Option[ByteString]] {
  val encodedRequest: ByteString = encode("BRPOPLPUSH",
    Seq(ByteString(source), ByteString(destination), ByteString(timeout.toSeconds.toString)))

  def decodeReply(redisReply: RedisReply): Option[ByteString] = redisReply match {
    case b: Bulk => b.asOptByteString
    case _ => None
  }
}