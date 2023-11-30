package redis.api.blists

import scala.concurrent.duration.{Duration, FiniteDuration}
import redis._
import org.apache.pekko.util.ByteString
import redis.protocol.{RedisReply, MultiBulk, Bulk}

case class Blpop[KK: ByteStringSerializer, R: ByteStringDeserializer](keys: Seq[KK], timeout: FiniteDuration = Duration.Zero)
  extends BXpop[KK, R]("BLPOP")


case class Brpop[KK: ByteStringSerializer, R: ByteStringDeserializer](keys: Seq[KK], timeout: FiniteDuration = Duration.Zero)
  extends BXpop[KK, R]("BRPOP")


private[redis] abstract class BXpop[KK, R](command: String)(implicit redisKeys: ByteStringSerializer[KK], deserializerR: ByteStringDeserializer[R])
  extends RedisCommandMultiBulk[Option[(String, R)]] {
  val isMasterOnly = true
  val keys: Seq[KK]
  val timeout: FiniteDuration

  val encodedRequest: ByteString = encode(command, keys.map(redisKeys.serialize) ++ Seq(ByteString(timeout.toSeconds.toString)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toOptionStringByteString(mb)
}

case class Brpoplpush[KS, KD, R](source: KS, destination: KD, timeout: FiniteDuration = Duration.Zero)
                             (implicit bsSource: ByteStringSerializer[KS], bsDest: ByteStringSerializer[KD], deserializerR: ByteStringDeserializer[R])
  extends RedisCommandRedisReply[Option[R]] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("BRPOPLPUSH",
    Seq(bsSource.serialize(source), bsDest.serialize(destination), ByteString(timeout.toSeconds.toString)))

  def decodeReply(redisReply: RedisReply): Option[R] = redisReply match {
    case b: Bulk => b.asOptByteString.map(deserializerR.deserialize)
    case _ => None
  }
}