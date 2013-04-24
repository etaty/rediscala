package redis

import akka.actor.{ActorRef, Stash, Actor}
import akka.event.Logging
import akka.io.Tcp
import akka.io.Tcp._
import java.net.{InetAddress, InetSocketAddress}
import scala.collection.mutable
import akka.util.{ByteStringBuilder, ByteString}
import scala.annotation.tailrec
import redis.protocol.RedisProtocolReply
import java.lang.RuntimeException
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import redis.protocol.Error
import akka.io.Tcp.Received

class RedisClientActor(host: String, port: Int) extends Actor with Stash {

  import context._

  val log = Logging(context.system, this)

  val tcp = akka.io.IO(Tcp)(context.system)

  val remote = new InetSocketAddress(InetAddress.getByName(host), port)

  var tcpWorker: ActorRef = null
  tcp ! Connect(remote)

  val queue = mutable.Queue[ActorRef]()

  var bufferRead: ByteString = ByteString.empty

  var bufferWrite: ByteStringBuilder = new ByteStringBuilder

  var readyToWrite = true

  def receive = {
    case Connected(remoteAddr, localAddr) => {
      println("connected")
      sender ! Register(self)
      tcpWorker = sender
      become(writing)
      unstashAll()
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
        tcpWorker ! Write(write, true)
        readyToWrite = false
      } else {
        bufferWrite.append(write)
      }
      queue enqueue (sender)
    }
    case true => {
      if (bufferWrite.length == 0) {
        readyToWrite = true
      } else {
        tcpWorker ! Write(bufferWrite.result(), true)
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
        case Error(error) => akka.actor.Status.Failure(new RuntimeException(error.toString())) // TODO runtime ???
        case _ => r.get._1
      }
      queue.dequeue() ! result
      decodeReplies(r.get._2)
    } else {
      bs
    }
  }

}
