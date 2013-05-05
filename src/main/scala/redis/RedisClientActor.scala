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
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import akka.io.Tcp.Received
import akka.actor.Status.Failure

class RedisClientActor extends Actor with Stash {

  import context._

  val log = Logging(context.system, this)

  val tcp = akka.io.IO(Tcp)(context.system)

  var tcpWorker: ActorRef = null

  val queue = mutable.Queue[ActorRef]()

  var bufferRead: ByteString = ByteString.empty

  var bufferWrite: ByteStringBuilder = new ByteStringBuilder

  var readyToWrite = true

  override def postStop() {
    tcp ! Close
  }

  def initConnectedBuffer() {
    bufferRead = ByteString.empty
    bufferWrite.clear()
    readyToWrite = true
  }

  def receive = {
    case address: InetSocketAddress => {
      log.info(s"Connect to $address")
      tcp ! Connect(address)
    }
    case Connected(remoteAddr, localAddr) => {
      initConnectedBuffer()
      sender ! Register(self)
      tcpWorker = sender
      become(writing)
      unstashAll()
      log.info("Connected to " + remoteAddr)
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
    case Transaction(commands) => {
      if (readyToWrite) {
        val b = new ByteStringBuilder
        commands.foreach(b.append)
        tcpWorker ! Write(b.result(), WriteAck)
        readyToWrite = false
      } else {
        commands.foreach(bufferWrite.append)
      }
      commands.foreach(_ => {
        queue enqueue (sender)
      })
    }
    case c: ConnectionClosed =>
      log.info(s"ConnectionClosed $c")
      queue foreach {
        sender =>
          sender ! Failure(NoConnectionException)
      }
      queue.clear()
      become(closed)
    case c: CommandFailed => log.error("CommandFailed ... " + c) // O/S buffer was full
    case ignored => log.error(s"ignored : $ignored")
  }

  // todo transaction + connection closed !
  def closed: Receive = {
    case write: ByteString =>
      log.error("refused  : " + write)
      sender ! Failure(NoConnectionException)
    case address: InetSocketAddress => {
      log.info(s"Connect to $address")
      tcp ! Connect(address)
      become(receive)
    }
    case m => log.error(m.toString)
  }

  @tailrec
  private def decodeReplies(bs: ByteString): ByteString = {
    if (bs.nonEmpty) {
      val r = RedisProtocolReply.decodeReply(bs)
      if (r.nonEmpty) {
        val result = r.get._1 match {
          case e: Error => Failure(ReplyErrorException(e.toString()))
          case _ => r.get._1
        }
        queue.dequeue() ! result
        decodeReplies(r.get._2)
      } else {
        bs
      }
    } else {
      bs
    }
  }
}

case class Transaction(commands: Seq[ByteString])

object WriteAck

case class ReplyErrorException(message: String) extends Exception(message)

object NoConnectionException extends Exception
