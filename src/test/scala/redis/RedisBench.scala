package redis

import scala.compat.Platform
import scala.concurrent._
import scala.concurrent.duration._

class RedisBench extends RedisSpec {

  "Rediscala stupid benchmark" should {
    "bench 1" in {
      val n = 200000
      for (i <- 1 to 10) yield {
        timed(s"ping $n times (run $i)", n) {
          val results = for (i <- 1 to n) yield {
            redis.ping()
            //redis.set("mykey", "myvalue") //.map(x => println(x))
            //redis.get("mykey").map(x => println(x))
          }
          Await.result(Future.sequence(results), FiniteDuration(30, "s"))
        }
        Platform.collectGarbage()

      }
      true mustEqual true // TODO remove that hack for spec2

    } tag ("benchmark")
  }

  def timed(desc: String, n: Int)(benchmark: ⇒ Unit) {
    println("* " + desc)
    val t = System.currentTimeMillis
    benchmark
    val d = System.currentTimeMillis - t

    println("* - number of ops/s: " + n / (d / 1000.0) + "\n")
  }
}
