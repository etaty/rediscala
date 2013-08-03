package redis.actors

import akka.util.{ByteString, ByteStringBuilder}
import redis.protocol.RedisReply
import java.net.InetSocketAddress
import redis.{Transaction, Operation}
import scala.concurrent.Promise
import akka.actor.{OneForOneStrategy, Terminated, PoisonPill, Props}
import scala.collection.mutable
import akka.actor.SupervisorStrategy.Stop

class RedisClientActor(override val address: InetSocketAddress) extends RedisWorkerIO {

  import context._

  var repliesDecoder = initRepliesDecoder

  def initRepliesDecoder = context.actorOf(Props(classOf[RedisReplyDecoder]).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

  var queuePromises = mutable.Queue[Promise[RedisReply]]()

  def writing: Receive = {
    case Operation(request, promise) =>
      queuePromises enqueue promise
      write(request)
    case Transaction(commands) => {
      val buffer = new ByteStringBuilder
      commands.foreach(operation => {
        buffer.append(operation.request)
        queuePromises enqueue operation.promise
      })
      write(buffer.result())
    }
    case Terminated(actorRef) =>
      println(s"Terminated($actorRef)")
  }

  def onDataReceived(dataByteString: ByteString) {
    repliesDecoder ! dataByteString
  }

  def onWriteSent() {
    repliesDecoder ! queuePromises
    queuePromises = mutable.Queue[Promise[RedisReply]]()
  }

  def onConnectionClosed() {
    queuePromises.foreach(promise => {
      promise.failure(NoConnectionException)
    })
    queuePromises.clear()
    repliesDecoder ! PoisonPill
    repliesDecoder = initRepliesDecoder
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: Exception => {
        // Start a new decoder
        repliesDecoder = initRepliesDecoder
        restartConnection()
        // stop the old one => clean the mailbox
        Stop
      }
    }
}

object NoConnectionException extends RuntimeException("No Connection established")
