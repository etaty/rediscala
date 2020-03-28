package redis

import scala.concurrent.Promise
import redis.protocol.{DecodeResult, RedisReply}
import akka.util.ByteString

import scala.util.Try

case class Operation[RedisReplyT <: RedisReply, T](redisCommand: RedisCommand[RedisReplyT, T], promise: Promise[T]) {
  def decodeRedisReplyThenComplete(bs: ByteString): DecodeResult[Unit] = {
    val r = redisCommand.decodeRedisReply.apply(bs)
    r.foreach { reply =>
      completeSuccess(reply)
    }
  }

  def completeSuccess(redisReply: RedisReplyT): Promise[T] = {
    val v = Try(redisCommand.decodeReply(redisReply))
    promise.complete(v)
  }

  def tryCompleteSuccess(redisReply: RedisReply) = {
    val v = Try(redisCommand.decodeReply(redisReply.asInstanceOf[RedisReplyT]))
    promise.tryComplete(v)
  }

  def completeSuccessValue(value: T) = promise.success(value)

  def completeFailed(t: Throwable) = promise.failure(t)
}

case class Transaction(commands: Seq[Operation[_, _]])
