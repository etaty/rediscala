package redis

import scala.concurrent.Promise
import redis.protocol.RedisReply

case class Operation[T](redisCommand: RedisCommand[_, T], promise: Promise[T]) {
  def completeSuccess(redisReply: RedisReply) = {
    val v = redisCommand.decodeRedisReply(redisReply)
    promise.success(v)
  }

  def tryCompleteSuccess(redisReply: RedisReply) = {
    val v = redisCommand.decodeRedisReply(redisReply)
    promise.trySuccess(v)
  }

  def completeSuccessValue(value: T) = promise.success(value)

  def completeFailed(t: Throwable) = promise.failure(t)
}

case class Transaction(commands: Seq[Operation[_]])
