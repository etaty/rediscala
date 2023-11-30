package redis.actors

import org.apache.pekko.testkit._
import org.apache.pekko.actor.{ActorRef, Props, ActorSystem}
import org.specs2.mutable.SpecificationLike
import java.net.InetSocketAddress
import org.apache.pekko.io.Tcp._
import org.apache.pekko.util.ByteString
import org.apache.pekko.io.Tcp.ErrorClosed
import org.apache.pekko.io.Tcp.Connected
import org.apache.pekko.io.Tcp.Register
import org.apache.pekko.io.Tcp.Connect
import org.apache.pekko.io.Tcp.CommandFailed
import redis.Redis

class RedisWorkerIOSpec extends TestKit(ActorSystem()) with SpecificationLike with ImplicitSender {

  import scala.concurrent.duration._

  val timeout = 120.seconds dilated

  "RedisWorkerIO" should {

    val address = new InetSocketAddress("localhost", 6379)
    "connect CommandFailed then reconnect" in within(timeout){
      val probeTcp = TestProbe()
      val probeMock = TestProbe()

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref, ByteString.empty).withDispatcher(Redis.dispatcher.name))

      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address, options = SO.KeepAlive(on = true) :: Nil)
      probeTcp.reply(CommandFailed(connectMsg))
      probeMock.expectMsg(OnConnectionClosed) mustEqual OnConnectionClosed

      // should reconnect in 2s
      within(1 second, 4 seconds) {
        val connectMsg = probeTcp.expectMsgType[Connect]
        connectMsg mustEqual Connect(address, options = SO.KeepAlive(on = true) :: Nil)
        connectMsg.remoteAddress must not beTheSameAs(address)

        val probeTcpWorker = TestProbe()
        probeTcpWorker.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

        probeTcpWorker.expectMsgType[Register] mustEqual Register(redisWorkerIO)
      }
    }

    "ok" in within(timeout){
      val probeTcp = TestProbe()
      val probeMock = TestProbe()

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref, ByteString.empty).withDispatcher(Redis.dispatcher.name))

      redisWorkerIO ! "PING1"

      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address, options = SO.KeepAlive(on = true) :: Nil)
      val probeTcpWorker = TestProbe()
      probeTcpWorker.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

      probeTcpWorker.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      probeTcpWorker.expectMsgType[Write] mustEqual Write(ByteString("PING1"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent

      redisWorkerIO ! "PING2"
      redisWorkerIO ! "PING3"
      probeTcpWorker.reply(WriteAck)
      probeTcpWorker.expectMsgType[Write] mustEqual Write(ByteString("PING2PING3"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent

      redisWorkerIO ! "PING"
      probeTcpWorker.expectNoMessage(1 seconds)
      probeTcpWorker.send(redisWorkerIO, WriteAck)
      probeTcpWorker.expectMsgType[Write] mustEqual Write(ByteString("PING"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent
    }

    "reconnect : connected <-> disconnected" in within(timeout){
      val probeTcp = TestProbe()
      val probeMock = TestProbe()

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref, ByteString.empty).withDispatcher(Redis.dispatcher.name))

      redisWorkerIO ! "PING1"

      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address, options = SO.KeepAlive(on = true) :: Nil)
      val probeTcpWorker = TestProbe()
      probeTcpWorker.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

      probeTcpWorker.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      probeTcpWorker.expectMsgType[Write] mustEqual Write(ByteString("PING1"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent

      redisWorkerIO ! "PING 2"
      awaitAssert(redisWorkerIO.underlyingActor.bufferWrite.result mustEqual ByteString("PING 2"))
      // ConnectionClosed
      probeTcpWorker.send(redisWorkerIO, ErrorClosed("test"))
      probeMock.expectMsg(OnConnectionClosed) mustEqual OnConnectionClosed
      awaitAssert(redisWorkerIO.underlyingActor.bufferWrite.length mustEqual 0)

      // Reconnect
      val connectMsg2 = probeTcp.expectMsgType[Connect]
      connectMsg2 mustEqual Connect(address, options = SO.KeepAlive(on = true) :: Nil)
      val probeTcpWorker2 = TestProbe()
      probeTcpWorker2.send(redisWorkerIO, Connected(connectMsg2.remoteAddress, address))
      probeTcpWorker2.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      redisWorkerIO ! "PING1"
      probeTcpWorker2.expectMsgType[Write] mustEqual Write(ByteString("PING1"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent
    }

    "onConnectedCommandFailed" in within(timeout){
      val probeTcp = TestProbe()
      val probeMock = TestProbe()

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref, ByteString.empty).withDispatcher(Redis.dispatcher.name))

      redisWorkerIO ! "PING1"

      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address, options = SO.KeepAlive(on = true) :: Nil)
      val probeTcpWorker = TestProbe()
      probeTcpWorker.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

      probeTcpWorker.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      val msg = probeTcpWorker.expectMsgType[Write]
      msg mustEqual Write(ByteString("PING1"), WriteAck)

      probeTcpWorker.reply(CommandFailed(msg))
      probeTcpWorker.expectMsgType[Write] mustEqual Write(ByteString("PING1"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent
    }

    "received" in within(timeout){
      val probeTcp = TestProbe()
      val probeMock = TestProbe()

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref, ByteString.empty).withDispatcher(Redis.dispatcher.name))

      redisWorkerIO ! "PING1"

      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address, options = SO.KeepAlive(on = true) :: Nil)
      val probeTcpWorker = TestProbe()
      probeTcpWorker.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

      probeTcpWorker.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      probeTcpWorker.expectMsgType[Write] mustEqual Write(ByteString("PING1"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent

      probeTcpWorker.send(redisWorkerIO, Received(ByteString("PONG")))
      probeMock.expectMsgType[ByteString] mustEqual ByteString("PONG")
    }

    "Address Changed" in within(timeout){
      val probeTcp = TestProbe()
      val probeMock = TestProbe()

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref, ByteString.empty).withDispatcher(Redis.dispatcher.name))

      redisWorkerIO ! "PING1"

      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address, options = SO.KeepAlive(on = true) :: Nil)
      val probeTcpWorker = TestProbe()
      probeTcpWorker.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

      probeTcpWorker.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      probeTcpWorker.expectMsgType[Write] mustEqual Write(ByteString("PING1"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent
      probeTcpWorker.reply(WriteAck)

      // change adresse
      val address2 = new InetSocketAddress("localhost", 6380)
      redisWorkerIO ! address2

      probeMock.expectMsg(OnConnectionClosed) mustEqual OnConnectionClosed

      redisWorkerIO ! "PING2"

      val connectMsg2 = probeTcp.expectMsgType[Connect]
      connectMsg2 mustEqual Connect(address2, options = SO.KeepAlive(on = true) :: Nil)

      val probeTcpWorker2 = TestProbe()
      probeTcpWorker2.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

      probeTcpWorker2.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      probeTcpWorker2.expectMsgType[Write] mustEqual Write(ByteString("PING2"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent
      probeTcpWorker2.reply(WriteAck)

      // receiving data on connection with the sending direction closed
      probeTcpWorker.send(redisWorkerIO, Received(ByteString("PONG1")))
      probeMock.expectMsg(DataReceivedOnClosingConnection) mustEqual DataReceivedOnClosingConnection

      // receiving data on open connection
      probeTcpWorker2.send(redisWorkerIO, Received(ByteString("PONG2")))
      probeMock.expectMsgType[ByteString] mustEqual ByteString("PONG2")

      // close connection
      probeTcpWorker.send(redisWorkerIO, ConfirmedClosed)
      probeMock.expectMsg(ClosingConnectionClosed) mustEqual ClosingConnectionClosed
    }

    "on connect write" in within(timeout){
      val probeTcp = TestProbe()
      val probeMock = TestProbe()
      val onConnectByteString = ByteString("on connect write")

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref, onConnectByteString).withDispatcher(Redis.dispatcher.name))


      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address, options = SO.KeepAlive(on = true) :: Nil)
      val probeTcpWorker = TestProbe()
      probeTcpWorker.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

      probeTcpWorker.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      probeTcpWorker.expectMsgType[Write] mustEqual Write(onConnectByteString, WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent

      redisWorkerIO ! "PING1"
      awaitAssert(redisWorkerIO.underlyingActor.bufferWrite.result mustEqual ByteString("PING1"))

      // ConnectionClosed
      probeTcpWorker.send(redisWorkerIO, ErrorClosed("test"))
      probeMock.expectMsg(OnConnectionClosed) mustEqual OnConnectionClosed

      awaitAssert(redisWorkerIO.underlyingActor.bufferWrite.length mustEqual 0)

      // Reconnect
      val connectMsg2 = probeTcp.expectMsgType[Connect]
      connectMsg2 mustEqual Connect(address, options = SO.KeepAlive(on = true) :: Nil)
      val probeTcpWorker2 = TestProbe()
      probeTcpWorker2.send(redisWorkerIO, Connected(connectMsg2.remoteAddress, address))
      probeTcpWorker2.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      probeTcpWorker2.expectMsgType[Write] mustEqual Write(onConnectByteString, WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent
    }
  }
}


class RedisWorkerIOMock(probeTcp: ActorRef, address: InetSocketAddress, probeMock: ActorRef, _onConnectWrite: ByteString) extends RedisWorkerIO(address, (status:Boolean) =>{()} ) {
  override val tcp = probeTcp

  def writing: Receive = {
    case s: String => write(ByteString(s))
  }

  def onConnectionClosed(): Unit = {
    probeMock ! OnConnectionClosed
  }

  def onDataReceived(dataByteString: ByteString): Unit = {
    probeMock ! dataByteString
  }

  def onWriteSent(): Unit = {
    probeMock ! WriteSent
  }

  def onConnectWrite(): ByteString = _onConnectWrite

  def onDataReceivedOnClosingConnection(dataByteString: ByteString): Unit = probeMock ! DataReceivedOnClosingConnection

  def onClosingConnectionClosed(): Unit = probeMock ! ClosingConnectionClosed
}

object WriteSent

object OnConnectionClosed

object DataReceivedOnClosingConnection

object ClosingConnectionClosed
