package redis.actors

import akka.testkit.{TestActorRef, TestProbe, ImplicitSender, TestKit}
import akka.actor._
import org.specs2.mutable.{Tags, SpecificationLike}
import org.specs2.time.NoTimeConversions
import java.net.InetSocketAddress
import akka.util.ByteString
import scala.concurrent.{Await, Promise}
import redis.protocol.{RedisProtocolRequest, RedisReply}
import scala.collection.mutable
import redis.{Redis, Operation}
import redis.api.pubsub.{PMessage, Message}
import akka.io.Tcp._
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import redis.api.pubsub.Message
import redis.api.pubsub.PMessage

class RedisSubscriberActorSpec extends TestKit(ActorSystem()) with SpecificationLike with Tags with NoTimeConversions with ImplicitSender {

  import scala.concurrent.duration._

  "RedisClientActor" should {

    "connection closed -> reconnect" in {
      val probeMock = TestProbe()
      val channels = Seq("channel")
      val patterns = Seq("pattern.*")

      val subscriberActor = TestActorRef[SubscriberActor](Props(classOf[SubscriberActor],
        new InetSocketAddress("localhost", 6379), channels, patterns, probeMock.ref)
        .withDispatcher(Redis.dispatcher))

      val connectMsg = probeMock.expectMsgType[Connect]
      connectMsg mustEqual Connect(subscriberActor.underlyingActor.address)
      val probeTcpWorker = TestProbe()
      probeTcpWorker.send(subscriberActor, Connected(connectMsg.remoteAddress, connectMsg.remoteAddress))
      probeTcpWorker.expectMsgType[Register] mustEqual Register(subscriberActor)
      val bs = RedisProtocolRequest.multiBulk("SUBSCRIBE", channels.map(ByteString(_))) ++ RedisProtocolRequest.multiBulk("PSUBSCRIBE", patterns.map(ByteString(_)))
      probeTcpWorker.expectMsgType[Write] mustEqual Write(bs, WriteAck)
      probeTcpWorker.reply(WriteAck)

      val newChannels = channels :+ "channel2"
      subscriberActor.underlyingActor.subscribe("channel2")
      subscriberActor.underlyingActor.channelsSubscribed must containTheSameElementsAs(newChannels)
      probeTcpWorker.expectMsgType[Write] mustEqual Write(RedisProtocolRequest.multiBulk("SUBSCRIBE", Seq(ByteString("channel2"))), WriteAck)
      probeTcpWorker.reply(WriteAck)

      // ConnectionClosed
      probeTcpWorker.send(subscriberActor, ErrorClosed("test"))

      // Reconnect
      val connectMsg2 = probeMock.expectMsgType[Connect]
      connectMsg2 mustEqual Connect(subscriberActor.underlyingActor.address)
      val probeTcpWorker2 = TestProbe()
      probeTcpWorker2.send(subscriberActor, Connected(connectMsg2.remoteAddress, connectMsg2.remoteAddress))
      probeTcpWorker2.expectMsgType[Register] mustEqual Register(subscriberActor)

      // check the new Channel is there
      val bs2 = RedisProtocolRequest.multiBulk("SUBSCRIBE", newChannels.map(ByteString(_))) ++ RedisProtocolRequest.multiBulk("PSUBSCRIBE", patterns.map(ByteString(_)))
      val m = probeTcpWorker2.expectMsgType[Write]
      m mustEqual Write(bs2, WriteAck)
    }
  }
}

class SubscriberActor( override val address: InetSocketAddress,
                       channels: Seq[String],
                       patterns: Seq[String],
                       probeMock: ActorRef
                       ) extends RedisSubscriberActor(channels, patterns) {

  override val tcp = probeMock

  override def onMessage(m: Message) = {
    probeMock ! m
  }

  def onPMessage(pm: PMessage) {
    probeMock ! pm
  }
}