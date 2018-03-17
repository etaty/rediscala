package redis.actors

import java.net.InetSocketAddress

import akka.actor._
import akka.testkit._
import akka.util.ByteString
import org.specs2.mutable.SpecificationLike
import redis.api.connection.Ping
import redis.api.strings.Get
import redis.{Operation, Redis}

import scala.collection.mutable
import scala.concurrent.{Await, Promise}

class RedisClientActorSpec extends TestKit(ActorSystem()) with SpecificationLike with ImplicitSender {

  import scala.concurrent.duration._

  val getConnectOperations: () => Seq[Operation[_, _]] = () => {
    Seq()
  }

  val timeout = 120.seconds dilated

  val onConnectStatus: (Boolean) => Unit = (status:Boolean) => {}

  "RedisClientActor" should {

    "ok" in within(timeout){
      val probeReplyDecoder = TestProbe()
      val probeMock = TestProbe()


      val promiseConnect1 = Promise[String]()
      val opConnectPing = Operation(Ping, promiseConnect1)
      val promiseConnect2 = Promise[Option[ByteString]]()
      val getCmd = Get("key")
      val opConnectGet = Operation(getCmd, promiseConnect2)

      val getConnectOperations: () => Seq[Operation[_, _]] = () => {
        Seq(opConnectPing, opConnectGet)
      }


      val redisClientActor = TestActorRef[RedisClientActorMock](Props(classOf[RedisClientActorMock], probeReplyDecoder.ref, probeMock.ref, getConnectOperations, onConnectStatus)
        .withDispatcher(Redis.dispatcher.name))

      val promise = Promise[String]()
      val op1 = Operation(Ping, promise)
      redisClientActor ! op1
      val promise2 = Promise[String]()
      val op2 = Operation(Ping, promise2)
      redisClientActor ! op2

      probeMock.expectMsg(WriteMock) mustEqual WriteMock
      awaitAssert(redisClientActor.underlyingActor.queuePromises.length mustEqual 2)

      //onConnectWrite
      redisClientActor.underlyingActor.onConnectWrite()
      awaitAssert(redisClientActor.underlyingActor.queuePromises.result() mustEqual Seq(opConnectPing, opConnectGet, op1, op2))
      awaitAssert(redisClientActor.underlyingActor.queuePromises.length mustEqual 4)

      //onWriteSent
      redisClientActor.underlyingActor.onWriteSent()
      probeReplyDecoder.expectMsgType[QueuePromises] mustEqual QueuePromises(mutable.Queue(opConnectPing, opConnectGet, op1, op2))
      awaitAssert(redisClientActor.underlyingActor.queuePromises must beEmpty)

      //onDataReceived
      awaitAssert(redisClientActor.underlyingActor.onDataReceived(ByteString.empty))
      probeReplyDecoder.expectMsgType[ByteString] mustEqual ByteString.empty

      awaitAssert(redisClientActor.underlyingActor.onDataReceived(ByteString("bytestring")))
      probeReplyDecoder.expectMsgType[ByteString] mustEqual ByteString("bytestring")

      //onConnectionClosed
      val deathWatcher = TestProbe()
      deathWatcher.watch(probeReplyDecoder.ref)
      redisClientActor.underlyingActor.onConnectionClosed()
      deathWatcher.expectTerminated(probeReplyDecoder.ref, 30 seconds) must beAnInstanceOf[Terminated]
    }

    "onConnectionClosed with promises queued" in {
      val probeReplyDecoder = TestProbe()
      val probeMock = TestProbe()

      val redisClientActor = TestActorRef[RedisClientActorMock](Props(classOf[RedisClientActorMock], probeReplyDecoder.ref, probeMock.ref, getConnectOperations, onConnectStatus)
        .withDispatcher(Redis.dispatcher.name))
        .underlyingActor

      val promise3 = Promise[String]()
      redisClientActor.receive(Operation(Ping, promise3))
      redisClientActor.queuePromises.length mustEqual 1

      val deathWatcher = TestProbe()
      deathWatcher.watch(probeReplyDecoder.ref)

      redisClientActor.onConnectionClosed()
      deathWatcher.expectTerminated(probeReplyDecoder.ref, 30 seconds) must beAnInstanceOf[Terminated]
      Await.result(promise3.future, 10 seconds) must throwA(NoConnectionException)
    }

    "replyDecoder died -> reset connection" in {
      val probeReplyDecoder = TestProbe()
      val probeMock = TestProbe()

      val redisClientActorRef = TestActorRef[RedisClientActorMock](Props(classOf[RedisClientActorMock], probeReplyDecoder.ref, probeMock.ref, getConnectOperations, onConnectStatus)
        .withDispatcher(Redis.dispatcher.name))
      val redisClientActor = redisClientActorRef.underlyingActor

      val promiseSent = Promise[String]()
      val promiseNotSent = Promise[String]()
      val operation = Operation(Ping, promiseSent)
      redisClientActor.receive(operation)
      redisClientActor.queuePromises.length mustEqual 1

      redisClientActor.onWriteSent()
      redisClientActor.queuePromises must beEmpty
      probeReplyDecoder.expectMsgType[QueuePromises] mustEqual QueuePromises(mutable.Queue(operation))

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

class RedisClientActorMock(probeReplyDecoder: ActorRef, probeMock: ActorRef, getConnectOperations: () => Seq[Operation[_, _]], onConnectStatus: Boolean => Unit )
  extends RedisClientActor(new InetSocketAddress("localhost", 6379), getConnectOperations, onConnectStatus, Redis.dispatcher.name) {
  override def initRepliesDecoder() = probeReplyDecoder

  override def preStart() {
    // disable preStart of RedisWorkerIO
  }

  override def write(byteString: ByteString) {
    probeMock ! WriteMock
  }
}

object WriteMock
