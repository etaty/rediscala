package redis

import redis.api.connection.Select

import scala.concurrent._
import scala.concurrent.duration._
class RedisPoolSpec extends RedisStandaloneServer {

  sequential

  "basic pool test" should {
    "ok" in {
      val redisPool = RedisClientPool(Seq(RedisServer(  port = port,db = Some(0)), RedisServer(  port = port,db = Some(1)), RedisServer(  port = port,db = Some(3))))
      val key = "keyPoolDb0"
      redisPool.set(key, 0)
      val r = for {
        getDb1 <- redisPool.get(key)
        getDb2 <- redisPool.get(key)
        getDb0 <- redisPool.get[String](key)
        select <- Future.sequence(redisPool.broadcast(Select(0)))
        getKey1 <- redisPool.get[String](key)
        getKey2 <- redisPool.get[String](key)
        getKey0 <- redisPool.get[String](key)
      } yield {
        getDb1 must beNone
        getDb2 must beNone
        getDb0 must beSome("0")
        select mustEqual Seq(true, true, true)
        getKey1 must beSome("0")
        getKey2 must beSome("0")
        getKey0 must beSome("0")
      }
      Await.result(r, timeOut)
    }
    
    "check status" in {
      val redisPool = RedisClientPool(Seq(RedisServer(  port = port,db = Some(0)), RedisServer(  port = port,db = Some(1)), RedisServer(port = 3333,db = Some(3))))
      val key = "keyPoolDb0"

      awaitAssert(redisPool.redisConnectionPool.size mustEqual 2,20 second)
      redisPool.set(key, 0)
      val r = for {
        getDb1 <- redisPool.get(key)
        getDb0 <- redisPool.get[String](key)
        select <- Future.sequence(redisPool.broadcast(Select(0)))
        getKey1 <- redisPool.get[String](key)
        getKey0 <- redisPool.get[String](key)
      } yield {
        getDb1 must beNone
        getDb0 must beSome("0")
        select mustEqual Seq(true, true)
        getKey1 must beSome("0")
        getKey0 must beSome("0")
      }
      Await.result(r, timeOut)

    }
  }
}
