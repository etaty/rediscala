package redis.actors

import akka.testkit._
import akka.actor._
import org.specs2.mutable.{Tags, SpecificationLike}
import org.specs2.time.NoTimeConversions
import akka.util.ByteString
import scala.concurrent.{Await, Promise}
import redis.protocol.{RedisProtocolRequest, RedisReply}
import scala.collection.mutable
import redis.{Operation, protocol}
import java.net.InetSocketAddress
import com.typesafe.config.ConfigFactory
import redis.Operation

class RedisReplyDecoderSpec
  extends TestKit(ActorSystem("testsystem", ConfigFactory.parseString( """akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with SpecificationLike with Tags with NoTimeConversions with ImplicitSender {

  import scala.concurrent.duration._

  sequential

  "RedisReplyDecoder" should {

    "ok" in {
      val promise = Promise[RedisReply]()
      val q = mutable.Queue[Promise[RedisReply]]()
      q.enqueue(promise)

      val redisReplyDecoder = TestActorRef[RedisReplyDecoder](Props(classOf[RedisReplyDecoder]).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

      redisReplyDecoder.underlyingActor.queuePromises must beEmpty

      redisReplyDecoder ! q
      redisReplyDecoder.underlyingActor.queuePromises.size mustEqual 1

      redisReplyDecoder ! ByteString("+PONG\r\n")
      Await.result(promise.future, 10 seconds) mustEqual protocol.Status(ByteString("PONG"))
      redisReplyDecoder.underlyingActor.queuePromises must beEmpty

      val promise2 = Promise[RedisReply]()
      val promise3 = Promise[RedisReply]()
      val q2 = mutable.Queue[Promise[RedisReply]]()
      q2.enqueue(promise2)
      q2.enqueue(promise3)

      redisReplyDecoder ! q2
      redisReplyDecoder.underlyingActor.queuePromises.size mustEqual 2

      redisReplyDecoder ! ByteString("+PONG\r\n+PONG\r\n")
      Await.result(promise2.future, 10 seconds) mustEqual protocol.Status(ByteString("PONG"))
      Await.result(promise3.future, 10 seconds) mustEqual protocol.Status(ByteString("PONG"))
      redisReplyDecoder.underlyingActor.queuePromises must beEmpty
    }

    "can't decode" in {
      val probeMock = TestProbe()

      val redisClientActor = TestActorRef[RedisClientActorMock2](Props(classOf[RedisClientActorMock2], probeMock.ref).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))
      val promise = Promise[RedisReply]()
      redisClientActor ! Operation(RedisProtocolRequest.inline("PING"), promise)
      redisClientActor.underlyingActor.queuePromises.length mustEqual 1
      redisClientActor.underlyingActor.onWriteSent()
      redisClientActor.underlyingActor.queuePromises must beEmpty

      EventFilter[Exception](occurrences = 1, message = "Redis Protocol error: Got 110 as initial reply byte").intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("not valid redis reply"))
      })
      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"
      Await.result(promise.future, 10 seconds) must throwA(InvalidRedisReply)


      val promise2 = Promise[RedisReply]()
      redisClientActor ! Operation(RedisProtocolRequest.inline("PING"), promise2)
      redisClientActor.underlyingActor.queuePromises.length mustEqual 1
      redisClientActor.underlyingActor.onWriteSent()
      redisClientActor.underlyingActor.queuePromises must beEmpty

      EventFilter[Exception](occurrences = 1, message = "Redis Protocol error: Got 110 as initial reply byte").intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("not valid redis reply"))
      })
      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"
      Await.result(promise2.future, 10 seconds) must throwA(InvalidRedisReply)
    }

    "no more promises" in {
      val probeMock = TestProbe()

      val redisClientActor = TestActorRef[RedisClientActorMock2](Props(classOf[RedisClientActorMock2], probeMock.ref).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))
      val promise = Promise[RedisReply]()
      redisClientActor ! Operation(RedisProtocolRequest.inline("PING"), promise)

      redisClientActor.underlyingActor.queuePromises.length mustEqual 1

      EventFilter[Exception](occurrences = 1, message = "queue empty").intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("+PONG\r\n"))
      })
      redisClientActor.underlyingActor.queuePromises.length mustEqual 1
      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"


      EventFilter[Exception](occurrences = 1, message = "queue empty").intercept({
        redisClientActor.underlyingActor.onDataReceived(ByteString("+PONG\r\n"))
      })
      redisClientActor.underlyingActor.queuePromises.length mustEqual 1
      probeMock.expectMsg("restartConnection") mustEqual "restartConnection"

    }
  }
}

class RedisClientActorMock2(probeMock: ActorRef) extends RedisClientActor(new InetSocketAddress("localhost", 6379)) {
  override def preStart() {
    // disable preStart of RedisWorkerIO
  }

  override def restartConnection() {
    super.restartConnection()
    probeMock ! "restartConnection"
  }
}