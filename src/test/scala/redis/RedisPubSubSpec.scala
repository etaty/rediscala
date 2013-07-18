package redis

import scala.concurrent.Await
import akka.actor.Props
import java.net.InetSocketAddress
import redis.actors.{PMessage, Message, Subscriber}

class RedisPubSubSpec extends RedisSpec {

  sequential

  "PubSub test" should {
    "PubSub" in {

      val host = "localhost"
      val port = 6379
      val channels = Seq("chan1", "secondChannel")
      val patterns = Seq("chan*")

      system.actorOf(Props(classOf[SubscribeActor], new InetSocketAddress(host, port), channels, patterns).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

      Thread.sleep(2000)

      val p = redis.publish("chan1", "message")
      val noListener = redis.publish("noListenerChan", "message")
      Await.result(p, timeOut) mustEqual 2
      Await.result(noListener, timeOut) mustEqual 0
      // todo finish tests
    }
  }

}

class SubscribeActor(addr: InetSocketAddress, channels: Seq[String] = Nil, patterns: Seq[String] = Nil) extends Subscriber {
  override def onMessage(message: Message) {

  }

  override def onPMessage(pmessage: PMessage) {

  }

  def address: InetSocketAddress = addr

  def subscribedChannels: Seq[String] = channels

  def subscribedPatterns: Seq[String] = patterns
}


