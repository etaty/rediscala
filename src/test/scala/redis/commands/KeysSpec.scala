package redis.commands

import redis._
import scala.concurrent.{Future, Await}
import akka.util.ByteString
import scala.util.Success
import scala.sys.process.Process
import redis.api._

class KeysSpec extends RedisSpec {

  "Keys commands" should {
    "DEL" in {
      val r = for {
        s <- redis.set("delKey", "value")
        d <- redis.del("delKey", "delKeyNonexisting")
      } yield {
        s mustEqual true
        d mustEqual 1
      }
      Await.result(r, timeOut)
    }
    "DUMP" in {
      val r = for {
        s <- redis.set("dumpKey", "value")
        d <- redis.dump("dumpKey")
      } yield {
        s mustEqual true
        d mustEqual Some(ByteString(0, 5, 118, 97, 108, 117, 101, 6, 0, 23, 27, -87, -72, 52, -1, -89, -3))
      }
      Await.result(r, timeOut)
    }

    "EXISTS" in {
      val r = for {
        s <- redis.set("existsKey", "value")
        e <- redis.exists("existsKey")
        e2 <- redis.exists("existsKeyNonexisting")
      } yield {
        s mustEqual true
        e mustEqual true
        e2 mustEqual false
      }
      Await.result(r, timeOut)
    }

    "EXPIRE" in {
      val r = for {
        s <- redis.set("expireKey", "value")
        e <- redis.expire("expireKey", 1)
        e2 <- redis.expire("expireKeyNonexisting", 1)
        expired <- {
          Thread.sleep(1000)
          redis.get("expireKey")
        }
      } yield {
        s mustEqual true
        e mustEqual true
        e2 mustEqual false
        expired mustEqual None
      }
      Await.result(r, timeOut)
    }

    "EXPIREAT" in {
      val r = for {
        s <- redis.set("expireatKey", "value")
        e <- redis.expireat("expireatKey", System.currentTimeMillis() / 1000)
        expired <- redis.get("expireatKey")
      } yield {
        s mustEqual true
        e mustEqual true
        expired mustEqual None
      }
      Await.result(r, timeOut)
    }

    "KEYS" in {
      val r = for {
        _ <- redis.set("keysKey", "value")
        _ <- redis.set("keysKey2", "value")
        k <- redis.keys("keysKey*")
        k2 <- redis.keys("keysKey?")
        k3 <- redis.keys("keysKeyNomatch")
      } yield {
        k must containTheSameElementsAs(Seq("keysKey2", "keysKey"))
        k2 must containTheSameElementsAs(Seq("keysKey2"))
        k3 must beEmpty
      }
      Await.result(r, timeOut)
    }

    "MIGRATE" in {
      import scala.concurrent.duration._

      withRedisServer(port => {
        val redisMigrate = RedisClient("localhost", port)
        val key = "migrateKey-" + System.currentTimeMillis()
        val r = for {
          _ <- redis.set(key, "value")
          m <- redis.migrate("localhost", port, key, 0, 10 seconds)
          get <- redisMigrate.get(key)
        } yield {
          m must beTrue
          get mustEqual Some(ByteString("value"))
        }
        Await.result(r, timeOut)
      })
    }

    "MOVE" in {
      val redisMove = RedisClient()
      val r = for {
        _ <- redis.set("moveKey", "value")
        _ <- redisMove.select(1)
        _ <- redisMove.del("moveKey")
        move <- redis.move("moveKey", 1)
        move2 <- redis.move("moveKey2", 1)
        get <- redisMove.get("moveKey")
        get2 <- redisMove.get("moveKey2")
      } yield {
        move must beTrue
        move2 must beFalse
        get mustEqual Some(ByteString("value"))
        get2 mustEqual None
      }
      Await.result(r, timeOut)
    }

    "OBJECT" in {
      "REFCOUNT" in {
        val r = for {
          _ <- redis.set("objectRefcount", "objectRefcountValue")
          ref <- redis.objectRefcount("objectRefcount")
          refNotFound <- redis.objectRefcount("objectRefcountNotFound")
        } yield {
          ref must beSome(1)
          refNotFound must beNone
        }
        Await.result(r, timeOut)
      }
      "IDLETIME" in {
        val r = for {
          _ <- redis.set("objectIdletime", "objectRefcountValue")
          time <- redis.objectIdletime("objectIdletime")
          timeNotFound <- redis.objectIdletime("objectIdletimeNotFound")
        } yield {
          time must beSome[Long]
          timeNotFound must beNone
        }
        Await.result(r, timeOut)
      }
      "ENCODING" in {
        val r = for {
          _ <- redis.set("objectEncoding", "objectRefcountValue")
          encoding <- redis.objectEncoding("objectEncoding")
          encodingNotFound <- redis.objectEncoding("objectEncodingNotFound")
        } yield {
          encoding must beSome[String]
          encodingNotFound must beNone
        }
        Await.result(r, timeOut)
      }
    }

    "PERSIST" in {
      val r = for {
        s <- redis.set("persistKey", "value")
        e <- redis.expire("persistKey", 10)
        ttl <- redis.ttl("persistKey")
        p <- redis.persist("persistKey")
        ttl2 <- redis.ttl("persistKey")
      } yield {
        s mustEqual true
        e mustEqual true
        ttl.toInt must beBetween(1, 10)
        p mustEqual true
        ttl2 mustEqual -1
      }
      Await.result(r, timeOut)
    }

    "PEXPIRE" in {
      val r = for {
        s <- redis.set("pexpireKey", "value")
        e <- redis.pexpire("pexpireKey", 1500)
        e2 <- redis.expire("pexpireKeyNonexisting", 1500)
        expired <- {
          Thread.sleep(1500)
          redis.get("pexpireKey")
        }
      } yield {
        s mustEqual true
        e mustEqual true
        e2 mustEqual false
        expired mustEqual None
      }
      Await.result(r, timeOut)
    }

    "PEXPIREAT" in {
      val r = for {
        s <- redis.set("pexpireatKey", "value")
        e <- redis.pexpireat("pexpireatKey", System.currentTimeMillis())
        expired <- redis.get("pexpireatKey")
      } yield {
        s mustEqual true
        e mustEqual true
        expired mustEqual None
      }
      Await.result(r, timeOut)
    }


    "PEXPIREAT" in {
      val r = for {
        s <- redis.set("pttlKey", "value")
        e <- redis.expire("pttlKey", 1)
        pttl <- redis.pttl("pttlKey")
      } yield {
        s mustEqual true
        e mustEqual true
        pttl.toInt must beBetween(1, 1000)
      }
      Await.result(r, timeOut)
    }

    "RANDOMKEY" in {
      val r = for {
        s <- redis.set("randomKey", "value") // could fail if database was empty
        s <- redis.randomkey()
      } yield {
        s must beSome
      }
      Await.result(r, timeOut)
    }

    "RENAME" in {
      val r = for {
        _ <- redis.del("renameNewKey")
        s <- redis.set("renameKey", "value")
        rename <- redis.rename("renameKey", "renameNewKey")
        renamedValue <- redis.get("renameNewKey")
      } yield {
        s mustEqual true
        rename mustEqual true
        renamedValue mustEqual Some(ByteString("value"))
      }
      Await.result(r, timeOut)
    }

    "RENAMENX" in {
      val r = for {
        _ <- redis.del("renamenxNewKey")
        s <- redis.set("renamenxKey", "value")
        s <- redis.set("renamenxNewKey", "value")
        rename <- redis.renamenx("renamenxKey", "renamenxNewKey")
        _ <- redis.del("renamenxNewKey")
        rename2 <- redis.renamenx("renamenxKey", "renamenxNewKey")
        renamedValue <- redis.get("renamenxNewKey")
      } yield {
        s mustEqual true
        rename mustEqual false
        rename2 mustEqual true
        renamedValue mustEqual Some(ByteString("value"))
      }
      Await.result(r, timeOut)
    }

    "RESTORE" in {
      val r = for {
        s <- redis.set("restoreKey", "value")
        dump <- redis.dump("restoreKey")
        _ <- redis.del("restoreKey")
        restore <- redis.restore("restoreKey", serializedValue = dump.get)
      } yield {
        s mustEqual true
        dump mustEqual Some(ByteString(0, 5, 118, 97, 108, 117, 101, 6, 0, 23, 27, -87, -72, 52, -1, -89, -3))
        restore mustEqual true
      }
      Await.result(r, timeOut)
    }

    "SCAN" in {

      withRedisServer(port => {
        val scanRedis = RedisClient("localhost", port)

        val r = for {
          _ <- scanRedis.set("scanKey1", "value1")
          _ <- scanRedis.set("scanKey2", "value2")
          _ <- scanRedis.set("scanKey3", "value3")
          result <- scanRedis.scan(count = Some(1000))
        } yield {
          result.index mustEqual 0
          result.data.sorted mustEqual Seq("scanKey1", "scanKey2", "scanKey3")
        }
        Await.result(r, timeOut)
      })
    }

    // @see https://gist.github.com/jacqui/983051
    "SORT" in {
      val init = Future.sequence(Seq(
        redis.hset("bonds|1", "bid_price", 96.01),
        redis.hset("bonds|1", "ask_price", 97.53),
        redis.hset("bonds|2", "bid_price", 95.50),
        redis.hset("bonds|2", "ask_price", 98.25),
        redis.del("bond_ids"),
        redis.sadd("bond_ids", 1),
        redis.sadd("bond_ids", 2),
        redis.del("sortAlpha"),
        redis.rpush("sortAlpha", "abc", "xyz")
      ))
      val r = for {
        _ <- init
        sort <- redis.sort("bond_ids")
        sortDesc <- redis.sort("bond_ids", order = Some(DESC))
        sortAlpha <- redis.sort("sortAlpha", alpha = true)
        sortLimit <- redis.sort("bond_ids", limit = Some(LimitOffsetCount(0, 1)))
        b1 <- redis.sort("bond_ids", byPattern = Some("bonds|*->bid_price"))
        b2 <- redis.sort("bond_ids", byPattern = Some("bonds|*->bid_price"), getPatterns = Seq("bonds|*->bid_price"))
        b3 <- redis.sort("bond_ids", Some("bonds|*->bid_price"), getPatterns = Seq("bonds|*->bid_price", "#"))
        b4 <- redis.sort("bond_ids", Some("bonds|*->bid_price"), Some(LimitOffsetCount(0, 1)))
        b5 <- redis.sort("bond_ids", Some("bonds|*->bid_price"), order = Some(DESC))
        b6 <- redis.sort("bond_ids", Some("bonds|*->bid_price"))
        b7 <- redis.sortStore("bond_ids", Some("bonds|*->ask_price"), store = "bond_ids_sorted_by_ask_price")
      } yield {
        sort mustEqual Seq(ByteString("1"), ByteString("2"))
        sortDesc mustEqual Seq(ByteString("2"), ByteString("1"))
        sortAlpha mustEqual Seq(ByteString("abc"), ByteString("xyz"))
        sortLimit mustEqual Seq(ByteString("1"))
        b1 mustEqual Seq(ByteString("2"), ByteString("1"))
        b2 mustEqual Seq(ByteString("95.5"), ByteString("96.01"))
        b3 mustEqual Seq(ByteString("95.5"), ByteString("2"), ByteString("96.01"), ByteString("1"))
        b4 mustEqual Seq(ByteString("2"))
        b5 mustEqual Seq(ByteString("1"), ByteString("2"))
        b6 mustEqual Seq(ByteString("2"), ByteString("1"))
        b7 mustEqual 2
      }
      Await.result(r, timeOut)
    }

    "TTL" in {
      val r = for {
        s <- redis.set("ttlKey", "value")
        e <- redis.expire("ttlKey", 10)
        ttl <- redis.ttl("ttlKey")
      } yield {
        s mustEqual true
        e mustEqual true
        ttl.toInt must beBetween(1, 10)
      }
      Await.result(r, timeOut)
    }

    "TYPE" in {
      val r = for {
        s <- redis.set("typeKey", "value")
        _type <- redis.`type`("typeKey")
        _typeNone <- redis.`type`("typeKeyNonExisting")
      } yield {
        s mustEqual true
        _type mustEqual "string"
        _typeNone mustEqual "none"
      }
      Await.result(r, timeOut)
    }

  }
}
