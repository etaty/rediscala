package redis.commands

import redis._
import scala.concurrent.Await
import akka.util.ByteString
import scala.concurrent.duration._

class BListsSpec extends RedisSpec {

  "Blocking Lists commands" should {
    "BLPOP" in {
      "already containing elements" in {
        val redisB = RedisBlockingClient()
        val r = for {
          _ <- redis.del("blpop1", "blpop2")
          p <- redis.rpush("blpop1", "a", "b", "c")
          b <- redisB.blpop(Seq("blpop1", "blpop2"))
        } yield {
          b mustEqual Some("blpop1", ByteString("a"))
        }
        val rr = Await.result(r, timeOut)
        redisB.disconnect()
        rr
      }

      "blocking" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 10.seconds) {
          val r = redis.del("blpopBlock").flatMap(_ => {
            val blpop = redisB.blpop(Seq("blpopBlock"))
            Thread.sleep(1000)
            redis.rpush("blpopBlock", "a", "b", "c")
            blpop
          })
          val rr = Await.result(r, timeOut) mustEqual Some("blpopBlock", ByteString("a"))
          redisB.disconnect()
          rr
        }
      }

      "blocking timeout" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 10.seconds) {
          val r = redis.del("blpopBlockTimeout").flatMap(_ => {
            redisB.brpop(Seq("blpopBlockTimeout"), 1.seconds)
          })
          val rr = Await.result(r, timeOut) must beNone
          redisB.disconnect()
          rr
        }
      }
    }

    "BRPOP" in {
      "already containing elements" in {
        val redisB = RedisBlockingClient()
        val r = for {
          _ <- redis.del("brpop1", "brpop2")
          p <- redis.rpush("brpop1", "a", "b", "c")
          b <- redisB.brpop(Seq("brpop1", "brpop2"))
        } yield {
          redisB.disconnect()
          b mustEqual Some("brpop1", ByteString("c"))
        }
        Await.result(r, timeOut)
      }

      "blocking" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 10.seconds) {
          val r = redis.del("brpopBlock").flatMap(_ => {
            val brpop = redisB.brpop(Seq("brpopBlock"))
            Thread.sleep(1000)
            redis.rpush("brpopBlock", "a", "b", "c")
            brpop
          })
          val rr = Await.result(r, timeOut) mustEqual Some("brpopBlock", ByteString("c"))
          redisB.disconnect()
          rr
        }
      }

      "blocking timeout" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 10.seconds) {
          val r = redis.del("brpopBlockTimeout").flatMap(_ => {
            redisB.brpop(Seq("brpopBlockTimeout"), 1.seconds)
          })
          val rr = Await.result(r, timeOut) must beNone
          redisB.disconnect()
          rr
        }
      }
    }

    "BRPOPLPUSH" in {
      "already containing elements" in {
        val redisB = RedisBlockingClient()
        val r = for {
          _ <- redis.del("brpopplush1", "brpopplush2")
          p <- redis.rpush("brpopplush1", "a", "b", "c")
          b <- redisB.brpopplush("brpopplush1", "brpopplush2")
        } yield {
          b mustEqual Some(ByteString("c"))
        }
        val rr = Await.result(r, timeOut)
        redisB.disconnect()
        rr
      }

      "blocking" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 10.seconds) {
          val r = redis.del("brpopplushBlock1", "brpopplushBlock2").flatMap(_ => {
            val brpopplush = redisB.brpopplush("brpopplushBlock1", "brpopplushBlock2")
            Thread.sleep(1000)
            redis.rpush("brpopplushBlock1", "a", "b", "c")
            brpopplush
          })
          val rr = Await.result(r, timeOut) mustEqual Some(ByteString("c"))
          redisB.disconnect()
          rr
        }
      }

      "blocking timeout" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 10.seconds) {
          val r = redis.del("brpopplushBlockTimeout").flatMap(_ => {
            redisB.brpopplush("brpopplushBlockTimeout1", "brpopplushBlockTimeout2", 1.seconds)
          })
          val rr = Await.result(r, timeOut) must beNone
          redisB.disconnect()
          rr
        }
      }
    }
  }
}
