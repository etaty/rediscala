package redis

import scala.compat.Platform
import scala.concurrent._
import scala.concurrent.duration._

class RedisBench extends RedisSpec {

  import Converter._

  "Rediscala stupid benchmark" should {
    "bench 1" in {
      val n = 200000
      for (i <- 1 to 10) yield {
        redis.set("i", "0")
        val ops = n * i / 10
        timed(s"ping $ops times (run $i)", ops) {
          val results = for (_ <- 1 to ops) yield {
            redis.ping()
            //redis.incr("i")
            //redis.get("i")
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
    val start = System.nanoTime()
    benchmark
    val stop = System.nanoTime()
    val elapsedSeconds = (stop - start) / (1000.0 * 1000 * 1000)
    val opsPerSec = n / elapsedSeconds

    println(s"* - number of ops/s: $opsPerSec ( $n ops in $elapsedSeconds)")
  }
}
