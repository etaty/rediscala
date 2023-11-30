package redis.commands

import redis._
import scala.concurrent.{Await, Future}
import org.apache.pekko.util.ByteString
import redis.actors.ReplyErrorException

class StringsSpec extends RedisStandaloneServer {

  sequential
  "Strings commands" should {
    "APPEND" in {
      val r = redis.set("appendKey", "Hello").flatMap(_ => {
        redis.append("appendKey", " World").flatMap(length => {
          length mustEqual "Hello World".length
          redis.get("appendKey")
        })
      })
      Await.result(r, timeOut) mustEqual Some(ByteString("Hello World"))
    }

    "BITCOUNT" in {
      val r = redis.set("bitcountKey", "foobar").flatMap(_ => {
        val a = redis.bitcount("bitcountKey")
        val b = redis.bitcount("bitcountKey", 0, 0)
        val c = redis.bitcount("bitcountKey", 1, 1)
        Future.sequence(Seq(a, b, c))
      })
      Await.result(r, timeOut) mustEqual Seq(26, 4, 6)
    }

    "BITOP" in {
      val s1 = redis.set("bitopKey1", "afoobar a")
      val s2 = redis.set("bitopKey2", "aabcdef a")
      val r = for {
        _ <- s1
        _ <- s2
        and <- redis.bitopAND("ANDbitopKey", "bitopKey1", "bitopKey2")
        or <- redis.bitopOR("ORbitopKey", "bitopKey1", "bitopKey2")
        xor <- redis.bitopXOR("XORbitopKey", "bitopKey1", "bitopKey2")
        not <- redis.bitopNOT("NOTbitopKey", "bitopKey1")
      } yield {
        "AND" in {
          Await.result(redis.get("ANDbitopKey"), timeOut) mustEqual Some(ByteString("a`bc`ab a"))
        }
        "OR" in {
          Await.result(redis.get("ORbitopKey"), timeOut) mustEqual Some(ByteString("agoofev a"))
        }
        "XOR" in {
          Await.result(redis.get("XORbitopKey"), timeOut) mustEqual Some(ByteString(0, 7, 13, 12, 6, 4, 20, 0, 0))
        }
        "NOT" in {
          Await.result(redis.get("NOTbitopKey"), timeOut) mustEqual Some(ByteString(-98, -103, -112, -112, -99, -98, -115, -33, -98))
        }
      }
      Await.result(r, timeOut)
    }

    "BITPOS" in {
      val r = for {
        s1 <- redis.set("bitposKey", "a+b") // 01100001 00101011 01100010
        v1 <- redis.bitpos("bitposKey", 0)
        v2 <- redis.bitpos("bitposKey", 1)
        v3 <- redis.bitpos("bitposKey", 1, 1)
        v4 <- redis.bitpos("bitposKey", 0, 3)
        v5 <- redis.bitpos("bitposKey", 0, 1, 2)
      } yield {
        s1 mustEqual true
        v1 mustEqual 0
        v2 mustEqual 1
        v3 mustEqual 10
        v4 mustEqual -1
        v5 mustEqual 8
      }
      Await.result(r, timeOut)
    }

    "DECR" in {
      val r = redis.set("decrKey", "10").flatMap(_ => {
        redis.decr("decrKey")
      })
      val r2 = redis.set("decrKeyError", "234293482390480948029348230948").flatMap(_ => {
        redis.decr("decrKeyError")
      })
      Await.result(r, timeOut) mustEqual 9
      Await.result(r2, timeOut) must throwA[ReplyErrorException]("ERR value is not an integer or out of range")
    }

    "DECRBY" in {
      val r = redis.set("decrbyKey", "10").flatMap(_ => {
        redis.decrby("decrbyKey", 5)
      })
      Await.result(r, timeOut) mustEqual 5
    }

    "GET" in {
      val r = redis.get("getKeyNonexisting")
      val r2 = redis.set("getKey", "Hello").flatMap(_ => {
        redis.get("getKey")
      })
      Await.result(r, timeOut) mustEqual None
      Await.result(r2, timeOut) mustEqual Some(ByteString("Hello"))

      val rrr = for {
        r3 <- redis.get[String]("getKey")
      } yield {
        r3 must beSome("Hello")
      }
      Await.result(rrr, timeOut)
    }

    "GET with conversion" in {
      val dumbObject = new DumbClass("foo", "bar")
      val r = redis.set("getDumbKey", dumbObject).flatMap(_ => {
        redis.get[DumbClass]("getDumbKey")
      })
      Await.result(r, timeOut) mustEqual Some(dumbObject)
    }

    "GETBIT" in {
      val r = redis.getbit("getbitKeyNonexisting", 0)
      val r2 = redis.set("getbitKey", "Hello").flatMap(_ => {
        redis.getbit("getbitKey", 1)
      })
      Await.result(r, timeOut) mustEqual false
      Await.result(r2, timeOut) mustEqual true
    }

    "GETRANGE" in {
      val r = redis.set("getrangeKey", "This is a string").flatMap(_ => {
        Future.sequence(Seq(
          redis.getrange("getrangeKey", 0, 3),
          redis.getrange("getrangeKey", -3, -1),
          redis.getrange("getrangeKey", 0, -1),
          redis.getrange("getrangeKey", 10, 100)
        ).map(_.map(_.map(_.utf8String).get)))
      })
      Await.result(r, timeOut) mustEqual Seq("This", "ing", "This is a string", "string")
    }

    "GETSET" in {
      val r = redis.set("getsetKey", "Hello").flatMap(_ => {
        redis.getset("getsetKey", "World").flatMap(hello => {
          hello mustEqual Some(ByteString("Hello"))
          redis.get("getsetKey")
        })
      })
      Await.result(r, timeOut) mustEqual Some(ByteString("World"))
    }

    "INCR" in {
      val r = redis.set("incrKey", "10").flatMap(_ => {
        redis.incr("incrKey")
      })
      Await.result(r, timeOut) mustEqual 11
    }

    "INCRBY" in {
      val r = redis.set("incrbyKey", "10").flatMap(_ => {
        redis.incrby("incrbyKey", 5)
      })
      Await.result(r, timeOut) mustEqual 15
    }

    "INCRBYFLOAT" in {
      val r = redis.set("incrbyfloatKey", "10.50").flatMap(_ => {
        redis.incrbyfloat("incrbyfloatKey", 0.15)
      })
      Await.result(r, timeOut) mustEqual Some(10.65)
    }

    "MGET" in {
      val s1 = redis.set("mgetKey", "Hello")
      val s2 = redis.set("mgetKey2", "World")
      val r = for {
        _ <- s1
        _ <- s2
        mget <- redis.mget("mgetKey", "mgetKey2", "mgetKeyNonexisting")
      } yield {
        mget mustEqual Seq(Some(ByteString("Hello")), Some(ByteString("World")), None)
      }
      Await.result(r, timeOut)
    }

    "MSET" in {
      val r = redis.mset(Map("msetKey" -> "Hello", "msetKey2" -> "World")).flatMap(ok => {
        ok mustEqual true
        Future.sequence(Seq(
          redis.get("msetKey"),
          redis.get("msetKey2")
        ))
      })
      Await.result(r, timeOut) mustEqual Seq(Some(ByteString("Hello")), Some(ByteString("World")))
    }

    "MSETNX" in {
      val r = for {
        _ <- redis.del("msetnxKey", "msetnxKey2")
        msetnx <- redis.msetnx(Map("msetnxKey" -> "Hello", "msetnxKey2" -> "World"))
        msetnxFalse <- redis.msetnx(Map("msetnxKey3" -> "Hello", "msetnxKey2" -> "already set !!"))
      } yield {
        msetnx mustEqual true
        msetnxFalse mustEqual false
      }
      Await.result(r, timeOut)
    }

    "PSETEX" in {
      val r = redis.psetex("psetexKey", 2000, "temp value").flatMap(x => {
        x mustEqual true
        redis.get("psetexKey").flatMap(v => {
          v mustEqual Some(ByteString("temp value"))
          Thread.sleep(2000)
          redis.get("psetexKey")
        })
      })
      Await.result(r, timeOut) mustEqual None
    }

    "SET" in {
      val rr = for {
        r <- redis.set("setKey", "value")
        ex <- redis.set("setKey", "value", exSeconds = Some(2))
        nxex <- redis.set("setKey2", "value", NX = true, exSeconds = Some(60))
        ttlnxex <- redis.ttl("setKey2")
        xxex <- redis.set("setKey2", "value", XX = true, exSeconds = Some(180))
        ttlxxex <- redis.ttl("setKey2")
        _ <- redis.del("setKey2")
        px <- redis.set("setKey", "value", pxMilliseconds = Some(1))
        nxTrue <- {
          Thread.sleep(20)
          redis.set("setKey", "value", NX = true)
        }
        xx <- redis.set("setKey", "value", XX = true)
        nxFalse <- redis.set("setKey", "value", NX = true)
      } yield {
        r mustEqual true
        ex mustEqual true
        nxex must beTrue
        ttlnxex must beBetween[Long](0, 60)
        xxex must beTrue
        ttlxxex must beBetween[Long](60, 180)
        px mustEqual true
        nxTrue mustEqual true // because pxMilliseconds = 1 millisecond
        xx mustEqual true
        nxFalse mustEqual false
      }
      Await.result(rr, timeOut)
    }

    "SETBIT" in {
      val r = for {
        _ <- redis.del("setbitKey")
        setTrue <- redis.setbit("setbitKey", 1, value = true)
        getTrue <- redis.getbit("setbitKey", 1)
        setFalse <- redis.setbit("setbitKey", 1, value = false)
        getFalse <- redis.getbit("setbitKey", 1)
      } yield {
        setTrue mustEqual false
        getTrue mustEqual true
        setFalse mustEqual true
        getFalse mustEqual false
      }
      Await.result(r, timeOut)
    }

    "SETEX" in {
      val r = redis.setex("setexKey", 1, "temp value").flatMap(x => {
        x mustEqual true
        redis.get("setexKey").flatMap(v => {
          v mustEqual Some(ByteString("temp value"))
          Thread.sleep(2000)
          redis.get("setexKey")
        })
      })
      Await.result(r, timeOut) mustEqual None
    }

    "SETNX" in {
      val r = for {
        _ <- redis.del("setnxKey")
        s1 <- redis.setnx("setnxKey", "Hello")
        s2 <- redis.setnx("setnxKey", "World")
      } yield {
        s1 mustEqual true
        s2 mustEqual false
      }
      Await.result(r, timeOut)
    }

    "SETRANGE" in {
      val r = redis.set("setrangeKey", "Hello World").flatMap(d => {
        redis.setrange("setrangeKey", 6, "Redis").flatMap(length => {
          length mustEqual "Hello Redis".length
          redis.get("setrangeKey")
        })
      })
      Await.result(r, timeOut) mustEqual Some(ByteString("Hello Redis"))
    }

    "STRLEN" in {
      val r = redis.set("strlenKey", "Hello World").flatMap(d => {
        redis.strlen("strlenKey").flatMap(length => {
          length mustEqual "Hello World".length
          redis.strlen("strlenKeyNonexisting")
        })
      })
      Await.result(r, timeOut) mustEqual 0
    }
  }
}
