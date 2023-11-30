package redis

import scala.concurrent.Promise
import redis.protocol.{DecodeResult, RedisReply}
//import akka.util.ByteString
import org.apache.pekko.util.ByteString

case class Operation[RedisReplyT <: RedisReply, T](redisCommand: RedisCommand[RedisReplyT, T], promise: Promise[T]) {
  def decodeRedisReplyThenComplete(bs: ByteString): DecodeResult[Unit] = {
    val r = redisCommand.decodeRedisReply.apply(bs)
    r.foreach { reply =>
      completeSuccess(reply)
    }
  }

  def completeSuccess(redisReply: RedisReplyT): Promise[T] = {
    val v = redisCommand.decodeReply(redisReply)
    promise.success(v)
  }

  def tryCompleteSuccess(redisReply: RedisReply) = {
    val v = redisCommand.decodeReply(redisReply.asInstanceOf[RedisReplyT])
    promise.trySuccess(v)
  }

  def completeSuccessValue(value: T) = promise.success(value)

  def completeFailed(t: Throwable) = promise.failure(t)
}

case class Transaction(commands: Seq[Operation[_, _]])
