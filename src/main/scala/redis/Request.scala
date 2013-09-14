package redis

import redis.protocol.RedisReply
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.collection.immutable.Queue
import akka.actor.ActorRef


trait Request {
  implicit val executionContext: ExecutionContext

  def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T]
}

trait ActorRequest {
  implicit val executionContext: ExecutionContext

  def redisConnection: ActorRef

  def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    val promise = Promise[T]()
    redisConnection ! Operation(redisCommand, promise)
    promise.future
  }
}

trait BufferedRequest extends redis.Request {
  implicit val executionContext: ExecutionContext

  val operations = Queue.newBuilder[Operation[_, _]]

  override def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    val promise = Promise[T]()
    operations += Operation(redisCommand, promise)
    promise.future
  }
}
