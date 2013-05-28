package redis.actors

import scala.collection.mutable
import akka.util.ByteStringBuilder
import redis.protocol.{RedisReply, Error}
import java.net.InetSocketAddress
import redis.{Transaction, Operation}
import scala.concurrent.Promise

class RedisClientActor(addr: InetSocketAddress) extends RedisWorkerIO {
  val queuePromise = mutable.Queue[Promise[RedisReply]]()

  def writing: Receive = {
    case Operation(request, promise) =>
      write(request)
      queuePromise enqueue (promise)
    case Transaction(commands) => {
      val buffer = new ByteStringBuilder
      commands.foreach(operation => {
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

case class ReplyErrorException(message: String) extends Exception(message)

