package redis.commands

import akka.util.ByteString
import redis._

import scala.concurrent.Await

class SetsSpec extends RedisStandaloneServer {

  "Sets commands" should {
    "SADD" in {
      val r = for {
        _ <- redis.del("saddKey")
        s1 <- redis.sadd("saddKey", "Hello", "World")
        s2 <- redis.sadd("saddKey", "World")
        m <- redis.smembers("saddKey")
      } yield {
        s1 mustEqual 2
        s2 mustEqual 0
        m must containTheSameElementsAs(Seq(ByteString("Hello"), ByteString("World")))
      }
      Await.result(r, timeOut)
    }

    "SCARD" in {
      val r = for {
        _ <- redis.del("scardKey")
        c1 <- redis.scard("scardKey")
        _ <- redis.sadd("scardKey", "Hello", "World")
        c2 <- redis.scard("scardKey")
      } yield {
        c1 mustEqual 0
        c2 mustEqual 2
      }
      Await.result(r, timeOut)
    }

    "SDIFF" in {
      val r = for {
        _ <- redis.del("sdiffKey1")
        _ <- redis.del("sdiffKey2")
        _ <- redis.sadd("sdiffKey1", "a", "b", "c")
        _ <- redis.sadd("sdiffKey2", "c", "d", "e")
        diff <- redis.sdiff("sdiffKey1", "sdiffKey2")
      } yield {
        diff must containTheSameElementsAs(Seq(ByteString("a"), ByteString("b")))
      }
      Await.result(r, timeOut)
    }

    "SDIFFSTORE" in {
      val r = for {
        _ <- redis.del("sdiffstoreKey1")
        _ <- redis.del("sdiffstoreKey2")
        _ <- redis.sadd("sdiffstoreKey1", "a", "b", "c")
        _ <- redis.sadd("sdiffstoreKey2", "c", "d", "e")
        diff <- redis.sdiffstore("sdiffstoreKeyDest", "sdiffstoreKey1", "sdiffstoreKey2")
        m <- redis.smembers("sdiffstoreKeyDest")
      } yield {
        diff mustEqual 2
        m must containTheSameElementsAs(Seq(ByteString("a"), ByteString("b")))
      }
      Await.result(r, timeOut)
    }

    "SINTER" in {
      val r = for {
        _ <- redis.del("sinterKey1")
        _ <- redis.del("sinterKey2")
        _ <- redis.sadd("sinterKey1", "a", "b", "c")
        _ <- redis.sadd("sinterKey2", "c", "d", "e")
        inter <- redis.sinter("sinterKey1", "sinterKey2")
      } yield {
        inter must containTheSameElementsAs(Seq(ByteString("c")))
      }
      Await.result(r, timeOut)
    }


    "SINTERSTORE" in {
      val r = for {
        _ <- redis.del("sinterstoreKey1")
        _ <- redis.del("sinterstoreKey2")
        _ <- redis.sadd("sinterstoreKey1", "a", "b", "c")
        _ <- redis.sadd("sinterstoreKey2", "c", "d", "e")
        inter <- redis.sinterstore("sinterstoreKeyDest", "sinterstoreKey1", "sinterstoreKey2")
        m <- redis.smembers("sinterstoreKeyDest")
      } yield {
        inter mustEqual 1
        m must containTheSameElementsAs(Seq(ByteString("c")))
      }
      Await.result(r, timeOut)
    }

    "SISMEMBER" in {
      val r = for {
        _ <- redis.del("sismemberKey")
        _ <- redis.sadd("sismemberKey", "Hello", "World")
        is <- redis.sismember("sismemberKey", "World")
        isNot <- redis.sismember("sismemberKey", "not member")
      } yield {
        is mustEqual true
        isNot mustEqual false
      }
      Await.result(r, timeOut)
    }

    "SMEMBERS" in {
      val r = for {
        _ <- redis.del("smembersKey")
        _ <- redis.sadd("smembersKey", "Hello", "World")
        m <- redis.smembers("smembersKey")
      } yield {
        m must containTheSameElementsAs(Seq(ByteString("Hello"), ByteString("World")))
      }
      Await.result(r, timeOut)
    }

    "SMOVE" in {
      val r = for {
        _ <- redis.del("smoveKey1")
        _ <- redis.del("smoveKey2")
        _ <- redis.sadd("smoveKey1", "one", "two")
        _ <- redis.sadd("smoveKey2", "three")
        isMoved <- redis.smove("smoveKey1", "smoveKey2", "two")
        isNotMoved <- redis.smove("smoveKey1", "smoveKey2", "non existing")
        m <- redis.smembers("smoveKey2")
      } yield {
        isMoved mustEqual true
        isNotMoved mustEqual false
        m must containTheSameElementsAs(Seq(ByteString("three"), ByteString("two")))
      }
      Await.result(r, timeOut)
    }

    "SPOP" in {
      val r = for {
        _ <- redis.del("spopKey")
        _ <- redis.sadd("spopKey", "one", "two", "three")
        pop <- redis.spop("spopKey")
        popNone <- redis.spop("spopKeyNonExisting")
        m <- redis.smembers("spopKey")
      } yield {
        Seq(ByteString("three"), ByteString("two"), ByteString("one")) must contain(equalTo(pop.get))
        popNone must beNone
        m must containAnyOf(Seq(ByteString("three"), ByteString("two"), ByteString("one")))
      }
      Await.result(r, timeOut)
    }

    "SRANDMEMBER" in {
      val r = for {
        _ <- redis.del("srandmemberKey")
        _ <- redis.sadd("srandmemberKey", "one", "two", "three")
        randmember <- redis.srandmember("srandmemberKey")
        randmember2 <- redis.srandmember("srandmemberKey", 2)
        randmemberNonExisting <- redis.srandmember("srandmemberKeyNonExisting", 2)
        m <- redis.smembers("spopKey")
      } yield {
        Seq(ByteString("three"), ByteString("two"), ByteString("one")) must contain(equalTo(randmember.get))
        randmember2 must have size 2
        randmemberNonExisting must beEmpty
      }
      Await.result(r, timeOut)
    }

    "SREM" in {
      val r = for {
        _ <- redis.del("sremKey")
        _ <- redis.sadd("sremKey", "one", "two", "three", "four")
        rem <- redis.srem("sremKey", "one", "four")
        remNothing <- redis.srem("sremKey", "five")
        m <- redis.smembers("sremKey")
      } yield {
        rem mustEqual 2
        remNothing mustEqual 0
        m must containTheSameElementsAs(Seq(ByteString("three"), ByteString("two")))
      }
      Await.result(r, timeOut)
    }

    "SSCAN" in {
      val r = for {
        _ <- redis.sadd("sscan", (1 to 20).map(_.toString):_*)
        scanResult <- redis.sscan[String]("sscan", count = Some(100))
      } yield {
        scanResult.index mustEqual 0
        scanResult.data.map(_.toInt).sorted mustEqual (1 to 20)
      }

      Await.result(r, timeOut)
    }

    "SUNION" in {
      val r = for {
        _ <- redis.del("sunionKey1")
        _ <- redis.del("sunionKey2")
        _ <- redis.sadd("sunionKey1", "a", "b", "c")
        _ <- redis.sadd("sunionKey2", "c", "d", "e")
        union <- redis.sunion("sunionKey1", "sunionKey2")
      } yield {
        union must containTheSameElementsAs(Seq(ByteString("a"), ByteString("b"), ByteString("c"), ByteString("d"), ByteString("e")))
      }
      Await.result(r, timeOut)
    }


    "SUNIONSTORE" in {
      val r = for {
        _ <- redis.del("sunionstoreKey1")
        _ <- redis.del("sunionstoreKey2")
        _ <- redis.sadd("sunionstoreKey1", "a", "b", "c")
        _ <- redis.sadd("sunionstoreKey2", "c", "d", "e")
        union <- redis.sunionstore("sunionstoreKeyDest", "sunionstoreKey1", "sunionstoreKey2")
        m <- redis.smembers("sunionstoreKeyDest")
      } yield {
        union mustEqual 5
        m must containTheSameElementsAs(Seq(ByteString("a"), ByteString("b"), ByteString("c"), ByteString("d"), ByteString("e")))
      }
      Await.result(r, timeOut)
    }
  }
}
