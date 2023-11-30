package redis.commands

import redis._
import scala.concurrent.Await
import org.apache.pekko.util.ByteString

class ListsSpec extends RedisStandaloneServer {

  "Lists commands" should {

    "LINDEX" in {
      val r = for {
        _ <- redis.del("lindexKey")
        _ <- redis.lpush("lindexKey", "World", "Hello")
        hello <- redis.lindex("lindexKey", 0)
        world <- redis.lindex("lindexKey", 1)
        none <- redis.lindex("lindexKey", 2)
      } yield {
        hello mustEqual Some(ByteString("Hello"))
        world mustEqual Some(ByteString("World"))
        none mustEqual None
      }
      Await.result(r, timeOut)
    }

    "LINSERT" in {
      val r = for {
        _ <- redis.del("linsertKey")
        _ <- redis.lpush("linsertKey", "World", "Hello")
        length <- redis.linsertBefore("linsertKey", "World", "There")
        list <- redis.lrange("linsertKey", 0, -1)
        length4 <- redis.linsertAfter("linsertKey", "World", "!!!")
        list4 <- redis.lrange("linsertKey", 0, -1)
      } yield {
        length mustEqual 3
        list mustEqual Seq(ByteString("Hello"), ByteString("There"), ByteString("World"))
        length4 mustEqual 4
        list4 mustEqual Seq(ByteString("Hello"), ByteString("There"), ByteString("World"), ByteString("!!!"))
      }
      Await.result(r, timeOut)
    }

    "LLEN" in {
      val r = for {
        _ <- redis.del("llenKey")
        _ <- redis.lpush("llenKey", "World", "Hello")
        length <- redis.llen("llenKey")
      } yield {
        length mustEqual 2
      }
      Await.result(r, timeOut)
    }

    "LPOP" in {
      val r = for {
        _ <- redis.del("lpopKey")
        _ <- redis.rpush("lpopKey", "one", "two", "three")
        e <- redis.lpop("lpopKey")
      } yield {
        e mustEqual Some(ByteString("one"))
      }
      Await.result(r, timeOut)
    }

    "LPUSH" in {
      val r = for {
        _ <- redis.del("lpushKey")
        _ <- redis.lpush("lpushKey", "World", "Hello")
        list <- redis.lrange("lpushKey", 0, -1)
      } yield {
        list mustEqual Seq(ByteString("Hello"), ByteString("World"))
      }
      Await.result(r, timeOut)
    }

    "LPUSHX" in {
      val r = for {
        _ <- redis.del("lpushxKey")
        _ <- redis.del("lpushxKeyOther")
        i <- redis.rpush("lpushxKey", "c")
        ii <- redis.lpushx("lpushxKey", "b", "a")
        zero <- redis.lpushx("lpushxKeyOther", "b", "a")
        list <- redis.lrange("lpushxKey", 0, -1)
        listOther <- redis.lrange("lpushxKeyOther", 0, -1)
      } yield {
        i mustEqual 1
        ii mustEqual 3
        zero mustEqual 0
        list mustEqual Seq(ByteString("a"), ByteString("b"), ByteString("c"))
        listOther must beEmpty
      }
      Await.result(r, timeOut)
    }

    "LRANGE" in {
      val r = for {
        _ <- redis.del("lrangeKey")
        _ <- redis.rpush("lrangeKey", "one", "two", "three")
        list <- redis.lrange("lrangeKey", 0, 0)
        list2 <- redis.lrange("lrangeKey", -3, 2)
        list3 <- redis.lrange("lrangeKey", 5, 10)
        nonExisting <- redis.lrange("lrangeKeyNonexisting", 5, 10)
      } yield {
        list mustEqual Seq(ByteString("one"))
        list2 mustEqual Seq(ByteString("one"), ByteString("two"), ByteString("three"))
        list3 must beEmpty
        nonExisting must beEmpty
      }
      Await.result(r, timeOut)
    }

    "LREM" in {
      val r = for {
        _ <- redis.del("lremKey")
        _ <- redis.rpush("lremKey", "hello", "hello", "foo", "hello")
        lrem <- redis.lrem("lremKey", -2, "hello")
        list2 <- redis.lrange("lremKey", 0, -1)
      } yield {
        lrem mustEqual 2
        list2 mustEqual Seq(ByteString("hello"), ByteString("foo"))
      }
      Await.result(r, timeOut)
    }

    "LSET" in {
      val r = for {
        _ <- redis.del("lsetKey")
        _ <- redis.rpush("lsetKey", "one", "two", "three")
        lset1 <- redis.lset("lsetKey", 0, "four")
        lset2 <- redis.lset("lsetKey", -2, "five")
        list <- redis.lrange("lsetKey", 0, -1)
      } yield {
        lset1 mustEqual true
        lset2 mustEqual true
        list mustEqual Seq(ByteString("four"), ByteString("five"), ByteString("three"))
      }
      Await.result(r, timeOut)
    }

    "LTRIM" in {
      val r = for {
        _ <- redis.del("ltrimKey")
        _ <- redis.rpush("ltrimKey", "one", "two", "three")
        ltrim <- redis.ltrim("ltrimKey", 1, -1)
        list <- redis.lrange("ltrimKey", 0, -1)
      } yield {
        ltrim mustEqual true
        list mustEqual Seq(ByteString("two"), ByteString("three"))
      }
      Await.result(r, timeOut)
    }

    "RPOP" in {
      val r = for {
        _ <- redis.del("rpopKey")
        _ <- redis.rpush("rpopKey", "one", "two", "three")
        rpop <- redis.rpop("rpopKey")
        list <- redis.lrange("rpopKey", 0, -1)
      } yield {
        rpop mustEqual Some(ByteString("three"))
        list mustEqual Seq(ByteString("one"), ByteString("two"))
      }
      Await.result(r, timeOut)
    }

    "RPOPLPUSH" in {
      val r = for {
        _ <- redis.del("rpoplpushKey")
        _ <- redis.del("rpoplpushKeyOther")
        _ <- redis.rpush("rpoplpushKey", "one", "two", "three")
        rpoplpush <- redis.rpoplpush("rpoplpushKey", "rpoplpushKeyOther")
        list <- redis.lrange("rpoplpushKey", 0, -1)
        listOther <- redis.lrange("rpoplpushKeyOther", 0, -1)
      } yield {
        rpoplpush mustEqual Some(ByteString("three"))
        list mustEqual Seq(ByteString("one"), ByteString("two"))
        listOther mustEqual Seq(ByteString("three"))
      }
      Await.result(r, timeOut)
    }

    "RPUSH" in {
      val r = for {
        _ <- redis.del("rpushKey")
        i <- redis.rpush("rpushKey", "hello")
        ii <- redis.rpush("rpushKey", "world")
        list <- redis.lrange("rpushKey", 0, -1)
      } yield {
        i mustEqual 1
        ii mustEqual 2
        list mustEqual Seq(ByteString("hello"), ByteString("world"))
      }
      Await.result(r, timeOut)
    }

    "RPUSHX" in {
      val r = for {
        _ <- redis.del("rpushxKey")
        _ <- redis.del("rpushxKeyOther")
        i <- redis.rpush("rpushxKey", "a")
        ii <- redis.rpushx("rpushxKey", "b", "c")
        zero <- redis.rpushx("rpushxKeyOther", "a", "b")
        list <- redis.lrange("rpushxKey", 0, -1)
        listOther <- redis.lrange("rpushxKeyOther", 0, -1)
      } yield {
        i mustEqual 1
        ii mustEqual 3
        zero mustEqual 0
        list mustEqual Seq(ByteString("a"), ByteString("b"), ByteString("c"))
        listOther must beEmpty
      }
      Await.result(r, timeOut)
    }
  }
}
