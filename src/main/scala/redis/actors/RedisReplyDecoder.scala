package redis.actors

import akka.actor.Actor
import scala.collection.mutable
import redis.protocol.{RedisProtocolReply, RedisReply}
import akka.util.ByteString
import akka.event.Logging
import scala.annotation.tailrec
import redis.Operation

class RedisReplyDecoder() extends Actor {


  val queuePromises = mutable.Queue[Operation[_,_]]()

  val log = Logging(context.system, this)

  override def postStop() {
    queuePromises.foreach(op => {
      op.completeFailed(InvalidRedisReply)
    })
  }

  def receive = {
    case promises: mutable.Queue[Operation[_,_]] => {
      queuePromises ++= promises
    }
    case byteStringInput: ByteString => decodeReplies(byteStringInput)
  }

  var bufferRead: ByteString = ByteString.empty

  def decodeReplies(dataByteString: ByteString) {
    bufferRead = decodeRepliesRecur(bufferRead ++ dataByteString).compact
  }

  @tailrec
  private def decodeRepliesRecur(bs: ByteString): ByteString = {
    val op = queuePromises.front

    val result : Option[(Any, ByteString)] = decodeRedisReply(op, bs)

    if(result.nonEmpty) {
      val tail = result.get._2
      if (queuePromises.nonEmpty && tail.nonEmpty)
        decodeRepliesRecur(tail)
      else
        tail
    } else {
      bs
    }
  }

  def decodeRedisReply(operation: Operation[_,_], bs: ByteString): Option[(Any, ByteString)] = {
    if (operation.redisCommand.decodeRedisReply.isDefinedAt(bs)) {
      val r = operation.decodeRedisReplyThenComplete(bs)
      if (r.nonEmpty) {
        queuePromises.dequeue()
      }
      r
    } else if (RedisProtocolReply.decodeReplyError.isDefinedAt(bs)) {
      val r = RedisProtocolReply.decodeReplyError.apply(bs)
      if (r.nonEmpty) {
        operation.completeFailed(ReplyErrorException(r.get._1.toString))
        queuePromises.dequeue()
      }
      r
    } else {
      throw new Exception(s"Redis Protocol error: Got ${bs.head} as initial reply byte for Operation: $operation")
    }
  }
}

case class ReplyErrorException(message: String) extends Exception(message)

object InvalidRedisReply extends RuntimeException("Could not decode the redis reply (Connection closed)")

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
