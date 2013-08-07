package redis.commands

import redis._
import akka.util.ByteString
import scala.concurrent.{Promise, Future, ExecutionContext}
import akka.actor._
import scala.collection.immutable.Queue
import redis.actors.ReplyErrorException
import redis.protocol._
import redis.Operation
import redis.protocol.MultiBulk
import scala.Some
import scala.util.{Failure, Success}

trait Transactions extends Request {

  def multi(): TransactionBuilder = transaction()

  def multi(operations: (TransactionBuilder) => Unit): TransactionBuilder = {
    val builder = TransactionBuilder(redisConnection)
    operations(builder)
    builder
  }

  def transaction(): TransactionBuilder = TransactionBuilder(redisConnection)

  def watch(watchKeys: String*): TransactionBuilder = {
    val builder = TransactionBuilder(redisConnection)
    builder.watch(watchKeys: _*)
    builder
  }

}

case class TransactionBuilder(redisConnection: ActorRef) extends RedisCommands {
  val operations = Queue.newBuilder[(ByteString, Promise[RedisReply])]
  val watcher = Set.newBuilder[String]

  def unwatch() {
    watcher.clear()
  }

  def watch(keys: String*) {
    watcher ++= keys
  }

  def discard() {
    operations.result().map(operation => {
      operation._2.failure(TransactionDiscardedException)
    })
    operations.clear()
    unwatch()
  }

  // todo maybe return a Future for the general state of the transaction ? (Success or Failure)
  def exec()(implicit ec: ExecutionContext): Future[MultiBulk] = {
    val t = Transaction(watcher.result(), operations.result(), redisConnection)
    val p = Promise[MultiBulk]()
    t.process(p)
    p.future
  }

  /**
   *
   * @param request
   * @return
   */
  override def send(request: ByteString): Future[Any] = {
    val p = Promise[RedisReply]()
    operations += ((request, p))
    p.future
  }
}

case class Transaction(watcher: Set[String], operations: Queue[(ByteString, Promise[RedisReply])], redisConnection: ActorRef) {
  val MULTI = RedisProtocolRequest.inline("MULTI")
  val EXEC = RedisProtocolRequest.inline("EXEC")

  def process(promise: Promise[MultiBulk])(implicit ec: ExecutionContext) {
    val multiOp = Operation(MULTI, ignoredPromise())
    val execOp = Operation(EXEC, execPromise(promise))

    val commands = Seq.newBuilder[Operation]

    val watchOp = watchOperation(watcher)
    watchOp.map(commands.+=(_))
    commands += multiOp
    commands ++= operations.map(op => Operation(op._1, ignoredPromise()))
    commands += execOp

    redisConnection ! redis.Transaction(commands.result())
  }

  def ignoredPromise() = Promise[RedisReply]()

  def execPromise(promise: Promise[MultiBulk])(implicit ec: ExecutionContext): Promise[RedisReply] = {
    val p = Promise[RedisReply]()
    p.future.onComplete(reply => {
      reply match {
        case Success(m: MultiBulk) => {
          promise.success(m)
          dispatchExecReply(m)
        }
        case Success(r) => {
          promise.failure(TransactionExecException(r))
          operations.foreach(_._2.failure(TransactionExecException(r)))
        }
        case Failure(f) => {
          promise.failure(f)
          operations.foreach(_._2.failure(f))
        }
      }
    })
    p
  }

  def dispatchExecReply(multiBulk: MultiBulk) = {
    multiBulk.responses.map(replies => {
      (replies, operations).zipped.map({
        case (reply, operation) => {
          reply match {
            case e: Error => operation._2.failure(ReplyErrorException(e.toString()))
            case _ => operation._2.trySuccess(reply)
          }
        }
      })
    }).getOrElse({
      operations.foreach(_._2.failure(TransactionWatchException()))
    })
  }


  def watchOperation(watcher: Set[String]): Option[Operation] = {
    if (watcher.nonEmpty) {
      val request = RedisProtocolRequest.multiBulk("WATCH", watcher.map(ByteString.apply).toSeq)
      Some(Operation(request, Promise[RedisReply]()))
    } else {
      None
    }
  }
}

case class TransactionExecException(reply: RedisReply) extends Exception(s"Expected MultiBulk response, got : $reply")

case object TransactionDiscardedException extends Exception

case class TransactionWatchException(message: String = "One watched key has been modified, transaction has failed") extends Exception(message)





