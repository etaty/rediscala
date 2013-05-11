package redis.actors

import akka.actor.ActorRef
import scala.collection.mutable
import akka.util.{ByteStringBuilder, ByteString}
import redis.protocol.{RedisReply, Error}
import akka.actor.Status.Failure
import java.net.InetSocketAddress

class RedisClientActor(addr: InetSocketAddress) extends RedisWorkerIO {
  val queue = mutable.Queue[ActorRef]()

  def writing: Receive = {
    case bs: ByteString => {
      write(bs)
      queue enqueue (sender)
    }
    case Transaction(commands) => {
      val buffer = new ByteStringBuilder
      commands.foreach(buffer.append)
      write(buffer.result())
      commands.foreach(_ => {
        queue enqueue (sender)
      })
    }
  }

  def onReceivedReply(reply: RedisReply) {
    val result = reply match {
      case e: Error => Failure(ReplyErrorException(e.toString()))
      case _ => reply
    }
    queue.dequeue() ! result
  }

  def onConnectionClosed() {
    queue foreach {
      sender =>
        sender ! Failure(NoConnectionException)
    }
    queue.clear()
  }

  def address: InetSocketAddress = addr
}

case class Transaction(commands: Seq[ByteString])

case class ReplyErrorException(message: String) extends Exception(message)

