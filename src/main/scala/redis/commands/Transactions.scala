package redis.commands

import redis._
import akka.util.{ByteString, Timeout}
import scala.concurrent.{Promise, Future, ExecutionContext}
import redis.protocol._
import scala.util.{Failure, Success}
import scala.collection.mutable
import akka.actor._
import akka.event.Logging
import redis.Transaction
import redis.ReplyErrorException
import redis.protocol.MultiBulk
import scala.collection.immutable.Queue

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

case class TransactionWatchException(message: String = "One watched key has been modified, transaction has failed") extends Exception(message)

case class TransactionInit(queue: Queue[(ByteString, Promise[Any])], watcher: Set[String]) {

}

class RedisTransactionActor(redisConnection: ActorRef) extends Actor {
  var queue: mutable.Queue[(ByteString, Promise[Any])] = mutable.Queue.empty
  val MULTI = RedisProtocolRequest.inline("MULTI")
  val EXEC = RedisProtocolRequest.inline("EXEC")
  val log = Logging(context.system, this)

  def receive: Receive = {
    case TransactionInit(q, watcher) =>
      queue.enqueue(q: _*)
      val seq = queue.map({
        case (bs, _) => bs
      }).toSeq
      val commands = (watch(watcher) +: MULTI +: seq) ++ Seq(EXEC)
      redisConnection ! Transaction(commands)
    case s: MultiBulk => {
      s.responses.map(_.map(
        r => {
          val reply = r match {
            case e: Error => Failure(ReplyErrorException(e.toString()))
            case _ => Success(r)
          }
          queue.dequeue()._2.complete(reply)
        })
      ).getOrElse({
        queue.foreach(_._2.failure(TransactionWatchException()))
      })
      // Transaction Result received, we can die now
      context.stop(self)
    }
    case s: protocol.Status => //OK or QUEUED
  }

  def watch(watcher: Set[String]): ByteString = {
    if (watcher.nonEmpty)
      RedisProtocolRequest.multiBulk("WATCH", watcher.map(ByteString.apply).toSeq)
    else
      ByteString.empty
  }
}




