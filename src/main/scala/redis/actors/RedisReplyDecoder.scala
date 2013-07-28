package redis.actors

import akka.actor.Actor
import scala.collection.mutable
import scala.concurrent.Promise
import redis.protocol.{Error, RedisProtocolReply, RedisReply}
import akka.util.ByteString
import akka.event.Logging
import scala.annotation.tailrec

class RedisReplyDecoder() extends Actor with DecodeReplies {

  import context._

  val queuePromises = mutable.Queue[Promise[RedisReply]]()

  val log = Logging(context.system, this)

  def receive = {
    case promises: mutable.Queue[Promise[RedisReply]] => {
      queuePromises ++= promises
    }
    case byteStringInput: ByteString => decodeReplies(byteStringInput)
  }

  def onDecodedReply(reply: RedisReply) {
    reply match {
      case e: Error => queuePromises.dequeue().failure(ReplyErrorException(e.toString()))
      case _ => queuePromises.dequeue().success(reply)
    }
  }

}

case class ReplyErrorException(message: String) extends Exception(message)

trait DecodeReplies {
  var bufferRead: ByteString = ByteString.empty

  def decodeReplies(dataByteString: ByteString) {
    bufferRead = decodeRepliesRecur(bufferRead ++ dataByteString).compact
  }

  @tailrec
  private def decodeRepliesRecur(bs: ByteString): ByteString = {
    val r = RedisProtocolReply.decodeReply(bs)
    if (r.nonEmpty) {
      onDecodedReply(r.get._1)
      decodeRepliesRecur(r.get._2)
    } else {
      bs
    }
  }

  def onDecodedReply(reply: RedisReply)
}
