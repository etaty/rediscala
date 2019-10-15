package redis.api.bsortedsets

import scala.concurrent.duration.{Duration, FiniteDuration}
import redis._
import akka.util.ByteString
import redis.protocol.MultiBulk

case class Bzpopmax[KK: ByteStringSerializer, R: ByteStringDeserializer](keys: Seq[KK], timeout: FiniteDuration = Duration.Zero)
  extends Bzpopx[KK, R]("BZPOPMAX")

case class Bzpopmin[KK: ByteStringSerializer, R: ByteStringDeserializer](keys: Seq[KK], timeout: FiniteDuration = Duration.Zero)
  extends Bzpopx[KK, R]("BZPOPMIN")

private[redis] abstract class Bzpopx[KK, R](command: String)(implicit redisKeys: ByteStringSerializer[KK], deserializerR: ByteStringDeserializer[R])
  extends RedisCommandMultiBulk[Option[(String, R, Double)]] {
  val isMasterOnly = true
  val keys: Seq[KK]
  val timeout: FiniteDuration

  val encodedRequest: ByteString = encode(command, keys.map(redisKeys.serialize) ++ Seq(ByteString(timeout.toSeconds.toString)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toOptionStringByteStringDouble(mb)
}
