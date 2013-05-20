package redis.actors

import akka.actor.ActorRef
import scala.collection.mutable
import akka.util.{ByteStringBuilder, ByteString}
import redis.protocol.{RedisReply, Error}
import akka.actor.Status.Failure
import java.net.InetSocketAddress
import redis.Operation
import scala.concurrent.Promise

class RedisClientActor(addr: InetSocketAddress) extends RedisWorkerIO {
  val queuePromise = mutable.Queue[Promise[RedisReply]]()

  def writing: Receive = {
    case Operation(request, promise) =>
      write(request)
      queuePromise enqueue (promise)
    case transactions: Seq[Operation] => {
      val buffer = new ByteStringBuilder
      transactions.foreach(operation => {
        buffer.append(operation.request)
        queuePromise enqueue (operation.promise)
      })
      write(buffer.result())
    }
  }

  def onReceivedReply(reply: RedisReply) {
    reply match {
      case e: Error => queuePromise.dequeue().failure(ReplyErrorException(e.toString()))
      case _ => queuePromise.dequeue().success(reply)
    }
    //queue.dequeue() ! result
  }

  def onConnectionClosed() {
    queuePromise foreach {
      promise =>
        promise.failure(NoConnectionException)
    }
    queuePromise.clear()
  }

  def address: InetSocketAddress = addr
}

case class Transaction(commands: Seq[ByteString])

case class ReplyErrorException(message: String) extends Exception(message)

