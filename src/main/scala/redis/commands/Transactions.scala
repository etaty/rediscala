package redis.commands

import redis._
import scala.concurrent.{Promise, Future, ExecutionContext}
import akka.actor._
import scala.collection.immutable.Queue
import redis.actors.ReplyErrorException
import redis.protocol._
import redis.protocol.MultiBulk
import scala.Some
import scala.util.{Failure, Success}
import redis.api.transactions.{Watch, Exec, Multi}
import akka.util.ByteString

trait Transactions extends ActorRequest {

  def multi(): TransactionBuilder = transaction()

  def multi(operations: (TransactionBuilder) => Unit): TransactionBuilder = {
    val builder = transaction()
    operations(builder)
    builder
  }

  def transaction(): TransactionBuilder = TransactionBuilder(redisConnection)

  def watch(watchKeys: String*): TransactionBuilder = {
    val builder = transaction()
    builder.watch(watchKeys: _*)
    builder
  }

}

case class TransactionBuilder(redisConnection: ActorRef)(implicit val executionContext: ExecutionContext) extends BufferedRequest with RedisCommands {

  //val operations = Queue.newBuilder[Operation[_, _]]
  val watcher = Set.newBuilder[String]

  def unwatch(): Unit = {
    watcher.clear()
  }

  def watch(keys: String*): Unit = {
    watcher ++= keys
  }

  def discard(): Unit = {
    operations.result().map(operation => {
      operation.completeFailed(TransactionDiscardedException)
    })
    operations.clear()
    unwatch()
  }

  // todo maybe return a Future for the general state of the transaction ? (Success or Failure)
  def exec(): Future[MultiBulk] = {
    val t = Transaction(watcher.result(), operations.result(), redisConnection)
    val p = Promise[MultiBulk]()
    t.process(p)
    p.future
  }
}

case class Transaction(watcher: Set[String], operations: Queue[Operation[_, _]], redisConnection: ActorRef)(implicit val executionContext: ExecutionContext) {

  def process(promise: Promise[MultiBulk]): Unit = {
    val multiOp = Operation(Multi, Promise[Boolean]())
    val execOp = Operation(Exec, execPromise(promise))

    val commands = Seq.newBuilder[Operation[_, _]]

    val watchOp = watchOperation(watcher)
    watchOp.map(commands.+=(_))
    commands += multiOp
    commands ++= operations.map(op => operationToQueuedOperation(op))
    commands += execOp

    redisConnection ! redis.Transaction(commands.result())
  }

  def operationToQueuedOperation(op: Operation[_, _]) = {
    val cmd = new RedisCommandStatusString {
      val isMasterOnly = true
      val encodedRequest: ByteString = op.redisCommand.encodedRequest
    }
    Operation(cmd, Promise[String]())
  }

  def ignoredPromise() = Promise[Any]()

  def execPromise(promise: Promise[MultiBulk]): Promise[MultiBulk] = {
    val p = Promise[MultiBulk]()
    p.future.onComplete(reply => {
      reply match {
        case Success(m: MultiBulk) => {
          promise.success(m)
          dispatchExecReply(m)
        }
        case Success(r) => {
          promise.failure(TransactionExecException(r))
          operations.foreach(_.completeFailed(TransactionExecException(r)))
        }
        case Failure(f) => {
          promise.failure(f)
          operations.foreach(_.completeFailed(f))
        }
      }
    })
    p
  }

  def dispatchExecReply(multiBulk: MultiBulk) = {
    multiBulk.responses.map(replies => {
      (replies, operations).zipped.map((reply, operation) => {
        reply match {
          case e: Error => operation.completeFailed(ReplyErrorException(e.toString()))
          case _ => operation.tryCompleteSuccess(reply)
        }
      })
    }).getOrElse({
      operations.foreach(_.completeFailed(TransactionWatchException()))
    })
  }


  def watchOperation(keys: Set[String]): Option[Operation[_, Boolean]] = {
    if (keys.nonEmpty) {
      Some(Operation(Watch(keys), Promise[Boolean]()))
    } else {
      None
    }
  }
}

case class TransactionExecException(reply: RedisReply) extends Exception(s"Expected MultiBulk response, got : $reply")

case object TransactionDiscardedException extends Exception

case class TransactionWatchException(message: String = "One watched key has been modified, transaction has failed") extends Exception(message)





