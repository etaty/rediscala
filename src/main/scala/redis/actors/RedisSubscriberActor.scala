package redis.actors

import akka.util.ByteString
import redis.protocol.{MultiBulk, RedisReply}
import redis.api.pubsub._
import java.net.InetSocketAddress
import scala.collection.mutable

class RedisSubscriberActorWithCallback(
                                        override val address: InetSocketAddress,
                                        channels: Seq[String],
                                        patterns: Seq[String],
                                        _onMessage: Message => Unit,
                                        _onPMessage: PMessage => Unit
                                        ) extends RedisSubscriberActor(channels, patterns) {
  def onMessage(m: Message) = _onMessage(m)

  def onPMessage(pm: PMessage) = _onPMessage(pm)
}

abstract class RedisSubscriberActor(
                                     channels: Seq[String],
                                     patterns: Seq[String]
                                     ) extends RedisWorkerIO with DecodeReplies {

  override val address: InetSocketAddress

  def onMessage(m: Message): Unit

  def onPMessage(pm: PMessage): Unit

  /**
   * Keep states of channels and actor in case of connection reset
   */
  val _channels = mutable.MutableList(channels: _*)
  val _patterns = mutable.MutableList(patterns: _*)

  override def preStart() {
    super.preStart()
    write(SUBSCRIBE(_channels: _*).toByteString)
    write(PSUBSCRIBE(_patterns: _*).toByteString)
  }

  def writing: Receive = {
    case message: SubscribeMessage => {
      write(message.toByteString)
      message match {
        case s: SUBSCRIBE => _channels ++= s.channel
        case u: UNSUBSCRIBE => _channels.filter(c => {
          u.channel.exists(_ == c)
        })
        case ps: PSUBSCRIBE => _patterns ++= ps.pattern
        case pu: PUNSUBSCRIBE => _patterns.filter(c => {
          pu.pattern.exists(_ == c)
        })
      }
    }
  }

  def subscribe(channels: String*) {
    self ! SUBSCRIBE(channels: _*)
  }

  def unsubscribe(channels: String*) {
    self ! UNSUBSCRIBE(channels: _*)
  }

  def psubscribe(patterns: String*) {
    self ! PSUBSCRIBE(patterns: _*)
  }

  def punsubscribe(patterns: String*) {
    self ! PUNSUBSCRIBE(patterns: _*)
  }

  def onConnectionClosed() {}

  def onWriteSent() {}

  def onDataReceived(dataByteString: ByteString) {
    decodeReplies(dataByteString)
  }

  def onDecodedReply(reply: RedisReply) {
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