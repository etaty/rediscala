package redis.api.blists

import scala.concurrent.duration.{Duration, FiniteDuration}
import redis.{ByteStringSerializer, RedisCommandRedisReply, RedisCommandMultiBulk, MultiBulkConverter}
import akka.util.ByteString
import redis.protocol.{RedisReply, MultiBulk, Bulk}

case class Blpop[KK: ByteStringSerializer](keys: Seq[KK], timeout: FiniteDuration = Duration.Zero)
  extends BXpop[KK]("BLPOP")


case class Brpop[KK: ByteStringSerializer](keys: Seq[KK], timeout: FiniteDuration = Duration.Zero)
  extends BXpop[KK]("BRPOP")


private[redis] abstract class BXpop[KK](command: String)(implicit redisKeys: ByteStringSerializer[KK])
  extends RedisCommandMultiBulk[Option[(String, ByteString)]] {
  val keys: Seq[KK]
  val timeout: FiniteDuration

  val encodedRequest: ByteString = encode(command, keys.map(redisKeys.serialize).seq ++ Seq(ByteString(timeout.toSeconds.toString)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toOptionStringByteString(mb)
}

case class Brpopplush[KS, KD](source: KS, destination: KD, timeout: FiniteDuration = Duration.Zero)
                             (implicit bsSource: ByteStringSerializer[KS], bsDest: ByteStringSerializer[KD])
  extends RedisCommandRedisReply[Option[ByteString]] {
  val encodedRequest: ByteString = encode("BRPOPLPUSH",
    Seq(bsSource.serialize(source), bsDest.serialize(destination), ByteString(timeout.toSeconds.toString)))

  def decodeReply(redisReply: RedisReply): Option[ByteString] = redisReply match {
    case b: Bulk => b.asOptByteString
    case _ => None
  }
}