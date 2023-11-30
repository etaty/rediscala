package redis.api.pubsub

import org.apache.pekko.util.ByteString
import redis.protocol.RedisProtocolRequest

case class Message(channel: String, data: ByteString)

case class PMessage(patternMatched: String, channel: String, data: ByteString)

sealed trait SubscribeMessage {
  def cmd: String

  def params: Seq[String]

  def toByteString: ByteString = RedisProtocolRequest.multiBulk(cmd, params.map(ByteString.apply))
}

case class PSUBSCRIBE(pattern: String*) extends SubscribeMessage {
  def cmd = "PSUBSCRIBE"

  def params = pattern
}

case class PUNSUBSCRIBE(pattern: String*) extends SubscribeMessage {
  def cmd = "PUNSUBSCRIBE"

  def params = pattern
}

case class SUBSCRIBE(channel: String*) extends SubscribeMessage {
  def cmd = "SUBSCRIBE"

  def params = channel
}

case class UNSUBSCRIBE(channel: String*) extends SubscribeMessage {
  def cmd = "UNSUBSCRIBE"

  def params = channel
}
