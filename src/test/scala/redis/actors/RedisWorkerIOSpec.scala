package redis.actors

import akka.testkit.{TestActorRef, TestProbe, ImplicitSender, TestKit}
import akka.actor.{ActorRef, Props, ActorSystem}
import org.specs2.mutable.{Tags, SpecificationLike}
import org.specs2.time.NoTimeConversions
import java.net.InetSocketAddress
import akka.io.Tcp._
import akka.util.ByteString
import akka.io.Tcp.ErrorClosed
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import redis.Redis

class RedisWorkerIOSpec extends TestKit(ActorSystem()) with SpecificationLike with Tags with NoTimeConversions with ImplicitSender {

  import scala.concurrent.duration._

  "RedisWorkerIO" should {

    val address = new InetSocketAddress("localhost", 6379)
    "connect CommandFailed then reconnect" in {
      val probeTcp = TestProbe()
      val probeMock = TestProbe()

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref).withDispatcher(Redis.dispatcher))

      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address)
      probeTcp.reply(CommandFailed(connectMsg))
      probeMock.expectMsg(OnConnectionClosed) mustEqual OnConnectionClosed

      // should reconnect in 2s
      within(1 second, 4 seconds) {
        val connectMsg = probeTcp.expectMsgType[Connect]
        connectMsg mustEqual Connect(address)

        val probeTcpWorker = TestProbe()
        probeTcpWorker.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

        probeTcpWorker.expectMsgType[Register] mustEqual Register(redisWorkerIO)
      }
    }

    "ok" in {
      val probeTcp = TestProbe()
      val probeMock = TestProbe()

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref).withDispatcher(Redis.dispatcher))

      redisWorkerIO ! "PING1"

      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address)
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
      probeTcpWorker.expectNoMsg(1 seconds)
      probeTcpWorker.send(redisWorkerIO, WriteAck)
      probeTcpWorker.expectMsgType[Write] mustEqual Write(ByteString("PING"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent
    }

    "reconnect : connected <-> disconnected" in {
      val probeTcp = TestProbe()
      val probeMock = TestProbe()

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref).withDispatcher(Redis.dispatcher))

      redisWorkerIO ! "PING1"

      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address)
      val probeTcpWorker = TestProbe()
      probeTcpWorker.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

      probeTcpWorker.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      probeTcpWorker.expectMsgType[Write] mustEqual Write(ByteString("PING1"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent

      // ConnectionClosed
      probeTcpWorker.send(redisWorkerIO, ErrorClosed("test"))
      probeMock.expectMsg(OnConnectionClosed) mustEqual OnConnectionClosed

      // Reconnect
      val connectMsg2 = probeTcp.expectMsgType[Connect]
      connectMsg2 mustEqual Connect(address)
      val probeTcpWorker2 = TestProbe()
      probeTcpWorker2.send(redisWorkerIO, Connected(connectMsg2.remoteAddress, address))
      probeTcpWorker2.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      redisWorkerIO ! "PING1"
      probeTcpWorker2.expectMsgType[Write] mustEqual Write(ByteString("PING1"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent
    }

    "onConnectedCommandFailed" in {
      val probeTcp = TestProbe()
      val probeMock = TestProbe()

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref).withDispatcher(Redis.dispatcher))

      redisWorkerIO ! "PING1"

      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address)
      val probeTcpWorker = TestProbe()
      probeTcpWorker.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

      probeTcpWorker.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      val msg = probeTcpWorker.expectMsgType[Write]
      msg mustEqual Write(ByteString("PING1"), WriteAck)

      probeTcpWorker.reply(CommandFailed(msg))
      probeTcpWorker.expectMsgType[Write] mustEqual Write(ByteString("PING1"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent
    }

    "received" in {
      val probeTcp = TestProbe()
      val probeMock = TestProbe()

      val redisWorkerIO = TestActorRef[RedisWorkerIOMock](Props(classOf[RedisWorkerIOMock], probeTcp.ref, address, probeMock.ref).withDispatcher(Redis.dispatcher))

      redisWorkerIO ! "PING1"

      val connectMsg = probeTcp.expectMsgType[Connect]
      connectMsg mustEqual Connect(address)
      val probeTcpWorker = TestProbe()
      probeTcpWorker.send(redisWorkerIO, Connected(connectMsg.remoteAddress, address))

      probeTcpWorker.expectMsgType[Register] mustEqual Register(redisWorkerIO)

      probeTcpWorker.expectMsgType[Write] mustEqual Write(ByteString("PING1"), WriteAck)
      probeMock.expectMsg(WriteSent) mustEqual WriteSent

      probeTcpWorker.send(redisWorkerIO, Received(ByteString("PONG")))
      probeMock.expectMsgType[ByteString] mustEqual ByteString("PONG")
    }

    "Address Changed" in {
      // stop / close akka worker ?
      todo
    }
  }
}


class RedisWorkerIOMock(probeTcp: ActorRef, override val address: InetSocketAddress, probeMock: ActorRef) extends RedisWorkerIO {
  override val tcp = probeTcp

  def writing: Receive = {
    case s: String => write(ByteString(s))
  }

  def onConnectionClosed() {
    probeMock ! OnConnectionClosed
  }

  def onDataReceived(dataByteString: ByteString) {
    probeMock ! dataByteString
  }

  def onWriteSent() {
    probeMock ! WriteSent
  }
}

object WriteSent

object OnConnectionClosed