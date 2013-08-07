package redis.actors

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.io.Tcp
import akka.util.{ByteStringBuilder, ByteString}
import java.net.InetSocketAddress
import akka.io.Tcp._
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import akka.io.Tcp.Received

trait RedisWorkerIO extends Actor with ActorLogging{

  def address: InetSocketAddress

  import context._

  val tcp = akka.io.IO(Tcp)(context.system)

  // todo watch tcpWorker
  var tcpWorker: ActorRef = null

  val bufferWrite: ByteStringBuilder = new ByteStringBuilder

  var readyToWrite = false

  override def preStart() {
    if (tcpWorker != null) {
      tcpWorker ! Close
    }
    log.info(s"Connect to $address")
    tcp ! Connect(address)
  }

  override def postStop() {
    log.info("RedisWorkerIO stop")
  }

  def initConnectedBuffer() {
    readyToWrite = true
  }

  def receive = connecting orElse writing

  def connecting: Receive = {
    case c: Connected => onConnected(c)
    case Reconnect => preStart()
    case c: CommandFailed => onConnectingCommandFailed(c)
  }

  def onConnected(cmd: Connected) = {
    sender ! Register(self)
    tcpWorker = sender
    initConnectedBuffer()
    tryWrite()
    become(connected)
    log.info("Connected to " + cmd.remoteAddress)
  }

  def onConnectingCommandFailed(cmdFailed: CommandFailed) = {
    log.error(cmdFailed.toString)
    cleanState()
    scheduleReconnect()
  }

  def connected: Receive = writing orElse reading

  private def reading: Receive = {
    case WriteAck => tryWrite()
    case Received(dataByteString) => onDataReceived(dataByteString)
    case c: ConnectionClosed => onConnectionClosed(c)
    case c: CommandFailed => onConnectedCommandFailed(c)

  }

  def onConnectionClosed(c: ConnectionClosed) = {
    log.warning(s"ConnectionClosed $c")
    cleanState()
    scheduleReconnect()
  }

  /** O/S buffer was full
    * Maybe to much data in the Command ?
    */
  def onConnectedCommandFailed(commandFailed: CommandFailed) = {
    log.error(commandFailed.toString) // O/S buffer was full
    tcpWorker ! commandFailed.cmd
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

  def onDataReceived(dataByteString: ByteString)

  def onWriteSent()

  def restartConnection() = {
    if (tcpWorker != null) {
      tcpWorker ! Close
    }
    scheduleReconnect()
  }

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
    } else {
      bufferWrite.append(byteString)
    }
  }

  import scala.concurrent.duration.{DurationInt, FiniteDuration}

  def reconnectDuration: FiniteDuration = 2 seconds

  private def writeWorker(byteString: ByteString) {
    onWriteSent()
    tcpWorker ! Write(byteString, WriteAck)
    readyToWrite = false
  }

}


object WriteAck extends Event

object Reconnect