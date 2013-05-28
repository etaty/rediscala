package redis.commands

import redis._
import scala.concurrent.Await
import akka.util.ByteString
import scala.util.Success
import scala.concurrent.duration._

class BListsSpec extends RedisSpec {

  import Converter._

  "Blocking Lists commands" should {
    "BLPOP" in {
      "already containing elements" in {
        val redisB = RedisBlockingClient()
        val r = for {
          _ <- redis.del("blpop1", "blpop2")
          p <- redis.rpush("blpop1", "a", "b", "c")
          b <- redisB.blpop(Seq("blpop1", "blpop2"))
        } yield {
          b mustEqual Success(Some("blpop1", ByteString("a")))
        }
        Await.result(r, timeOut)
        redisB.disconnect()
      }

      "blocking" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 4.seconds) {
          val r = redis.del("blpopBlock").flatMap(_ => {
            val blpop = redisB.blpop(Seq("blpopBlock"))
            Thread.sleep(1000)
            redis.rpush("blpopBlock", "a", "b", "c")
            blpop
          })
          Await.result(r, timeOut) mustEqual Success(Some("blpopBlock", ByteString("a")))
          redisB.disconnect()
        }
      }

      "blocking timeout" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 4.seconds) {
          val r = redis.del("blpopBlockTimeout").flatMap(_ => {
            redisB.brpop(Seq("blpopBlockTimeout"), 1.seconds)
          })
          Await.result(r, timeOut) mustEqual Success(None)
          redisB.disconnect()
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
          b mustEqual Success(Some("brpop1", ByteString("c")))
          redisB.disconnect()
        }
        Await.result(r, timeOut)
        redisB.disconnect()
      }

      "blocking" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 4.seconds) {
          val r = redis.del("brpopBlock").flatMap(_ => {
            val brpop = redisB.brpop(Seq("brpopBlock"))
            Thread.sleep(1000)
            redis.rpush("brpopBlock", "a", "b", "c")
            brpop
          })
          Await.result(r, timeOut) mustEqual Success(Some("brpopBlock", ByteString("c")))
          redisB.disconnect()
        }
      }

      "blocking timeout" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 4.seconds) {
          val r = redis.del("brpopBlockTimeout").flatMap(_ => {
            redisB.brpop(Seq("brpopBlockTimeout"), 1.seconds)
          })
          Await.result(r, timeOut) mustEqual Success(None)
          redisB.disconnect()
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
        Await.result(r, timeOut)
        redisB.disconnect()
      }

      "blocking" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 4.seconds) {
          val r = redis.del("brpopplushBlock1", "brpopplushBlock2").flatMap(_ => {
            val brpopplush = redisB.brpopplush("brpopplushBlock1", "brpopplushBlock2")
            Thread.sleep(1000)
            redis.rpush("brpopplushBlock1", "a", "b", "c")
            brpopplush
          })
          Await.result(r, timeOut) mustEqual Some(ByteString("c"))
          redisB.disconnect()
        }
      }

      "blocking timeout" in {
        val redisB = RedisBlockingClient()
        within(1.seconds, 4.seconds) {
          val r = redis.del("brpopplushBlockTimeout").flatMap(_ => {
            redisB.brpopplush("brpopplushBlockTimeout1", "brpopplushBlockTimeout2", 1.seconds)
          })
          Await.result(r, timeOut) mustEqual None
          redisB.disconnect()
        }
      }
    }
  }
}
