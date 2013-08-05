package redis.actors

import akka.util.ByteString
import redis.protocol.{MultiBulk, RedisReply}
import redis.api.pubsub._

abstract class Subscriber extends RedisWorkerIO with DecodeReplies {

  /** Return the list of Channels which are going to be connected during initialization of the actor
    *
    * @return sequence of channels
    */
  def subscribedChannels: Seq[String]

  /** Return the list of Patterns which are going to be connected during initialization of the actor
    *
    * @return sequence of patterns
    */
  def subscribedPatterns: Seq[String]

  /** Called when a Message is received
    *
    * @param message
    */
  def onMessage(message: Message)

  /** Called when a PMessage is received
    *
    * @param pmessage
    */
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