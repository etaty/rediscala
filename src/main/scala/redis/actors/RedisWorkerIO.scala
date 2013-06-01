package redis.actors

import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import akka.io.Tcp
import akka.util.{ByteStringBuilder, ByteString}
import java.net.InetSocketAddress
import akka.io.Tcp._
import redis.protocol.{RedisProtocolReply, RedisReply}
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import akka.io.Tcp.Received
import scala.annotation.tailrec

trait RedisWorkerIO extends Actor {

  def address: InetSocketAddress

  import context._

  val log = Logging(context.system, this)

  val tcp = akka.io.IO(Tcp)(context.system)

  // todo watch tcpWorker
  var tcpWorker: ActorRef = null

  var bufferRead: ByteString = ByteString.empty

  val bufferWrite: ByteStringBuilder = new ByteStringBuilder

  var readyToWrite = false

  override def preStart() {
    log.info(s"Connect to $address")
    tcp ! Connect(address)
  }

  override def postStop() {
    log.info("RedisWorkerIO stop")
    if (tcpWorker != null) {
      tcpWorker ! Close
    }
  }

  def initConnectedBuffer() {
    bufferRead = ByteString.empty
    readyToWrite = true
  }

  def receive = connecting orElse writing

  def connecting: Receive = {
    case Connected(remoteAddr, localAddr) => {
      sender ! Register(self)
      tcpWorker = sender
      initConnectedBuffer()
      tryWrite()
      become(connected)
      log.info("Connected to " + remoteAddr)
    }
    case Reconnect => preStart()
    case c: CommandFailed => {
      log.error(c.toString)
      cleanState()
      scheduleReconnect()
    }
  }

  def connected: Receive = writing orElse reading

  def reading: Receive = {
    case WriteAck => tryWrite()
    case Received(dataByteString) => {
      bufferRead = decodeReplies(bufferRead ++ dataByteString).compact
    }
    case c: ConnectionClosed =>
      log.warning(s"ConnectionClosed $c")
      cleanState()
      scheduleReconnect()
    case c: CommandFailed =>
      log.error("CommandFailed ... " + c) // O/S buffer was full
      cleanState()
      scheduleReconnect()
  }

  def scheduleReconnect() {
    log.info(s"Trying to reconnect in $reconnectDuration")
    this.context.system.scheduler.scheduleOnce(reconnectDuration, self, Reconnect)
    become(receive)
  }

  def cleanState() {
    onConnectionClosed()
    readyToWrite = false
  }

  def writing: Receive

  def onConnectionClosed()

  def onReceivedReply(reply: RedisReply)

  def tryWrite() {
    if (bufferWrite.length == 0) {
      readyToWrite = true
    } else {
      writeWorker(bufferWrite.result())
      bufferWrite.clear()
    }
  }

  def write(byteString: ByteString) {
    if (readyToWrite) {
      writeWorker(byteString)
      readyToWrite = false
    } else {
      bufferWrite.append(byteString)
    }
  }

  import scala.concurrent.duration.{DurationInt, FiniteDuration}

  def reconnectDuration: FiniteDuration = 2 seconds

  private def writeWorker(byteString: ByteString) {
    tcpWorker ! Write(byteString, WriteAck)
  }

  @tailrec
  private def decodeReplies(bs: ByteString): ByteString = {
    if (bs.nonEmpty) {
      val r = RedisProtocolReply.decodeReply(bs)
      if (r.nonEmpty) {
        onReceivedReply(r.get._1)
        decodeReplies(r.get._2)
      } else {
        bs
      }
    } else {
      bs
    }
  }
}


object WriteAck

object Reconnect

object NoConnectionException extends RuntimeException("No Connection established")
