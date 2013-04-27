package redis

import akka.actor.{ActorRef, Stash, Actor}
import akka.event.Logging
import akka.io.Tcp
import akka.io.Tcp._
import java.net.InetSocketAddress
import scala.collection.mutable
import akka.util.{ByteStringBuilder, ByteString}
import scala.annotation.tailrec
import redis.protocol.{RedisProtocolReply, Error}
import java.lang.RuntimeException
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import akka.io.Tcp.Received

class RedisClientActor(address: InetSocketAddress) extends Actor with Stash {

  import context._

  val log = Logging(context.system, this)

  val tcp = akka.io.IO(Tcp)(context.system)

  var tcpWorker: ActorRef = null

  val queue = mutable.Queue[ActorRef]()

  var bufferRead: ByteString = ByteString.empty

  var bufferWrite: ByteStringBuilder = new ByteStringBuilder

  var readyToWrite = true

  override def preStart() {
    tcp ! Connect(address)
  }

  override def postStop() {
    tcp ! Close
  }

  def receive = {
    case Connected(remoteAddr, localAddr) => {
      sender ! Register(self)
      tcpWorker = sender
      become(writing)
      unstashAll()
      log.debug("Connected to " + remoteAddr)
    }
    case c: CommandFailed => log.error(c.toString) // TODO failed connection
    case _ => stash()
  }

  // TODO remove query in timeout before sending them to Redis Server ?
  def writing: Receive = {
    case Received(dataByteString) => {
      bufferRead = decodeReplies(bufferRead ++ dataByteString).compact
    }
    case write: ByteString => {
      if (readyToWrite) {
        tcpWorker ! Write(write, WriteAck)
        readyToWrite = false
      } else {
        bufferWrite.append(write)
      }
      queue enqueue (sender)
    }
    case WriteAck => {
      if (bufferWrite.length == 0) {
        readyToWrite = true
      } else {
        tcpWorker ! Write(bufferWrite.result(), WriteAck)
        bufferWrite.clear()
      }
    }
    case c: ConnectionClosed => println("connection close " + c)
    case c: CommandFailed => log.error("CommandFailed ... " + c) // O/S buffer was full
    case ignored => log.error("ignored : " + ignored.toString)
  }

  @tailrec
  private def decodeReplies(bs: ByteString): ByteString = {
    val r = RedisProtocolReply.decodeReply(bs)
    if (r.nonEmpty) {
      val result = r.get._1 match {
        case e: Error => akka.actor.Status.Failure(RedisReplyErrorException(e.toString()))
        case _ => r.get._1
      }
      queue.dequeue() ! result
      decodeReplies(r.get._2)
    } else {
      bs
    }
  }
}

object WriteAck

case class RedisReplyErrorException(message: String) extends RuntimeException(message)
