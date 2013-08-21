package redis

import scala.concurrent.Promise
import redis.protocol.RedisReply
import akka.util.ByteString

case class Operation[RedisReplyT <: RedisReply,T](redisCommand: RedisCommand[RedisReplyT, T], promise: Promise[T]) {
  def decodeRedisReplyThenComplete(bs: ByteString): Option[(RedisReplyT, ByteString)]= {
    val r = redisCommand.decodeRedisReply.apply(bs)
    completeSuccess(r.get._1)
    r
  }

  def completeSuccess(redisReply: RedisReplyT) = {
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

case class Transaction(commands: Seq[Operation[_,_]])
