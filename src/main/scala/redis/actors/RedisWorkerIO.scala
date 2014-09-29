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

abstract class RedisWorkerIO(val address: InetSocketAddress) extends Actor with ActorLogging {

  private var currAddress = address

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
    log.info(s"Connect to $currAddress")
    // Create a new InetSocketAddress to clear the cached IP address.
    currAddress = new InetSocketAddress(currAddress.getHostName, currAddress.getPort)
    tcp ! Connect(currAddress)
  }

  def reconnect() = {
    become(receive)
    preStart()
  }

  override def postStop() {
    log.info("RedisWorkerIO stop")
  }

  def initConnectedBuffer() {
    readyToWrite = true
  }

  def receive = connecting orElse writing

  def connecting: Receive = {
    case a: InetSocketAddress => onAddressChanged(a)
    case c: Connected => onConnected(c)
    case Reconnect => reconnect()
    case c: CommandFailed => onConnectingCommandFailed(c)
    case c: ConnectionClosed => onClosingConnectionClosed() // not the current opening connection
  }

  def onConnected(cmd: Connected) = {
    sender ! Register(self)
    tcpWorker = sender
    initConnectedBuffer()
    tryInitialWrite() // TODO write something in head buffer
    become(connected)
    log.info("Connected to " + cmd.remoteAddress)
  }

  def onConnectingCommandFailed(cmdFailed: CommandFailed) = {
    log.error(cmdFailed.toString)
    scheduleReconnect()
  }

  def connected: Receive = writing orElse reading

  private def reading: Receive = {
    case WriteAck => tryWrite()
    case Received(dataByteString) => {
      if(sender == tcpWorker)
        onDataReceived(dataByteString)
      else
        onDataReceivedOnClosingConnection(dataByteString)
    }
    case a: InetSocketAddress => onAddressChanged(a)
    case c: ConnectionClosed => {
      if(sender == tcpWorker)
        onConnectionClosed(c)
      else
        onClosingConnectionClosed()
    }
    case c: CommandFailed => onConnectedCommandFailed(c)
  }

  def onAddressChanged(addr: InetSocketAddress) {
    log.info(s"Address change [old=$address, new=$addr]")
    tcpWorker ! ConfirmedClose // close the sending direction of the connection (TCP FIN)
    currAddress = addr
    scheduleReconnect()
  }

  def onConnectionClosed(c: ConnectionClosed) = {
    log.warning(s"ConnectionClosed $c")
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
    cleanState()
    log.info(s"Trying to reconnect in $reconnectDuration")
    this.context.system.scheduler.scheduleOnce(reconnectDuration, self, Reconnect)
    become(receive)
  }

  def cleanState() {
    onConnectionClosed()
    readyToWrite = false
    bufferWrite.clear()
  }

  def writing: Receive

  def onConnectionClosed()

  def onDataReceived(dataByteString: ByteString)

  def onDataReceivedOnClosingConnection(dataByteString: ByteString)

  def onClosingConnectionClosed()

  def onWriteSent()

  def restartConnection() = reconnect()

  def onConnectWrite(): ByteString

  def tryInitialWrite() {
    val data = onConnectWrite()

    if (data.nonEmpty) {
      writeWorker(data ++ bufferWrite.result())
      bufferWrite.clear()
    } else {
      tryWrite()
    }
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