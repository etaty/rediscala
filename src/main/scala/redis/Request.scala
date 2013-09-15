package redis

import redis.protocol.RedisReply
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.collection.immutable.Queue
import akka.actor.ActorRef
import java.util.concurrent.atomic.AtomicInteger


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

trait BufferedRequest {
  implicit val executionContext: ExecutionContext

  val operations = Queue.newBuilder[Operation[_, _]]

  def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    val promise = Promise[T]()
    operations += Operation(redisCommand, promise)
    promise.future
  }
}

trait RoundRobinPoolRequest {
  implicit val executionContext: ExecutionContext

  def redisConnectionPool: Seq[ActorRef]

  val next = new AtomicInteger(0)

  def getNextConnection : ActorRef = {
    redisConnectionPool(next.getAndIncrement % redisConnectionPool.size)
  }

  private def send[T](redisConnection: ActorRef, redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    val promise = Promise[T]()
    redisConnection ! Operation(redisCommand, promise)
    promise.future
  }

  def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    // TODO empty Pool ?
    val redisConnection = getNextConnection
    send(redisConnection, redisCommand)
  }

  /**
   *
   * @param redisCommand
   * @tparam T
   * @return behave nicely with Future helpers like firstCompletedOf or sequence
   */
  def broadcast[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Seq[Future[T]] = {
    redisConnectionPool.map(redisConnection => {
      send(redisConnection, redisCommand)
    })
  }
}