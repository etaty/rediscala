package redis.commands

import redis._
import scala.concurrent.Await
import org.apache.pekko.util.ByteString
import scala.concurrent.duration._

class BListsSpec extends RedisStandaloneServer {

  "Blocking Lists commands" should {
    "BLPOP" in {
      "already containing elements" in {
        val redisB = RedisBlockingClient(port=port)
        val r = for {
          _ <- redis.del("blpop1", "blpop2")
          p <- redis.rpush("blpop1", "a", "b", "c")
          b <- redisB.blpop(Seq("blpop1", "blpop2"))
        } yield {
          b mustEqual Some("blpop1" -> ByteString("a"))
        }
        val rr = Await.result(r, timeOut)
        redisB.stop()
        rr
      }

      "blocking" in {
        val redisB = RedisBlockingClient(port=port)
        val rr = within(1.seconds, 10.seconds) {
          val r = redis.del("blpopBlock").flatMap(_ => {
            val blpop = redisB.blpop(Seq("blpopBlock"))
            Thread.sleep(1000)
            redis.rpush("blpopBlock", "a", "b", "c")
            blpop
          })
          Await.result(r, timeOut) mustEqual Some("blpopBlock" -> ByteString("a"))
        }
        redisB.stop()
        rr
      }

      "blocking timeout" in {
        val redisB = RedisBlockingClient(port=port)
        val rr = within(1.seconds, 10.seconds) {
          val r = redis.del("blpopBlockTimeout").flatMap(_ => {
            redisB.brpop(Seq("blpopBlockTimeout"), 1.seconds)
          })
          Await.result(r, timeOut) must beNone
        }
        redisB.stop()
        rr
      }
    }

    "BRPOP" in {
      "already containing elements" in {
        val redisB = RedisBlockingClient(port=port)
        val r = for {
          _ <- redis.del("brpop1", "brpop2")
          p <- redis.rpush("brpop1", "a", "b", "c")
          b <- redisB.brpop(Seq("brpop1", "brpop2"))
        } yield {
          redisB.stop()
          b mustEqual Some("brpop1" -> ByteString("c"))
        }
        Await.result(r, timeOut)
      }

      "blocking" in {
        val redisB = RedisBlockingClient(port=port)
        val rr = within(1.seconds, 10.seconds) {
          val r = redis.del("brpopBlock").flatMap(_ => {
            val brpop = redisB.brpop(Seq("brpopBlock"))
            Thread.sleep(1000)
            redis.rpush("brpopBlock", "a", "b", "c")
            brpop
          })
          Await.result(r, timeOut) mustEqual Some("brpopBlock" -> ByteString("c"))
        }
        redisB.stop()
        rr
      }

      "blocking timeout" in {
        val redisB = RedisBlockingClient(port=port)
        val rr = within(1.seconds, 10.seconds) {
          val r = redis.del("brpopBlockTimeout").flatMap(_ => {
            redisB.brpop(Seq("brpopBlockTimeout"), 1.seconds)
          })
          Await.result(r, timeOut) must beNone
        }
        redisB.stop()
        rr
      }
    }

    "BRPOPLPUSH" in {
      "already containing elements" in {
        val redisB = RedisBlockingClient(port=port)
        val r = for {
          _ <- redis.del("brpopplush1", "brpopplush2")
          p <- redis.rpush("brpopplush1", "a", "b", "c")
          b <- redisB.brpoplpush("brpopplush1", "brpopplush2")
        } yield {
          b mustEqual Some(ByteString("c"))
        }
        val rr = Await.result(r, timeOut)
        redisB.stop()
        rr
      }

      "blocking" in {
        val redisB = RedisBlockingClient(port=port)
        val rr = within(1.seconds, 10.seconds) {
          val r = redis.del("brpopplushBlock1", "brpopplushBlock2").flatMap(_ => {
            val brpopplush = redisB.brpoplpush("brpopplushBlock1", "brpopplushBlock2")
            Thread.sleep(1000)
            redis.rpush("brpopplushBlock1", "a", "b", "c")
            brpopplush
          })
          Await.result(r, timeOut) mustEqual Some(ByteString("c"))
        }
        redisB.stop()
        rr
      }

      "blocking timeout" in {
        val redisB = RedisBlockingClient(port=port)
        val rr = within(1.seconds, 10.seconds) {
          val r = redis.del("brpopplushBlockTimeout1", "brpopplushBlockTimeout2").flatMap(_ => {
            redisB.brpoplpush("brpopplushBlockTimeout1", "brpopplushBlockTimeout2", 1.seconds)
          })
          Await.result(r, timeOut) must beNone
        }
        redisB.stop()
        rr
      }
    }
  }
}
