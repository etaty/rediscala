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
import redis.api.connection.Ping

class RedisClientActorSpec extends TestKit(ActorSystem()) with SpecificationLike with Tags with NoTimeConversions with ImplicitSender {

  import scala.concurrent.duration._

  "RedisClientActor" should {

    "ok" in {
      val probeReplyDecoder = TestProbe()
      val probeMock = TestProbe()

      val redisClientActor = TestActorRef[RedisClientActorMock](Props(classOf[RedisClientActorMock], probeReplyDecoder.ref, probeMock.ref).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

      val promise = Promise[String]()
      val op1 = Operation(Ping, promise)
      redisClientActor ! op1
      val promise2 = Promise[String]()
      val op2 = Operation(Ping, promise2)
      redisClientActor ! op2

      probeMock.expectMsg(WriteMock) mustEqual WriteMock
      redisClientActor.underlyingActor.queuePromises.length mustEqual 2

      //onWriteSent
      redisClientActor.underlyingActor.onWriteSent()
      probeReplyDecoder.expectMsgType[mutable.Queue[Operation[_]]] mustEqual mutable.Queue[Operation[_]](op1, op2)
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

      val redisClientActor = TestActorRef[RedisClientActorMock](Props(classOf[RedisClientActorMock], probeReplyDecoder.ref, probeMock.ref)
        .withDispatcher("rediscala.rediscala-client-worker-dispatcher"))
        .underlyingActor

      val promise3 = Promise[String]()
      redisClientActor.receive(Operation(Ping, promise3))
      redisClientActor.queuePromises.length mustEqual 1

      val deathWatcher = TestProbe()
      deathWatcher.watch(probeReplyDecoder.ref)

      redisClientActor.onConnectionClosed()
      deathWatcher.expectTerminated(probeReplyDecoder.ref) must beAnInstanceOf[Terminated]
      Await.result(promise3.future, 10 seconds) must throwA(NoConnectionException)
    }

    "replyDecoder died -> reset connection" in {
      val probeReplyDecoder = TestProbe()
      val probeMock = TestProbe()

      val redisClientActorRef = TestActorRef[RedisClientActorMock](Props(classOf[RedisClientActorMock], probeReplyDecoder.ref, probeMock.ref)
        .withDispatcher("rediscala.rediscala-client-worker-dispatcher"))
      val redisClientActor = redisClientActorRef.underlyingActor

      val promiseSent = Promise[String]()
      val promiseNotSent = Promise[String]()
      val operation = Operation(Ping, promiseSent)
      redisClientActor.receive(operation)
      redisClientActor.queuePromises.length mustEqual 1

      redisClientActor.onWriteSent()
      redisClientActor.queuePromises must beEmpty
      probeReplyDecoder.expectMsgType[mutable.Queue[Operation[_]]] mustEqual mutable.Queue[Operation[_]](operation)

      redisClientActor.receive(Operation(Ping, promiseNotSent))
      redisClientActor.queuePromises.length mustEqual 1

      val deathWatcher = TestProbe()
      deathWatcher.watch(probeReplyDecoder.ref)
      deathWatcher.watch(redisClientActorRef)

      probeReplyDecoder.ref ! Kill
      deathWatcher.expectTerminated(probeReplyDecoder.ref) must beAnInstanceOf[Terminated]
      redisClientActor.queuePromises.length mustEqual 1
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