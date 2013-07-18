package redis.commands

import redis._
import redis.api._
import scala.concurrent.Await
import akka.util.ByteString

class SortedSetsSpec extends RedisSpec {

  "Sorted Sets commands" should {
    "ZADD" in {
      val r = for {
        _ <- redis.del("zaddKey")
        z1 <- redis.zadd("zaddKey", 1.0 -> "one", (1, "uno"), (2, "two"))
        z2 <- redis.zadd("zaddKey", (3, "two"))
        zr <- redis.zrangeWithscores("zaddKey", 0, -1)
      } yield {
        z1 mustEqual 3
        z2 mustEqual 0
        zr.get mustEqual Seq((ByteString("one"), 1), (ByteString("uno"), 1), (ByteString("two"), 3))
      }
      Await.result(r, timeOut)
    }

    "ZCARD" in {
      val r = for {
        _ <- redis.del("zcardKey")
        c1 <- redis.zcard("zcardKey")
        _ <- redis.zadd("zcardKey", 1.0 -> "one", (2, "two"))
        c2 <- redis.zcard("zcardKey")
      } yield {
        c1 mustEqual 0
        c2 mustEqual 2
      }
      Await.result(r, timeOut)
    }

    "ZCOUNT" in {
      val r = for {
        _ <- redis.del("zcountKey")
        c1 <- redis.zcount("zcountKey")
        _ <- redis.zadd("zcountKey", 1.0 -> "one", (2, "two"), (3, "three"))
        c2 <- redis.zcount("zcountKey")
        c3 <- redis.zcount("zcountKey", Limit(1, inclusive = false), Limit(3))
      } yield {
        c1 mustEqual 0
        c2 mustEqual 3
        c3 mustEqual 2
      }
      Await.result(r, timeOut)
    }

    "ZINCRBY" in {
      val r = for {
        _ <- redis.del("zincrbyKey")
        _ <- redis.zadd("zincrbyKey", 1.0 -> "one", (2, "two"))
        d <- redis.zincrby("zincrbyKey", 2.1, "one")
        d2 <- redis.zincrby("zincrbyKey", 2.1, "notexisting")
        zr <- redis.zrangeWithscores("zincrbyKey", 0, -1)
      } yield {
        d mustEqual 3.1
        d2 mustEqual 2.1
        zr.get mustEqual Seq((ByteString("two"), 2.0), (ByteString("notexisting"), 2.1), (ByteString("one"), 3.1))
      }
      Await.result(r, timeOut)
    }

    "ZINTERSTORE" in {
      val r = for {
        _ <- redis.del("zinterstoreKey1")
        _ <- redis.del("zinterstoreKey2")
        z1 <- redis.zadd("zinterstoreKey1", 1.0 -> "one", (2, "two"))
        z2 <- redis.zadd("zinterstoreKey2", 1.0 -> "one", (2, "two"), (3, "three"))
        i <- redis.zinterstoreWeighted("zinterstoreKeyOut", Seq(("zinterstoreKey1", 2), ("zinterstoreKey2", 3)))
        zr <- redis.zrangeWithscores("zinterstoreKeyOut", 0, -1)
      } yield {
        z1 mustEqual 2
        z2 mustEqual 3
        i mustEqual 2
        zr.get mustEqual Seq((ByteString("one"), 5), (ByteString("two"), 10))
      }
      Await.result(r, timeOut)
    }

    "ZRANGE" in {
      val r = for {
        _ <- redis.del("zrangeKey")
        z1 <- redis.zadd("zrangeKey", 1.0 -> "one", (2, "two"), (3, "three"))
        zr1 <- redis.zrange("zrangeKey", 0, -1)
        zr2 <- redis.zrange("zrangeKey", 2, 3)
        zr3 <- redis.zrange("zrangeKey", -2, -1)
      } yield {
        z1 mustEqual 3
        zr1.get mustEqual Seq(ByteString("one"), ByteString("two"), ByteString("three"))
        zr2.get mustEqual Seq(ByteString("three"))
        zr3.get mustEqual Seq(ByteString("two"), ByteString("three"))
      }
      Await.result(r, timeOut)
    }

    "ZRANGEBYSCORE" in {
      val r = for {
        _ <- redis.del("zrangebyscoreKey")
        z1 <- redis.zadd("zrangebyscoreKey", 1.0 -> "one", (2, "two"), (3, "three"))
        zr1 <- redis.zrangebyscore("zrangebyscoreKey", Limit(Double.NegativeInfinity), Limit(Double.PositiveInfinity))
        zr2 <- redis.zrangebyscore("zrangebyscoreKey", Limit(1), Limit(2))
        zr3 <- redis.zrangebyscore("zrangebyscoreKey", Limit(1, inclusive = false), Limit(2))
        zr4 <- redis.zrangebyscore("zrangebyscoreKey", Limit(1, inclusive = false), Limit(2, inclusive = false))
      } yield {
        z1 mustEqual 3
        zr1.get mustEqual Seq(ByteString("one"), ByteString("two"), ByteString("three"))
        zr2.get mustEqual Seq(ByteString("one"), ByteString("two"))
        zr3.get mustEqual Seq(ByteString("two"))
        zr4.get mustEqual Seq()
      }
      Await.result(r, timeOut)
    }

    "ZRANK" in {
      val r = for {
        _ <- redis.del("zrankKey")
        z1 <- redis.zadd("zrankKey", 1.0 -> "one", (2, "two"), (3, "three"))
        zr1 <- redis.zrank("zrankKey", "three")
        zr2 <- redis.zrank("zrankKey", "four")
      } yield {
        z1 mustEqual 3
        zr1.get mustEqual 2
        zr2 mustEqual None
      }
      Await.result(r, timeOut)
    }

    "ZREM" in {
      val r = for {
        _ <- redis.del("zremKey")
        z1 <- redis.zadd("zremKey", 1.0 -> "one", (2, "two"), (3, "three"))
        z2 <- redis.zrem("zremKey", "two", "nonexisting")
        zr <- redis.zrangeWithscores("zremKey", 0, -1)
      } yield {
        z1 mustEqual 3
        z2 mustEqual 1
        zr.get mustEqual Seq((ByteString("one"), 1), (ByteString("three"), 3))
      }
      Await.result(r, timeOut)
    }

    "ZREMRANGEBYRANK" in {
      val r = for {
        _ <- redis.del("zremrangebyrankKey")
        z1 <- redis.zadd("zremrangebyrankKey", 1.0 -> "one", (2, "two"), (3, "three"))
        z2 <- redis.zremrangebyrank("zremrangebyrankKey", 0, 1)
        zr <- redis.zrangeWithscores("zremrangebyrankKey", 0, -1)
      } yield {
        z1 mustEqual 3
        z2 mustEqual 2
        zr.get mustEqual Seq((ByteString("three"), 3))
      }
      Await.result(r, timeOut)
    }

    "ZREMRANGEBYSCORE" in {
      val r = for {
        _ <- redis.del("zremrangebyscoreKey")
        z1 <- redis.zadd("zremrangebyscoreKey", 1.0 -> "one", (2, "two"), (3, "three"))
        z2 <- redis.zremrangebyscore("zremrangebyscoreKey", Limit(Double.NegativeInfinity), Limit(2, inclusive = false))
        zr <- redis.zrangeWithscores("zremrangebyscoreKey", 0, -1)
      } yield {
        z1 mustEqual 3
        z2 mustEqual 1
        zr.get mustEqual Seq((ByteString("two"), 2), (ByteString("three"), 3))
      }
      Await.result(r, timeOut)
    }

    "ZREVRANGE" in {
      val r = for {
        _ <- redis.del("zrevrangeKey")
        z1 <- redis.zadd("zrevrangeKey", 1.0 -> "one", (2, "two"), (3, "three"))
        zr1 <- redis.zrevrange("zrevrangeKey", 0, -1)
        zr2 <- redis.zrevrange("zrevrangeKey", 2, 3)
        zr3 <- redis.zrevrange("zrevrangeKey", -2, -1)
      } yield {
        z1 mustEqual 3
        zr1.get mustEqual Seq(ByteString("three"), ByteString("two"), ByteString("one"))
        zr2.get mustEqual Seq(ByteString("one"))
        zr3.get mustEqual Seq(ByteString("two"), ByteString("one"))
      }
      Await.result(r, timeOut)
    }

    "ZREVRANGEBYSCORE" in {
      val r = for {
        _ <- redis.del("zrevrangebyscoreKey")
        z1 <- redis.zadd("zrevrangebyscoreKey", 1.0 -> "one", (2, "two"), (3, "three"))
        zr1 <- redis.zrevrangebyscore("zrevrangebyscoreKey", Limit(Double.PositiveInfinity), Limit(Double.NegativeInfinity))
        zr2 <- redis.zrevrangebyscore("zrevrangebyscoreKey", Limit(2), Limit(1))
        zr3 <- redis.zrevrangebyscore("zrevrangebyscoreKey", Limit(2), Limit(1, inclusive = false))
        zr4 <- redis.zrevrangebyscore("zrevrangebyscoreKey", Limit(2, inclusive = false), Limit(1, inclusive = false))
      } yield {
        z1 mustEqual 3
        zr1.get mustEqual Seq(ByteString("three"), ByteString("two"), ByteString("one"))
        zr2.get mustEqual Seq(ByteString("two"), ByteString("one"))
        zr3.get mustEqual Seq(ByteString("two"))
        zr4.get mustEqual Seq()
      }
      Await.result(r, timeOut)
    }

    "ZREVRANK" in {
      val r = for {
        _ <- redis.del("zrevrankKey")
        z1 <- redis.zadd("zrevrankKey", 1.0 -> "one", (2, "two"), (3, "three"))
        zr1 <- redis.zrevrank("zrevrankKey", "one")
        zr2 <- redis.zrevrank("zrevrankKey", "four")
      } yield {
        z1 mustEqual 3
        zr1.get mustEqual 2
        zr2 mustEqual None
      }
      Await.result(r, timeOut)
    }

    "ZSCORE" in {
      val r = for {
        _ <- redis.del("zscoreKey")
        z1 <- redis.zadd("zscoreKey", 1.1 -> "one", (2, "two"), (3, "three"))
        zr1 <- redis.zscore("zscoreKey", "one")
        zr2 <- redis.zscore("zscoreKey", "notexisting")
      } yield {
        z1 mustEqual 3
        zr1 mustEqual Some(1.1)
        zr2 mustEqual None
      }
      Await.result(r, timeOut)
    }

    "ZUNIONSTORE" in {
      val r = for {
        _ <- redis.del("zunionstoreKey1")
        _ <- redis.del("zunionstoreKey2")
        z1 <- redis.zadd("zunionstoreKey1", 1.0 -> "one", (2, "two"))
        z2 <- redis.zadd("zunionstoreKey2", 1.0 -> "one", (2, "two"), (3, "three"))
        i <- redis.zinterstoreWeighted("zunionstoreKeyOut", Seq(("zunionstoreKey1", 2), ("zunionstoreKey2", 3)))
        zr <- redis.zrangeWithscores("zunionstoreKeyOut", 0, -1)
      } yield {
        z1 mustEqual 2
        z2 mustEqual 3
        i mustEqual 2
        zr.get mustEqual Seq((ByteString("one"), 5), (ByteString("two"), 10))
      }
      Await.result(r, timeOut)
    }

  }
}
