package redis

import scala.concurrent.Await
import redis.api.pubsub._
import redis.actors.RedisSubscriberActor
import java.net.InetSocketAddress
import akka.actor.{Props, ActorRef}
import akka.testkit.{TestActorRef, TestProbe}

class RedisPubSubSpec extends RedisSpec {

  sequential

  "PubSub test" should {
    "ok (client + callback)" in {

      var redisPubSub: RedisPubSub = null

      redisPubSub = RedisPubSub(
        channels = Seq("chan1", "secondChannel"),
        patterns = Seq("chan*"),
        onMessage = (m: Message) => {
          redisPubSub.unsubscribe("chan1", "secondChannel")
          redisPubSub.punsubscribe("chan*")
          redisPubSub.subscribe(m.data)
          redisPubSub.psubscribe("next*")
        }
      )

      Thread.sleep(2000)

      val p = redis.publish("chan1", "nextChan")
      val noListener = redis.publish("noListenerChan", "message")
      Await.result(p, timeOut) mustEqual 2
      Await.result(noListener, timeOut) mustEqual 0

      Thread.sleep(2000)
      val nextChan = redis.publish("nextChan", "message")
      val p2 = redis.publish("chan1", "nextChan")
      Await.result(p2, timeOut) mustEqual 0
      Await.result(nextChan, timeOut) mustEqual 2
    }

    "ok (actor)" in {
      val probeMock = TestProbe()
      val channels = Seq("channel")
      val patterns = Seq("pattern.*")

      val subscriberActor = TestActorRef[SubscriberActor](
        Props(classOf[SubscriberActor], new InetSocketAddress("localhost", 6379),
              channels, patterns, probeMock.ref)
          .withDispatcher("rediscala.rediscala-client-worker-dispatcher"),
        "SubscriberActor"
      )
      import scala.concurrent.duration._

      system.scheduler.scheduleOnce(2 seconds)(redis.publish("channel", "value"))

      probeMock.expectMsgType[Message](5 seconds) mustEqual Message("channel", "value")

      redis.publish("pattern.1", "value")

      probeMock.expectMsgType[PMessage] mustEqual PMessage("pattern.*", "pattern.1", "value")

      subscriberActor.underlyingActor.subscribe("channel2")
      subscriberActor.underlyingActor.unsubscribe("channel")

      system.scheduler.scheduleOnce(2 seconds)({
        redis.publish("channel", "value")
        redis.publish("channel2", "value")
      })
      probeMock.expectMsgType[Message](5 seconds) mustEqual Message("channel2", "value")


      subscriberActor.underlyingActor.psubscribe("pattern2.*")
      subscriberActor.underlyingActor.punsubscribe("pattern.*")

      system.scheduler.scheduleOnce(2 seconds)({
        redis.publish("pattern2.match", "value")
        redis.publish("pattern.*", "value")
      })
      probeMock.expectMsgType[PMessage](5 seconds) mustEqual PMessage("pattern2.*", "pattern2.match", "value")
    }
  }

}

class SubscriberActor( override val address: InetSocketAddress,
                       channels: Seq[String],
                       patterns: Seq[String],
                       probeMock: ActorRef
                       ) extends RedisSubscriberActor(channels, patterns) {

  override def onMessage(m: Message) = {
    probeMock ! m
  }

  def onPMessage(pm: PMessage) {
    probeMock ! pm
  }
}


