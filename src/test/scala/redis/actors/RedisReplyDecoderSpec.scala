package redis.actors

import akka.actor._
import org.specs2.mutable.SpecificationLike
import akka.util.ByteString
import redis.api.hashes.Hgetall
import redis.protocol.MultiBulk
import scala.concurrent.{Await, Promise}
import scala.collection.mutable
import java.net.InetSocketAddress
import com.typesafe.config.ConfigFactory
import redis.{Redis, Operation}
import redis.api.connection.Ping
import akka.testkit._

class RedisReplyDecoderSpec
  extends TestKit(ActorSystem("testsystem", ConfigFactory.parseString( """akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with SpecificationLike with ImplicitSender {

  import scala.concurrent.duration._

  sequential

  val timeout = 5.seconds dilated

  "RedisReplyDecoder" should {
    "ok" in within(timeout){
      val promise = Promise[String]()
      val operation = Operation(Ping, promise)
      val q = QueuePromises(mutable.Queue[Operation[_, _]]())
      q.queue.enqueue(operation)

      val redisReplyDecoder = TestActorRef[RedisReplyDecoder](Props(classOf[RedisReplyDecoder])
          .withDispatcher(Redis.dispatcher.name))

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

      val redisClientActor = TestActorRef[RedisClientActorMock2](
        Props(classOf[RedisClientActorMock2], probeMock.ref).withDispatcher(Redis.dispatcher.name))
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

    "redis reply in many chunks" in within(timeout){
      val promise1 = Promise[String]()
      val promise2 = Promise[String]()
      val promise3 = Promise[Map[String, String]]()
      val operation1 = Operation(Ping, promise1)
      val operation2 = Operation(Ping, promise2)
      val operation3 = Operation[MultiBulk, Map[String, String]](Hgetall[String, String]("key"), promise3)
      val q = QueuePromises(mutable.Queue[Operation[_, _]]())
      q.queue.enqueue(operation1)
      q.queue.enqueue(operation2)
      q.queue.enqueue(operation3)

      val redisReplyDecoder = TestActorRef[RedisReplyDecoder](Props(classOf[RedisReplyDecoder])
          .withDispatcher(Redis.dispatcher.name))

      redisReplyDecoder.underlyingActor.queuePromises must beEmpty

      redisReplyDecoder ! q
      awaitAssert({
        redisReplyDecoder.underlyingActor.queuePromises.size mustEqual 3
      }, timeout)

      redisReplyDecoder ! ByteString("+P")
      awaitAssert(redisReplyDecoder.underlyingActor.partiallyDecoded.rest == ByteString("+P"))
      awaitAssert(redisReplyDecoder.underlyingActor.partiallyDecoded.isFullyDecoded should beFalse)

      redisReplyDecoder ! ByteString("ONG\r")
      awaitAssert(redisReplyDecoder.underlyingActor.partiallyDecoded.rest == ByteString("+PONG\r"))

      redisReplyDecoder ! ByteString("\n+PONG2")
      awaitAssert(redisReplyDecoder.underlyingActor.partiallyDecoded.rest == ByteString("+PONG2"))

      Await.result(promise1.future, timeout) mustEqual "PONG"

      redisReplyDecoder ! ByteString("\r\n")
      awaitAssert(redisReplyDecoder.underlyingActor.partiallyDecoded.isFullyDecoded must beTrue)
      awaitAssert(redisReplyDecoder.underlyingActor.partiallyDecoded.rest.isEmpty must beTrue)

      Await.result(promise2.future, timeout) mustEqual "PONG2"

      awaitAssert(redisReplyDecoder.underlyingActor.queuePromises.size mustEqual 1)

      val multibulkString0 = ByteString()
      val multibulkString = ByteString("*4\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n$5\r\nWorld\r\n")
      val (multibulkStringStart, multibulkStringEnd) = multibulkString.splitAt(multibulkString.length - 1)

      redisReplyDecoder ! multibulkString0
      awaitAssert(redisReplyDecoder.underlyingActor.partiallyDecoded.isFullyDecoded must beTrue, timeout)

      for {
        b <- multibulkStringStart
      } yield {
        redisReplyDecoder ! ByteString(b)
        awaitAssert(redisReplyDecoder.underlyingActor.partiallyDecoded.isFullyDecoded must beFalse, timeout)
      }
      redisReplyDecoder ! multibulkStringEnd

      awaitAssert(redisReplyDecoder.underlyingActor.queuePromises.size mustEqual 0)
      awaitAssert(redisReplyDecoder.underlyingActor.partiallyDecoded.isFullyDecoded must beTrue, timeout)

      Await.result(promise3.future, timeout) mustEqual Map("foo" -> "bar", "Hello" -> "World")

      redisReplyDecoder.underlyingActor.queuePromises must beEmpty
    }
  }
}

class RedisClientActorMock2(probeMock: ActorRef)
  extends RedisClientActor(new InetSocketAddress("localhost", 6379), () => {Seq()}, (status:Boolean) => {()}, Redis.dispatcher.name) {
  override def preStart(): Unit = {
    // disable preStart of RedisWorkerIO
  }

  override def restartConnection(): Unit = {
    super.restartConnection()
    probeMock ! "restartConnection"
  }
}
