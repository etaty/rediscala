package redis.actors

import akka.util.ByteStringBuilder
import redis.protocol.{RedisReply, Error}
import java.net.InetSocketAddress
import redis.{Transaction, Operation}
import scala.concurrent.Promise
import java.util.concurrent.LinkedBlockingQueue

class RedisClientActor(addr: InetSocketAddress) extends RedisWorkerIO {
  val queuePromise = new LinkedBlockingQueue[Promise[RedisReply]]()

  def writing: Receive = {
    case Operation(request, promise) =>
      write(request)
      queuePromise put (promise)
    case Transaction(commands) => {
      val buffer = new ByteStringBuilder
      commands.foreach(operation => {
        buffer.append(operation.request)
        queuePromise put (operation.promise)
      })
      write(buffer.result())
    }
  }

  def onReceivedReply(reply: RedisReply) {
    reply match {
      case e: Error => queuePromise.poll().failure(ReplyErrorException(e.toString()))
      case _ => queuePromise.poll().success(reply)
    }
    //queue.dequeue() ! result
  }

  def onConnectionClosed() {
    scala.collection.convert.Wrappers.JIteratorWrapper(queuePromise.iterator()).foreach {
      promise =>
        promise.failure(NoConnectionException)
    }
    queuePromise.clear()
  }

  def address: InetSocketAddress = addr
}

case class ReplyErrorException(message: String) extends Exception(message)

