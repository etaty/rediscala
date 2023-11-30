package redis

import redis.protocol.RedisReply
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.collection.immutable.Queue
import org.apache.pekko.actor.ActorRef
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

  def getNextConnection: Option[ActorRef] = {
    val size = redisConnectionPool.size
    if (size == 0) {
      None
    } else {
      val index = next.getAndIncrement % size
      Some(redisConnectionPool(if (index < 0) size + index - 1 else index))
    }
  }

  protected def send[T](redisConnection: ActorRef, redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    val promise = Promise[T]()
    redisConnection ! Operation(redisCommand, promise)
    promise.future
  }

  def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    getNextConnection.fold(
      Future.failed[T](new RuntimeException("redis pool is empty"))
    ) { redisConnection =>
      send(redisConnection, redisCommand)
    }
  }

}
