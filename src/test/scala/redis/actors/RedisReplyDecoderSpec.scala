package redis.actors

import akka.actor._
import org.specs2.mutable.{Tags, SpecificationLike}
import org.specs2.time.NoTimeConversions
import akka.util.ByteString
import scala.concurrent.{Await, Promise}
import scala.collection.mutable
import java.net.InetSocketAddress
import com.typesafe.config.ConfigFactory
import redis.{Redis, Operation}
import redis.api.connection.Ping
import akka.testkit._

class RedisReplyDecoderSpec
  extends TestKit(ActorSystem("testsystem", ConfigFactory.parseString( """akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with SpecificationLike with Tags with NoTimeConversions with ImplicitSender {

  import scala.concurrent.duration._

  sequential

  val timeout = 120.seconds dilated

  "RedisReplyDecoder" should {
    "ok" in within(timeout){
      val promise = Promise[String]()
      val operation = Operation(Ping, promise)
      val q = QueuePromises(mutable.Queue[Operation[_, _]]())
      q.queue.enqueue(operation)

      val redisReplyDecoder = TestActorRef[RedisReplyDecoder](Props(classOf[RedisReplyDecoder]).withDispatcher(Redis.dispatcher))

      redisReplyDecoder.underlyingActor.queuePromises must beEmpty

      redisReplyDecoder ! q
      awaitAssert(redisReplyDecoder.underlyingActor.queuePromises.size mustEqual 1)

      redisReplyDecoder ! ByteString("+PONG\r\n")
      Await.result(promise.future, timeout) mustEqual "PONG"

      awaitAssert(redisReplyDecoder.underlyingActor.queuePromises must beEmpty)

      val promise2 = Promise[String]()
      val promise3 = Promise[String]()
      val op2 = Operation(Ping, promise2)
      val op3 = Operation(Ping, promise3)
      val q2 = QueuePromises(mutable.Queue[Operation[_, _]]())
      q2.queue.enqueue(op2)
      q2.queue.enqueue(op3)

      redisReplyDecoder ! q2
      awaitAssert(redisReplyDecoder.underlyingActor.queuePromises.size mustEqual 2)

      redisReplyDecoder ! ByteString("+PONG\r\n+PONG\r\n")
      Await.result(promise2.future, timeout) mustEqual "PONG"
      Await.result(promise3.future, timeout) mustEqual "PONG"
      redisReplyDecoder.underlyingActor.queuePromises must beEmpty
    }

    "can't decode" in within(timeout){
      val probeMock = TestProbe()

      val redisClientActor = TestActorRef[RedisClientActorMock2](Props(classOf[RedisClientActorMock2], probeMock.ref).withDispatcher(Redis.dispatcher))
      val promise = Promise[String]()
      redisClientActor ! Operation(Ping, promise)
      awaitAssert({
        redisClientActor.underlyingActor.queuePromises.length mustEqual 1
        redisClientActor.underlyingActor.onWriteSent()
        redisClientActor.underlyingActor.queuePromises must beEmpty
      }, timeout)

      EventFilter[Exception](occurrences = 1, start = "Redis Protocol error: Got 110 as initial reply byte").intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("not valid redis reply"))
      })
      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"
      Await.result(promise.future, timeout) must throwA(InvalidRedisReply)


      val promise2 = Promise[String]()
      redisClientActor ! Operation(Ping, promise2)
      awaitAssert({
        redisClientActor.underlyingActor.queuePromises.length mustEqual 1
        redisClientActor.underlyingActor.onWriteSent()
        redisClientActor.underlyingActor.queuePromises must beEmpty
      }, timeout)

      EventFilter[Exception](occurrences = 1, start = "Redis Protocol error: Got 110 as initial reply byte").intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("not valid redis reply"))
      })
      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"
      Await.result(promise2.future, timeout) must throwA(InvalidRedisReply)
    }

    "no more operations" in within(timeout){
      val probeMock = TestProbe()

      val redisClientActor = TestActorRef[RedisClientActorMock2](Props(classOf[RedisClientActorMock2], probeMock.ref).withDispatcher(Redis.dispatcher))
      val promise = Promise[String]()
      redisClientActor ! Operation(Ping, promise)

      awaitAssert({
        redisClientActor.underlyingActor.queuePromises.length mustEqual 1
      }, timeout)

      EventFilter[NoSuchElementException](occurrences = 1).intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("+PONG\r\n"))
      })
      awaitAssert({
        redisClientActor.underlyingActor.queuePromises.length mustEqual 1
      }, timeout)

      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"

      EventFilter[NoSuchElementException](occurrences = 1).intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("+PONG\r\n"))
      })
      awaitAssert({
        redisClientActor.underlyingActor.queuePromises.length mustEqual 1
      }, timeout)

      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"

    }

    "redis reply in many chunks" in within(timeout){
      val promise1 = Promise[String]()
      val promise2 = Promise[String]()
      val operation1 = Operation(Ping, promise1)
      val operation2 = Operation(Ping, promise2)
      val q = QueuePromises(mutable.Queue[Operation[_, _]]())
      q.queue.enqueue(operation1)
      q.queue.enqueue(operation2)

      val redisReplyDecoder = TestActorRef[RedisReplyDecoder](Props(classOf[RedisReplyDecoder]).withDispatcher(Redis.dispatcher))

      redisReplyDecoder.underlyingActor.queuePromises must beEmpty

      redisReplyDecoder ! q
      awaitAssert({
        redisReplyDecoder.underlyingActor.queuePromises.size mustEqual 2
      }, timeout)

      redisReplyDecoder ! ByteString("+P")
      awaitAssert(redisReplyDecoder.underlyingActor.bufferRead == ByteString("+P"))

      redisReplyDecoder ! ByteString("ONG\r")
      awaitAssert(redisReplyDecoder.underlyingActor.bufferRead == ByteString("+PONG\r"))

      redisReplyDecoder ! ByteString("\n+PONG2")
      awaitAssert(redisReplyDecoder.underlyingActor.bufferRead == ByteString("+PONG2"))

      Await.result(promise1.future, timeout) mustEqual "PONG"

      redisReplyDecoder ! ByteString("\r\n")
      awaitAssert(redisReplyDecoder.underlyingActor.bufferRead.isEmpty)

      Await.result(promise2.future, timeout) mustEqual "PONG2"

      redisReplyDecoder.underlyingActor.queuePromises must beEmpty
    }
  }
}

class RedisClientActorMock2(probeMock: ActorRef)
  extends RedisClientActor(new InetSocketAddress("localhost", 6379), () => {Seq()}) {
  override def preStart() {
    // disable preStart of RedisWorkerIO
  }

  override def restartConnection() {
    super.restartConnection()
    probeMock ! "restartConnection"
  }
}
