package redis.commands

import redis._
import redis.api._
import redis.api.ZaddOption.{CH, NX, XX}

import scala.concurrent.{Await, Future}
import org.apache.pekko.util.ByteString

class SortedSetsSpec extends RedisStandaloneServer {

  "Sorted Sets commands" should {
    "ZADD" in {
      val r = for {
        version <- redisVersion()
        ge_3_0_2 = version.exists(_ >= RedisVersion(3, 0, 2))
        _ <- redis.del("zaddKey")
        z1 <- redis.zadd("zaddKey", 1.0 -> "one", (1, "uno"), (2, "two"))
        z2 <- redis.zadd("zaddKey", (3, "two"))
        z3 <- if (ge_3_0_2) redis.zaddWithOptions("zaddKey", Seq(XX, CH), 0.9 -> "one", (3, "three")) else Future.successful(1)
        z4 <- if (ge_3_0_2) redis.zaddWithOptions("zaddKey", Seq(NX), 0.8 -> "one", (4, "three")) else Future.successful(1)
        _ <- redis.zadd("zaddKey", 1.0 -> "one", (4, "three"))
        zr <- redis.zrangeWithscores("zaddKey", 0, -1)
      } yield {
        z1 mustEqual 3
        z2 mustEqual 0
        z3 mustEqual 1
        z4 mustEqual 1
        zr mustEqual Seq((ByteString("one"), 1.0), (ByteString("uno"), 1), (ByteString("two"), 3), (ByteString("three"), 4))
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
        zr mustEqual Seq((ByteString("two"), 2.0), (ByteString("notexisting"), 2.1), (ByteString("one"), 3.1))
      }
      Await.result(r, timeOut)
    }

    "ZINTERSTORE" in {
      val r = for {
        _ <- redis.del("zinterstoreKey1")
        _ <- redis.del("zinterstoreKey2")
        z1 <- redis.zadd("zinterstoreKey1", 1.0 -> "one", (2, "two"))
        z2 <- redis.zadd("zinterstoreKey2", 1.0 -> "one", (2, "two"), (3, "three"))
        zinterstore <- redis.zinterstore("zinterstoreKeyOut", "zinterstoreKey1", Seq("zinterstoreKey2"))
        zinterstoreWeighted <- redis.zinterstoreWeighted("zinterstoreKeyOutWeighted", Map("zinterstoreKey1" -> 2, "zinterstoreKey2" -> 3))
        zr <- redis.zrangeWithscores("zinterstoreKeyOut", 0, -1)
        zrWeighted <- redis.zrangeWithscores("zinterstoreKeyOutWeighted", 0, -1)
      } yield {
        z1 mustEqual 2
        z2 mustEqual 3
        zinterstore mustEqual 2
        zinterstoreWeighted mustEqual 2
        zr mustEqual Seq((ByteString("one"), 2), (ByteString("two"), 4))
        zrWeighted mustEqual Seq((ByteString("one"), 5), (ByteString("two"), 10))
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
        zr1 mustEqual Seq(ByteString("one"), ByteString("two"), ByteString("three"))
        zr2 mustEqual Seq(ByteString("three"))
        zr3 mustEqual Seq(ByteString("two"), ByteString("three"))
      }
      Await.result(r, timeOut)
    }

    "ZRANGEBYSCORE" in {
      val r = for {
        _ <- redis.del("zrangebyscoreKey")
        z1 <- redis.zadd("zrangebyscoreKey", 1.0 -> "one", (2, "two"), (3, "three"))
        zr1 <- redis.zrangebyscore("zrangebyscoreKey", Limit(Double.NegativeInfinity), Limit(Double.PositiveInfinity))
        zr1Limit <- redis.zrangebyscore("zrangebyscoreKey", Limit(Double.NegativeInfinity), Limit(Double.PositiveInfinity), Some(1L -> 2L))
        zr2 <- redis.zrangebyscore("zrangebyscoreKey", Limit(1), Limit(2))
        zr2WithScores <- redis.zrangebyscoreWithscores("zrangebyscoreKey", Limit(1), Limit(2))
        zr3 <- redis.zrangebyscore("zrangebyscoreKey", Limit(1, inclusive = false), Limit(2))
        zr4 <- redis.zrangebyscore("zrangebyscoreKey", Limit(1, inclusive = false), Limit(2, inclusive = false))
      } yield {
        z1 mustEqual 3
        zr1 mustEqual Seq(ByteString("one"), ByteString("two"), ByteString("three"))
        zr1Limit mustEqual Seq(ByteString("two"), ByteString("three"))
        zr2 mustEqual Seq(ByteString("one"), ByteString("two"))
        zr2WithScores mustEqual Seq((ByteString("one"), 1), (ByteString("two"), 2))
        zr3 mustEqual Seq(ByteString("two"))
        zr4 mustEqual Seq()
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
        zr1 must beSome(2)
        zr2 must beNone
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
        zr mustEqual Seq((ByteString("one"), 1), (ByteString("three"), 3))
      }
      Await.result(r, timeOut)
    }

    "ZREMRANGEBYLEX" in {
      val r = for {
        _ <- redis.del("zremrangebylexKey")
        z1 <- redis.zadd("zremrangebylexKey", 0d -> "a", 0d -> "b", 0d -> "c", 0d -> "d", 0d -> "e", 0d -> "f", 0d -> "g")
        z2 <- redis.zremrangebylex("zremrangebylexKey", "[z", "[d")
        z3 <- redis.zremrangebylex("zremrangebylexKey", "[b", "[d")
        zrange1 <- redis.zrange("zremrangebylexKey", 0, -1)
      } yield {
        z1 mustEqual 7
        z2 mustEqual 0
        z3 mustEqual 3
        zrange1 mustEqual Seq(ByteString("a"), ByteString("e"), ByteString("f"), ByteString("g"))
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
        zr mustEqual Seq((ByteString("three"), 3))
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
        zr mustEqual Seq((ByteString("two"), 2), (ByteString("three"), 3))
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
        zr3WithScores <- redis.zrevrangeWithscores("zrevrangeKey", -2, -1)
      } yield {
        z1 mustEqual 3
        zr1 mustEqual Seq(ByteString("three"), ByteString("two"), ByteString("one"))
        zr2 mustEqual Seq(ByteString("one"))
        zr3 mustEqual Seq(ByteString("two"), ByteString("one"))
        zr3WithScores mustEqual Seq((ByteString("two"), 2), (ByteString("one"), 1))
      }
      Await.result(r, timeOut)
    }

    "ZREVRANGEBYSCORE" in {
      val r = for {
        _ <- redis.del("zrevrangebyscoreKey")
        z1 <- redis.zadd("zrevrangebyscoreKey", 1.0 -> "one", (2, "two"), (3, "three"))
        zr1 <- redis.zrevrangebyscore("zrevrangebyscoreKey", Limit(Double.PositiveInfinity), Limit(Double.NegativeInfinity))
        zr2 <- redis.zrevrangebyscore("zrevrangebyscoreKey", Limit(2), Limit(1))
        zr2WithScores <- redis.zrevrangebyscoreWithscores("zrevrangebyscoreKey", Limit(2), Limit(1))
        zr3 <- redis.zrevrangebyscore("zrevrangebyscoreKey", Limit(2), Limit(1, inclusive = false))
        zr4 <- redis.zrevrangebyscore("zrevrangebyscoreKey", Limit(2, inclusive = false), Limit(1, inclusive = false))
      } yield {
        z1 mustEqual 3
        zr1 mustEqual Seq(ByteString("three"), ByteString("two"), ByteString("one"))
        zr2 mustEqual Seq(ByteString("two"), ByteString("one"))
        zr2WithScores mustEqual Seq((ByteString("two"), 2), (ByteString("one"), 1))
        zr3 mustEqual Seq(ByteString("two"))
        zr4 mustEqual Seq()
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
        zr1 must beSome(2)
        zr2 must beNone
      }
      Await.result(r, timeOut)
    }

    "ZSCAN" in {
      val r = for {
        _ <- redis.del("zscan")
        _ <- redis.zadd("zscan", (1 to 20).map(x => x.toDouble -> x.toString):_*)
        scanResult <- redis.zscan[String]("zscan", count = Some(100))
      } yield {
        scanResult.index mustEqual 0
        scanResult.data mustEqual (1 to 20).map(x => x.toDouble -> x.toString)
      }

      Await.result(r, timeOut)
    }

    "ZSCORE" in {
      val r = for {
        _ <- redis.del("zscoreKey")
        z1 <- redis.zadd("zscoreKey", 1.1 -> "one", (2, "two"), (3, "three"), Double.PositiveInfinity -> "positiveinf", Double.NegativeInfinity -> "negativeinf")
        zr1 <- redis.zscore("zscoreKey", "one")
        zr2 <- redis.zscore("zscoreKey", "notexisting")
        zr3 <- redis.zscore("zscoreKey", "positiveinf")
        zr4 <- redis.zscore("zscoreKey", "negativeinf")
      } yield {
        z1 mustEqual 5
        zr1 mustEqual Some(1.1)
        zr2 mustEqual None
        zr3 mustEqual Some(Double.PositiveInfinity)
        zr4 mustEqual Some(Double.NegativeInfinity)
      }
      Await.result(r, timeOut)
    }

    "ZUNIONSTORE" in {
      val r = for {
        _ <- redis.del("zunionstoreKey1")
        _ <- redis.del("zunionstoreKey2")
        z1 <- redis.zadd("zunionstoreKey1", 1.0 -> "one", (2, "two"))
        z2 <- redis.zadd("zunionstoreKey2", 1.0 -> "one", (2, "two"), (3, "three"))
        zunionstore <- redis.zunionstore("zunionstoreKeyOut", "zunionstoreKey1", Seq("zunionstoreKey2"))
        zr <- redis.zrangeWithscores("zunionstoreKeyOut", 0, -1)
        zunionstoreWeighted <- redis.zunionstoreWeighted("zunionstoreKeyOutWeighted", Map("zunionstoreKey1" -> 2, "zunionstoreKey2" -> 3))
        zrWeighted <- redis.zrangeWithscores("zunionstoreKeyOutWeighted", 0, -1)
      } yield {
        z1 mustEqual 2
        z2 mustEqual 3
        zunionstore mustEqual 3
        zr mustEqual Seq((ByteString("one"), 2), (ByteString("three"), 3), (ByteString("two"), 4))
        zunionstoreWeighted mustEqual 3
        zrWeighted mustEqual Seq((ByteString("one"), 5), (ByteString("three"), 9), (ByteString("two"), 10))
      }
      Await.result(r, timeOut)
    }

    "ZRANGEBYLEX" in {
      val r = for {
        _ <- redis.del("zrangebylexKey")
        z1 <- redis.zadd("zrangebylexKey", (0, "lexA"), (0, "lexB"), (0, "lexC"))
        zr1 <- redis.zrangebylex("zrangebylexKey", Some("[lex"), None, None)
        zr2 <- redis.zrangebylex("zrangebylexKey", Some("[lex"), None, Some((0L, 1L)))
      } yield {
        z1 mustEqual 3
        zr1 mustEqual Seq(ByteString("lexA"), ByteString("lexB"), ByteString("lexC"))
        zr2 mustEqual Seq(ByteString("lexA"))
      }
      Await.result(r, timeOut)
    }

    "ZREVRANGEBYLEX" in {
      val r = for {
        _ <- redis.del("zrevrangebylexKey")
        z1 <- redis.zadd("zrevrangebylexKey", (0, "lexA"), (0, "lexB"), (0, "lexC"))
        zr1 <- redis.zrevrangebylex("zrevrangebylexKey", None, Some("[lex"), None)
        zr2 <- redis.zrevrangebylex("zrevrangebylexKey", None, Some("[lex"), Some((0L, 1L)))
      } yield {
        z1 mustEqual 3
        zr1 mustEqual Seq(ByteString("lexC"), ByteString("lexB"), ByteString("lexA"))
        zr2 mustEqual Seq(ByteString("lexC"))
      }
      Await.result(r, timeOut)
    }
  }
}
