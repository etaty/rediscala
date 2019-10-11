package redis.commands

import redis._
import scala.concurrent.Await
import akka.util.ByteString
import scala.concurrent.duration._

class BSortedSetsSpec extends RedisStandaloneServer {

  "Blocking Sorted Sets commands" should {
    "BZPOPMAX" in {
      "already containing elements" in {
        val redisB = RedisBlockingClient(port=port)
        val r = for {
          _ <- redis.del("bzpopmax1", "bzpopmax2")
          p <- redis.zadd("bzpopmax1", (0, "a"), (1, "b"))
          b <- redisB.bzpopmax(Seq("bzpopmax1", "bzpopmax2"))
        } yield {
          b mustEqual Some(("bzpopmax1", ByteString("b"), 1))
        }
        val rr = Await.result(r, timeOut)
        redisB.stop()
        rr
      }

      "blocking" in {
        val redisB = RedisBlockingClient(port=port)
        val rr = within(1.seconds, 10.seconds) {
          val r = redis.del("bzpopmaxBlock").flatMap(_ => {
            val bzpopmax = redisB.bzpopmax(Seq("bzpopmaxBlock"))
            Thread.sleep(1000)
            redis.zadd("bzpopmaxBlock", (0, "a"), (1, "b"))
            bzpopmax
          })
          Await.result(r, timeOut) mustEqual Some(("bzpopmaxBlock", ByteString("b"), 1))
        }
        redisB.stop()
        rr
      }

      "blocking timeout" in {
        val redisB = RedisBlockingClient(port=port)
        val rr = within(1.seconds, 10.seconds) {
          val r = redis.del("bzpopmaxBlockTimeout").flatMap(_ => {
            redisB.bzpopmax(Seq("bzpopmaxBlockTimeout"), 1.seconds)
          })
          Await.result(r, timeOut) must beNone
        }
        redisB.stop()
        rr
      }
    }
    
    "BZPOPMIN" in {
      "already containing elements" in {
        val redisB = RedisBlockingClient(port=port)
        val r = for {
          _ <- redis.del("bzpopmin1", "bzpopmin2")
          p <- redis.zadd("bzpopmin1", (0, "a"), (1, "b"))
          b <- redisB.bzpopmin(Seq("bzpopmin1", "bzpopmin2"))
        } yield {
          b mustEqual Some(("bzpopmin1", ByteString("a"), 0))
        }
        val rr = Await.result(r, timeOut)
        redisB.stop()
        rr
      }

      "blocking" in {
        val redisB = RedisBlockingClient(port=port)
        val rr = within(1.seconds, 10.seconds) {
          val r = redis.del("bzpopminBlock").flatMap(_ => {
            val bzpopmin = redisB.bzpopmin(Seq("bzpopminBlock"))
            Thread.sleep(1000)
            redis.zadd("bzpopminBlock", (0, "a"), (1, "b"))
            bzpopmin
          })
          Await.result(r, timeOut) mustEqual Some(("bzpopminBlock", ByteString("a"), 0))
        }
        redisB.stop()
        rr
      }

      "blocking timeout" in {
        val redisB = RedisBlockingClient(port=port)
        val rr = within(1.seconds, 10.seconds) {
          val r = redis.del("bzpopminBlockTimeout").flatMap(_ => {
            redisB.bzpopmin(Seq("bzpopminBlockTimeout"), 1.seconds)
          })
          Await.result(r, timeOut) must beNone
        }
        redisB.stop()
        rr
      }
    }
  }
}
