package redis

import scala.concurrent._

class RedisPoolSpec extends RedisHelper {

  sequential

  "basic pool test" should {
    "ok" in {
      val redisPool = RedisClientPool(Seq(RedisServer(db = Some(0)), RedisServer(db = Some(1)), RedisServer(db = Some(3))))

      val key = "keyPoolDb0"
      redisPool.set(key, 0)
      val r = for {
        getDb1 <- redisPool.get(key)
        getDb2 <- redisPool.get(key)
        getDb0 <- redisPool.get[String](key)
      } yield {
        getDb1 must beNone
        getDb2 must beNone
        getDb0 must beSome("0")
      }
      Await.result(r, timeOut)
    }
  }
}
