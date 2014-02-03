package redis.actors

import akka.testkit._
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
import scala.concurrent.duration.DurationInt

class RedisReplyDecoderSpec
  extends TestKit(ActorSystem("testsystem", ConfigFactory.parseString( """akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with SpecificationLike with Tags with NoTimeConversions with ImplicitSender {

  import scala.concurrent.duration._

  sequential

  "RedisReplyDecoder" should {

    "ok" in {
      val promise = Promise[String]()
      val operation = Operation(Ping, promise)
      val q = mutable.Queue[Operation[_, _]]()
      q.enqueue(operation)

      val redisReplyDecoder = TestActorRef[RedisReplyDecoder](Props(classOf[RedisReplyDecoder]).withDispatcher(Redis.dispatcher))

      redisReplyDecoder.underlyingActor.queuePromises must beEmpty

      redisReplyDecoder ! q
      awaitCond({
        redisReplyDecoder.underlyingActor.queuePromises.size mustEqual 1
      }, 1 seconds)

      redisReplyDecoder ! ByteString("+PONG\r\n")
      Await.result(promise.future, 10 seconds) mustEqual "PONG"
      redisReplyDecoder.underlyingActor.queuePromises must beEmpty

      val promise2 = Promise[String]()
      val promise3 = Promise[String]()
      val op2 = Operation(Ping, promise2)
      val op3 = Operation(Ping, promise3)
      val q2 = mutable.Queue[Operation[_, _]]()
      q2.enqueue(op2)
      q2.enqueue(op3)

      redisReplyDecoder ! q2
      awaitCond({
        redisReplyDecoder.underlyingActor.queuePromises.size mustEqual 2
      }, 1 seconds)

      redisReplyDecoder ! ByteString("+PONG\r\n+PONG\r\n")
      Await.result(promise2.future, 10 seconds) mustEqual "PONG"
      Await.result(promise3.future, 10 seconds) mustEqual "PONG"
      redisReplyDecoder.underlyingActor.queuePromises must beEmpty
    }

    "can't decode" in {
      val probeMock = TestProbe()

      val redisClientActor = TestActorRef[RedisClientActorMock2](Props(classOf[RedisClientActorMock2], probeMock.ref).withDispatcher(Redis.dispatcher))
      val promise = Promise[String]()
      redisClientActor ! Operation(Ping, promise)
      awaitCond({
        redisClientActor.underlyingActor.queuePromises.length mustEqual 1
        redisClientActor.underlyingActor.onWriteSent()
        redisClientActor.underlyingActor.queuePromises must beEmpty
      }, 1 seconds)

      EventFilter[Exception](occurrences = 1, start = "Redis Protocol error: Got 110 as initial reply byte").intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("not valid redis reply"))
      })
      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"
      Await.result(promise.future, 10 seconds) must throwA(InvalidRedisReply)


      val promise2 = Promise[String]()
      redisClientActor ! Operation(Ping, promise2)
      awaitCond({
        redisClientActor.underlyingActor.queuePromises.length mustEqual 1
        redisClientActor.underlyingActor.onWriteSent()
        redisClientActor.underlyingActor.queuePromises must beEmpty
      }, 1 seconds)

      EventFilter[Exception](occurrences = 1, start = "Redis Protocol error: Got 110 as initial reply byte").intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("not valid redis reply"))
      })
      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"
      Await.result(promise2.future, 10 seconds) must throwA(InvalidRedisReply)
    }

    "no more operations" in {
      val probeMock = TestProbe()

      val redisClientActor = TestActorRef[RedisClientActorMock2](Props(classOf[RedisClientActorMock2], probeMock.ref).withDispatcher(Redis.dispatcher))
      val promise = Promise[String]()
      redisClientActor ! Operation(Ping, promise)

      awaitCond({
        redisClientActor.underlyingActor.queuePromises.length mustEqual 1
      }, 1 seconds)

      EventFilter[NoSuchElementException](occurrences = 1).intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("+PONG\r\n"))
      })
      awaitCond({
        redisClientActor.underlyingActor.queuePromises.length mustEqual 1
      }, 1 seconds)

      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"

      EventFilter[NoSuchElementException](occurrences = 1).intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("+PONG\r\n"))
      })
      awaitCond({
        redisClientActor.underlyingActor.queuePromises.length mustEqual 1
      }, 1 seconds)

      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"

    }

    "redis reply in many chunks" in {
      val promise1 = Promise[String]()
      val promise2 = Promise[String]()
      val operation1 = Operation(Ping, promise1)
      val operation2 = Operation(Ping, promise2)
      val q = mutable.Queue[Operation[_, _]]()
      q.enqueue(operation1)
      q.enqueue(operation2)

      val redisReplyDecoder = TestActorRef[RedisReplyDecoder](Props(classOf[RedisReplyDecoder]).withDispatcher(Redis.dispatcher))

      redisReplyDecoder.underlyingActor.queuePromises must beEmpty

      redisReplyDecoder ! q
      awaitCond({
        redisReplyDecoder.underlyingActor.queuePromises.size mustEqual 2
      }, 3 seconds)

      redisReplyDecoder ! ByteString("+P")
      awaitCond({
        redisReplyDecoder.underlyingActor.bufferRead == ByteString("+P")
      }, 3 seconds)

      redisReplyDecoder ! ByteString("ONG\r")
      awaitCond({
        redisReplyDecoder.underlyingActor.bufferRead == ByteString("+PONG\r")
      }, 3 seconds)

      redisReplyDecoder ! ByteString("\n+PONG2")
      awaitCond({
        redisReplyDecoder.underlyingActor.bufferRead == ByteString("+PONG2")
      }, 3 seconds)

      Await.result(promise1.future, 3 seconds) mustEqual "PONG"

      redisReplyDecoder ! ByteString("\r\n")
      awaitCond({
        redisReplyDecoder.underlyingActor.bufferRead.isEmpty
      }, 3 seconds)

      Await.result(promise2.future, 3 seconds) mustEqual "PONG2"

      redisReplyDecoder.underlyingActor.queuePromises must beEmpty
    }
  }
}

class RedisClientActorMock2(probeMock: ActorRef)
  extends RedisClientActor(new InetSocketAddress("localhost", 6379), 2 seconds, () => {Seq()}) {
  override def preStart() {
    // disable preStart of RedisWorkerIO
  }

  override def restartConnection() {
    super.restartConnection()
    probeMock ! "restartConnection"
  }
}