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
import redis.Operation

class RedisClientActorSpec extends TestKit(ActorSystem()) with SpecificationLike with Tags with NoTimeConversions with ImplicitSender {

  import scala.concurrent.duration._

  "RedisClientActor" should {

    "ok" in {
      val probeReplyDecoder = TestProbe()
      val probeMock = TestProbe()

      val redisClientActor = TestActorRef[RedisClientActorMock](Props(classOf[RedisClientActorMock], probeReplyDecoder.ref, probeMock.ref).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

      val promise = Promise[RedisReply]()
      redisClientActor ! Operation(RedisProtocolRequest.inline("PING"), promise)
      val promise2 = Promise[RedisReply]()
      redisClientActor ! Operation(RedisProtocolRequest.inline("PING"), promise2)

      probeMock.expectMsg(WriteMock) mustEqual WriteMock
      redisClientActor.underlyingActor.queuePromises.length mustEqual 2

      //onWriteSent
      redisClientActor.underlyingActor.onWriteSent()
      probeReplyDecoder.expectMsgType[mutable.Queue[Promise[RedisReply]]] mustEqual mutable.Queue[Promise[RedisReply]](promise, promise2)
      redisClientActor.underlyingActor.queuePromises must beEmpty

      //onDataReceived
      redisClientActor.underlyingActor.onDataReceived(ByteString.empty)
      probeReplyDecoder.expectMsgType[ByteString] mustEqual ByteString.empty

      redisClientActor.underlyingActor.onDataReceived(ByteString("bytestring"))
      probeReplyDecoder.expectMsgType[ByteString] mustEqual ByteString("bytestring")

      //onConnectionClosed
      val deathWatcher = TestProbe()
      deathWatcher.watch(probeReplyDecoder.ref)
      redisClientActor.underlyingActor.onConnectionClosed()
      deathWatcher.expectTerminated(probeReplyDecoder.ref) must beAnInstanceOf[Terminated]
    }

    "onConnectionClosed with promises queued" in {
      val probeReplyDecoder = TestProbe()
      val probeMock = TestProbe()

      val redisClientActor = TestActorRef[RedisClientActorMock](Props(classOf[RedisClientActorMock], probeReplyDecoder.ref, probeMock.ref).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

      val promise3 = Promise[RedisReply]()
      redisClientActor ! Operation(RedisProtocolRequest.inline("PING"), promise3)
      redisClientActor.underlyingActor.queuePromises.length mustEqual 1

      val deathWatcher = TestProbe()
      deathWatcher.watch(probeReplyDecoder.ref)

      redisClientActor.underlyingActor.onConnectionClosed()
      deathWatcher.expectTerminated(probeReplyDecoder.ref) must beAnInstanceOf[Terminated]
      Await.result(promise3.future, 10 seconds) must throwA(NoConnectionException)
    }

    "replyDecoder died -> reset connection" in {
      val probeReplyDecoder = TestProbe()
      val probeMock = TestProbe()

      val redisClientActor = TestActorRef[RedisClientActorMock](Props(classOf[RedisClientActorMock], probeReplyDecoder.ref, probeMock.ref).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

      val promiseSent = Promise[RedisReply]()
      val promiseNotSent = Promise[RedisReply]()
      redisClientActor ! Operation(RedisProtocolRequest.inline("PING"), promiseSent)
      redisClientActor.underlyingActor.queuePromises.length mustEqual 1

      redisClientActor.underlyingActor.onWriteSent()
      redisClientActor.underlyingActor.queuePromises must beEmpty
      probeReplyDecoder.expectMsgType[mutable.Queue[Promise[RedisReply]]] mustEqual mutable.Queue[Promise[RedisReply]](promiseSent)

      redisClientActor ! Operation(RedisProtocolRequest.inline("PING"), promiseNotSent)
      redisClientActor.underlyingActor.queuePromises.length mustEqual 1

      val deathWatcher = TestProbe()
      deathWatcher.watch(probeReplyDecoder.ref)
      deathWatcher.watch(redisClientActor)

      probeReplyDecoder.ref ! Kill
      deathWatcher.expectTerminated(probeReplyDecoder.ref) must beAnInstanceOf[Terminated]
      redisClientActor.underlyingActor.queuePromises.length mustEqual 1
    }
  }
}

class RedisClientActorMock(probeReplyDecoder: ActorRef, probeMock: ActorRef) extends RedisClientActor(new InetSocketAddress("localhost", 6379)) {
  override def initRepliesDecoder = probeReplyDecoder

  override def preStart() {
    // disable preStart of RedisWorkerIO
  }

  override def write(byteString: ByteString) {
    probeMock ! WriteMock
  }
}

object WriteMock