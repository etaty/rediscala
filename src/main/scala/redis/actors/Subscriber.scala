package redis.actors

import akka.util.ByteString
import redis.protocol.{MultiBulk, RedisReply, RedisProtocolRequest}

abstract class Subscriber extends RedisWorkerIO {
  def subscribedChannels: Seq[String]

  def subscribedPatterns: Seq[String]

  def onMessage(message: Message)

  def onPMessage(pmessage: PMessage)

  override def preStart() {
    super.preStart()
    subscribedChannels.foreach(channel => {
      self ! SUBSCRIBE(channel)
    })
    subscribedPatterns.foreach(pattern => {
      self ! PSUBSCRIBE(pattern)
    })
  }

  def writing: Receive = {
    case message: SubscribeMessage => write(message.toByteString)
  }

  def onConnectionClosed() {}

  def onReceivedReply(reply: RedisReply) {
    reply match {
      case MultiBulk(Some(list)) if list.length == 3 && list.head.toByteString.utf8String == "message" => {
        onMessage(Message(list(1).toByteString.utf8String, list(2).toByteString.utf8String))
      }
      case MultiBulk(Some(list)) if list.length == 4 && list.head.toByteString.utf8String == "pmessage" => {
        onPMessage(PMessage(list(1).toByteString.utf8String, list(2).toByteString.utf8String, list(3).toByteString.utf8String))
      }
      case _ => // subscribe or psubscribe
    }
  }
}

case class Message(channel: String, data: String)

case class PMessage(patternMatched: String, channel: String, data: String)

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