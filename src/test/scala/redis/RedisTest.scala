package redis

import scala.concurrent._
import akka.util.ByteString

class RedisTest extends RedisSpec {

  sequential

  "basic test" should {
    "ping" in {

      val r = for {
        i <- 0 to 5000
      } yield {
        Thread.sleep(10)
        redis.ping()
      }

      Await.result(Future.sequence(r), timeOut)

      Await.result(redis.ping, timeOut) mustEqual "PONG"
    }
  }

}
