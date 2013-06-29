package redis.bench

import scala.compat.Platform
import scala.concurrent._
import scala.concurrent.duration._
import redis.RedisSpec
import redis.protocol.RedisProtocolRequest
import akka.util.ByteString

class RedisBenchProtocol extends RedisSpec {


  "Rediscala stupid benchmark" should {
    "bench 1" in {
      val n = 200000
      for (i <- 1 to 10) yield {
        val ops = n * i / 10
        timed(s"ping $ops times (run $i)", ops) {
          val results = for (_ <- 1 to ops) yield {
            RedisProtocolRequest.multiBulk("INCR", Seq(ByteString("i")))
          }
        }
        Platform.collectGarbage()

      }
      true mustEqual true // TODO remove that hack for spec2

    } tag ("benchmark")
  }

  def timed(desc: String, n: Int)(benchmark: ⇒ Unit) {
    println("* " + desc)
    val start = System.currentTimeMillis
    benchmark
    val stop = System.currentTimeMillis
    val elapsedSeconds = (stop - start) / 1000.0
    val opsPerSec = n / elapsedSeconds

    println(s"* - number of ops/s: $opsPerSec ( $n ops in $elapsedSeconds)")
  }
}
