package redis.commands

import redis.RedisStandaloneServer

import scala.concurrent.Await

class HyperLogLogSpec extends RedisStandaloneServer {

  sequential

  "HyperLogLog commands" should {
    "PFADD" in {
      val r = redis.pfadd("hll", "a", "b", "c", "d", "e", "f", "g").flatMap(_ => {
        redis.pfcount("hll").flatMap(count => {
          count mustEqual 7
          redis.pfadd("hll", "h", "i").flatMap(_ ⇒ {
            redis.pfcount("hll")
          })
        })
      })
      Await.result(r, timeOut) mustEqual 9
    }

    "PFCOUNT" in {
      val r = redis.pfadd("hll2", "a", "b", "c", "d", "e", "f", "g").flatMap(_ => {
        redis.pfcount("hll2")
      })
      Await.result(r, timeOut) mustEqual 7
    }

    "PFMERGE" in {
      val r = redis.pfadd("hll3", "a", "b").flatMap(_ ⇒ {
        redis.pfadd("hll4", "c", "d").flatMap(_ ⇒ {
          redis.pfmerge("hll5", "hll4", "hll3").flatMap(merged ⇒ {
            merged mustEqual true
            redis.pfcount("hll5")
          })
        })
      })

      Await.result(r, timeOut) mustEqual 4
    }
  }
}
