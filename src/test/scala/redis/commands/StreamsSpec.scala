package redis.commands

import redis._
import redis.api._
import scala.concurrent.{Await, Future}
import akka.util.ByteString

class StreamsSpec extends RedisStandaloneServer {
  "Streams commands" should {
    "XADD" in {
      val key = "xaddKey"
      val r = for {
        _ <- redis.del(key)
        a <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", 1))
      } yield {
        a.time mustNotEqual 0
      }
      Await.result(r, timeOut)
    }

    "XADD MAXLEN" in {
      val key = "xaddmaxlenKey"
      val r = for {
        _ <- redis.del(key)
        _ <- redis.xadd(key, MaxLength(1), StreamId.AUTOGENERATE, ("item", 1))
        _ <- redis.xadd(key, MaxLength(1), StreamId.AUTOGENERATE, ("item", 2))
        l <- redis.xlen(key)
      } yield {
        l mustEqual 1
      }
      Await.result(r, timeOut)
    }

    "XDEL" in {
      val key = "xdelKey"
      val r = for {
        _ <- redis.del(key)
        a <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", 1))
        d <- redis.xdel(key, a)
      } yield {
        d mustEqual 1
      }
      Await.result(r, timeOut)
    }

    "XLEN" in {
      val key = "xlenKey"
      val r = for {
        _ <- redis.del(key)
        _ <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", 1))
        _ <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", 2))
        _ <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", 3))
        l <- redis.xlen(key)
      } yield {
        l mustEqual 3
      }
      Await.result(r, timeOut)
    }

    "XRANGE" in {
      val key = "xrangeKey"
      val r = for {
        _ <- redis.del(key)
        r1 <- redis.xrange(key, StreamId.MIN, StreamId.MAX)
        a1 <- redis.xadd(key, StreamId.AUTOGENERATE, ("b", "1"), ("a", "one"))
        a2 <- redis.xadd(key, StreamId.AUTOGENERATE, ("a", "2"), ("b", "two"))
        r2 <- redis.xrange[ByteString](key, StreamId.MIN, StreamId.MAX)
      } yield {
        r1.length mustEqual 0
        r2.length mustEqual 2
        r2(0).id mustEqual a1
        r2(0).fields mustEqual Seq("b" -> ByteString("1"), "a" -> ByteString("one"))
        r2(1).id mustEqual a2
        r2(1).fields mustEqual Seq("a" -> ByteString("2"), "b" -> ByteString("two"))
      }
      Await.result(r, timeOut)
    }

    "XRANGE COUNT" in {
      val key = "xrangecountKey"
      val r = for {
        _ <- redis.del(key)
        a1 <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", "1"))
        _ <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", "2"))
        r1 <- redis.xrange(key, StreamId.MIN, StreamId.MAX, Some(1))
      } yield {
        r1.length mustEqual 1
        r1(0).id mustEqual a1
        r1(0).fields mustEqual Seq("item" -> ByteString("1"))
      }
      Await.result(r, timeOut)
    }

    "XREAD" in {
      val key1 = "xreadKey1"
      val key2 = "xreadKey2"
      val r = for {
        _ <- redis.del(key1, key2)
        a1 <- redis.xadd(key1, StreamId.AUTOGENERATE, ("item", "1"))
        a2 <- redis.xadd(key1, StreamId.AUTOGENERATE, ("item", "2"))
        a3 <- redis.xadd(key2, StreamId.AUTOGENERATE, ("item", "3"))
        r1 <- redis.xread(key1 -> StreamId(0), key2 -> StreamId(0))
        _ <- redis.del(key1, key2)
        r2 <- redis.xread(key1 -> StreamId(0), key2 -> StreamId(0))
      } yield {
        val x = r1.get
        x.length mustEqual 2
        x(0)._1 mustEqual key1
        x(0)._2.length mustEqual 2
        x(0)._2(0).id mustEqual a1
        x(0)._2(0).fields mustEqual Seq(("item" -> ByteString("1")))
        x(0)._2(1).fields mustEqual Seq(("item" -> ByteString("2")))
        x(1)._1 mustEqual key2
        x(1)._2.length mustEqual 1
        x(1)._2(0).id mustEqual a3
        x(1)._2(0).fields mustEqual Seq(("item" -> ByteString("3")))
        r2 mustEqual None
      }
      Await.result(r, timeOut)
    }

    "XREAD COUNT" in {
      val key = "xreadcountKey"
      val r = for {
        _ <- redis.del(key)
        a1 <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", "1"))
        a2 <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", "2"))
        _ <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", "3"))
        r1 <- redis.xread(1, key -> a1)
      } yield {
        val x = r1.get
        x.length mustEqual 1
        x(0)._1 mustEqual key
        x(0)._2.length mustEqual 1
        x(0)._2(0).id mustEqual a2
        x(0)._2(0).fields mustEqual Seq(("item" -> ByteString("2")))
      }
      Await.result(r, timeOut)
    }


    "XREVRANGE" in {
      val key = "xrevrangeKey"
      val r = for {
        _ <- redis.del(key)
        a1 <- redis.xadd(key, StreamId.AUTOGENERATE, ("b", "1"), ("a", "one"))
        a2 <- redis.xadd(key, StreamId.AUTOGENERATE, ("a", "2"), ("b", "two"))
        r1 <- redis.xrevrange[ByteString](key, StreamId.MAX, StreamId.MIN)
      } yield {
        r1.length mustEqual 2
        r1(0).id mustEqual a2
        r1(0).fields mustEqual Seq("a" -> ByteString("2"), "b" -> ByteString("two"))
        r1(1).id mustEqual a1
        r1(1).fields mustEqual Seq("b" -> ByteString("1"), "a" -> ByteString("one"))
      }
      Await.result(r, timeOut)
    }

    "XREVRANGE COUNT" in {
      val key = "xrevrangecountKey"
      val r = for {
        _ <- redis.del(key)
        _ <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", "1"))
        a2 <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", "2"))
        r1 <- redis.xrevrange(key, StreamId.MAX, StreamId.MIN, Some(1))
      } yield {
        r1.length mustEqual 1
        r1(0).id mustEqual a2
        r1(0).fields mustEqual Seq("item" -> ByteString("2"))
      }
      Await.result(r, timeOut)
    }

    "XTRIM" in {
      val key = "xtrimKey"
      val r = for {
        _ <- redis.del(key)
        _ <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", "1"))
        _ <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", "2"))
        _ <- redis.xadd(key, StreamId.AUTOGENERATE, ("item", "3"))
        r <- redis.xtrim(key, MaxLength(1))
        l <- redis.xlen(key)
      } yield {
        r mustEqual 2
        l mustEqual 1
      }
      Await.result(r, timeOut)
    }
  }
}