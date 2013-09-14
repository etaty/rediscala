package redis.actors

import akka.util.ByteString
import redis.protocol.{MultiBulk, RedisReply}
import redis.api.pubsub._
import java.net.InetSocketAddress
import scala.collection.mutable
import redis.api.connection.Auth

class RedisSubscriberActorWithCallback(
                                        address: InetSocketAddress,
                                        channels: Seq[String],
                                        patterns: Seq[String],
                                        messageCallback: Message => Unit,
                                        pmessageCallback: PMessage => Unit,
                                        authPassword: Option[String] = None
                                        ) extends RedisSubscriberActor(address, channels, patterns, authPassword) {
  def onMessage(m: Message) = messageCallback(m)

  def onPMessage(pm: PMessage) = pmessageCallback(pm)
}

abstract class RedisSubscriberActor(
                                     address: InetSocketAddress,
                                     channels: Seq[String],
                                     patterns: Seq[String],
                                     authPassword: Option[String] = None
                                     ) extends RedisWorkerIO(address) with DecodeReplies {
  def onConnectWrite(): ByteString = {
    authPassword.map(Auth(_).encodedRequest).getOrElse(ByteString.empty)
  }

  def onMessage(m: Message): Unit

  def onPMessage(pm: PMessage): Unit

  /**
   * Keep states of channels and actor in case of connection reset
   */
  val channelsSubscribed = mutable.MutableList(channels: _*)
  val patternsSubscribed = mutable.MutableList(patterns: _*)

  override def preStart() {
    super.preStart()
    write(SUBSCRIBE(channelsSubscribed: _*).toByteString)
    write(PSUBSCRIBE(patternsSubscribed: _*).toByteString)
  }

  def writing: Receive = {
    case message: SubscribeMessage => {
      write(message.toByteString)
      message match {
        case s: SUBSCRIBE => channelsSubscribed ++= s.channel
        case u: UNSUBSCRIBE => channelsSubscribed.filter(c => {
          u.channel.exists(_ == c)
        })
        case ps: PSUBSCRIBE => patternsSubscribed ++= ps.pattern
        case pu: PUNSUBSCRIBE => patternsSubscribed.filter(c => {
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