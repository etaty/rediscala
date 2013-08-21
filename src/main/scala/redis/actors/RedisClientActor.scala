package redis.actors

import akka.util.{ByteString, ByteStringBuilder}
import java.net.InetSocketAddress
import redis.{Operation, Transaction}
import akka.actor.{OneForOneStrategy, Terminated, PoisonPill, Props}
import scala.collection.mutable
import akka.actor.SupervisorStrategy.Stop

class RedisClientActor(override val address: InetSocketAddress) extends RedisWorkerIO {


  var repliesDecoder = initRepliesDecoder

  def initRepliesDecoder = context.actorOf(Props(classOf[RedisReplyDecoder]).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

  var queuePromises = mutable.Queue[Operation[_,_]]()

  def writing: Receive = {
    case op : Operation[_,_] =>
      queuePromises enqueue op
      write(op.redisCommand.encodedRequest)
    case Transaction(commands) => {
      val buffer = new ByteStringBuilder
      commands.foreach(operation => {
        buffer.append(operation.redisCommand.encodedRequest)
        queuePromises enqueue operation
      })
      write(buffer.result())
    }
    case Terminated(actorRef) =>
      log.warning(s"Terminated($actorRef)")
  }

  def onDataReceived(dataByteString: ByteString) {
    repliesDecoder ! dataByteString
  }

  def onWriteSent() {
    repliesDecoder ! queuePromises
    queuePromises = mutable.Queue[Operation[_,_]]()
  }

  def onConnectionClosed() {
    queuePromises.foreach(op => {
      op.completeFailed(NoConnectionException)
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

case object NoConnectionException extends RuntimeException("No Connection established")
