package redis.actors

import akka.util.{ByteString, ByteStringBuilder}
import java.net.InetSocketAddress
import redis.{Redis, Operation, Transaction}
import akka.actor._
import scala.collection.mutable
import akka.actor.SupervisorStrategy.Stop

class RedisClientActor(override val address: InetSocketAddress, getConnectOperations: () => Seq[Operation[_, _]], onConnectStatus: Boolean => Unit  ) extends RedisWorkerIO(address,onConnectStatus) {


  import context._

  var repliesDecoder = initRepliesDecoder()

  // connection closed on the sending direction
  var oldRepliesDecoder: Option[ActorRef] = None

  def initRepliesDecoder() = context.actorOf(Props(classOf[RedisReplyDecoder]).withDispatcher(Redis.dispatcher))

  var queuePromises = mutable.Queue[Operation[_, _]]()

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
    case KillOldRepliesDecoder => killOldRepliesDecoder()
  }

  def onDataReceived(dataByteString: ByteString) {
    repliesDecoder ! dataByteString
  }

  def onDataReceivedOnClosingConnection(dataByteString: ByteString) {
    oldRepliesDecoder.foreach(oldRepliesDecoder => oldRepliesDecoder ! dataByteString)
  }

  def onWriteSent() {
    repliesDecoder ! QueuePromises(queuePromises)
    queuePromises = mutable.Queue[Operation[_, _]]()
  }

  def onConnectionClosed() {
    queuePromises.foreach(op => {
      op.completeFailed(NoConnectionException)
    })
    queuePromises.clear()
    killOldRepliesDecoder()
    oldRepliesDecoder = Some(repliesDecoder)
    // TODO send delayed message to oldRepliesDecoder to kill himself after X seconds
    this.context.system.scheduler.scheduleOnce(reconnectDuration * 10, self, KillOldRepliesDecoder)
    repliesDecoder = initRepliesDecoder()
  }

  def onClosingConnectionClosed(): Unit = killOldRepliesDecoder()

  def killOldRepliesDecoder() = {
    oldRepliesDecoder.foreach(oldRepliesDecoder => oldRepliesDecoder ! PoisonPill)
    oldRepliesDecoder = None
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: Exception => {
        // Start a new decoder
        repliesDecoder = initRepliesDecoder()
        restartConnection()
        // stop the old one => clean the mailbox
        Stop
      }
    }

  def onConnectWrite(): ByteString = {
    val ops = getConnectOperations()
    val buffer = new ByteStringBuilder

    val queuePromisesConnect = mutable.Queue[Operation[_, _]]()
    ops.foreach(operation => {
      buffer.append(operation.redisCommand.encodedRequest)
      queuePromisesConnect enqueue operation
    })
    queuePromises = queuePromisesConnect ++ queuePromises
    buffer.result()
  }

}

case object NoConnectionException extends RuntimeException("No Connection established")

case object KillOldRepliesDecoder
