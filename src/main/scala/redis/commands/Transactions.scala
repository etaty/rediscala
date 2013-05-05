package redis.commands

import redis._
import akka.util.{ByteString, Timeout}
import scala.concurrent.{Promise, Future, ExecutionContext}
import akka.actor._
import scala.collection.immutable.Queue
import redis.actors.{TransactionInit, RedisTransactionActor}

trait Transactions extends Request {

  def multi()(implicit timeout: Timeout) = transaction()

  def transaction()(implicit timeout: Timeout): TransactionBuilder = TransactionBuilder(redisConnection)

}

// not thread-safe !
case class TransactionBuilder(redisConnection: ActorRef) extends RedisCommands {
  val queueBuilder = Queue.newBuilder[(ByteString, Promise[Any])]
  val watcher = Set.newBuilder[String]

  def unwatch() {
    watcher.clear()
  }

  def watch(keys: String*) {
    watcher ++= keys
  }

  def discard() {
    queueBuilder.result().map {
      case (request, promise) => promise.failure(TransactionDiscardedException)
    }
    queueBuilder.clear()
    unwatch()
  }

  // todo maybe return a Future for the general state of the transaction ? (Success or Failure)
  def exec()(implicit system: ActorSystem, timeout: Timeout, ec: ExecutionContext) {
    val queue = queueBuilder.result()
    if (queue.nonEmpty) {
      val transaction = system.actorOf(Props(classOf[RedisTransactionActor], redisConnection).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))
      transaction ! TransactionInit(queue, watcher.result())
    }
  }

  /**
   *
   * @param request
   * @param timeout has no effect here
   * @return
   */
  def send(request: ByteString)(implicit timeout: Timeout): Future[Any] = {
    val p = Promise[Any]()
    queueBuilder += ((request, p))
    p.future
  }
}

case object TransactionDiscardedException extends Exception




