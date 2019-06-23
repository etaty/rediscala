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
import scala.concurrent.duration.FiniteDuration

abstract class RedisWorkerIO(val address: InetSocketAddress, onConnectStatus: Boolean => Unit, connectTimeout: Option[FiniteDuration] = None) extends Actor with ActorLogging {

  private var currAddress = address

  import context._

  val tcp = akka.io.IO(Tcp)(context.system)

  // todo watch tcpWorker
  var tcpWorker: ActorRef = null

  val bufferWrite: ByteStringBuilder = new ByteStringBuilder

  var readyToWrite = false

  override def preStart(): Unit = {
    if (tcpWorker != null) {
      tcpWorker ! Close
    }
    log.info(s"Connect to $currAddress")
    // Create a new InetSocketAddress to clear the cached IP address.
    currAddress = new InetSocketAddress(currAddress.getHostName, currAddress.getPort)
    tcp ! Connect(remoteAddress = currAddress, options = SO.KeepAlive(on = true) :: Nil, timeout = connectTimeout)
  }

  def reconnect() = {
    become(receive)
    preStart()
  }

  override def postStop(): Unit = {
    log.info("RedisWorkerIO stop")
  }

  def initConnectedBuffer(): Unit = {
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
    onConnectStatus(true)
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
      else {
        onConnectStatus(false)
        onClosingConnectionClosed()
      }
    }
    case c: CommandFailed => onConnectedCommandFailed(c)
  }

  def onAddressChanged(addr: InetSocketAddress): Unit = {
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

  def scheduleReconnect(): Unit = {
    cleanState()
    log.info(s"Trying to reconnect in $reconnectDuration")
    this.context.system.scheduler.scheduleOnce(reconnectDuration, self, Reconnect)
    become(receive)
  }

  def cleanState(): Unit = {
    onConnectStatus(false)
    onConnectionClosed()
    readyToWrite = false
    bufferWrite.clear()
  }

  def writing: Receive

  def onConnectionClosed(): Unit

  def onDataReceived(dataByteString: ByteString): Unit

  def onDataReceivedOnClosingConnection(dataByteString: ByteString): Unit

  def onClosingConnectionClosed(): Unit

  def onWriteSent(): Unit

  def restartConnection() = reconnect()

  def onConnectWrite(): ByteString

  def tryInitialWrite(): Unit = {
    val data = onConnectWrite()

    if (data.nonEmpty) {
      writeWorker(data ++ bufferWrite.result())
      bufferWrite.clear()
    } else {
      tryWrite()
    }
  }

  def tryWrite(): Unit = {
    if (bufferWrite.length == 0) {
      readyToWrite = true
    } else {
      writeWorker(bufferWrite.result())
      bufferWrite.clear()
    }
  }

  def write(byteString: ByteString): Unit = {
    if (readyToWrite) {
      writeWorker(byteString)
    } else {
      bufferWrite.append(byteString)
    }
  }

  import scala.concurrent.duration.{DurationInt, FiniteDuration}

  def reconnectDuration: FiniteDuration = 2 seconds

  private def writeWorker(byteString: ByteString): Unit = {
    onWriteSent()
    tcpWorker ! Write(byteString, WriteAck)
    readyToWrite = false
  }

}


object WriteAck extends Event

object Reconnect