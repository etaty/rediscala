package redis.actors

import scala.collection.immutable.Queue
import akka.util.ByteString
import scala.concurrent.Promise
import akka.actor.{Actor, ActorRef}
import scala.collection.mutable
import redis.protocol.{Error, MultiBulk, RedisProtocolRequest}
import akka.event.Logging
import redis.{protocol}
import scala.util.{Success, Failure}

case class TransactionWatchException(message: String = "One watched key has been modified, transaction has failed") extends Exception(message)

case class TransactionInit(queue: Queue[(ByteString, Promise[Any])], watcher: Set[String])

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
