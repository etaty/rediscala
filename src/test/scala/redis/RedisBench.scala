package redis

import org.specs2.mutable.{Specification, Tags}
import scala.compat.Platform
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import akka.util.Timeout
import org.specs2.time.NoTimeConversions

class RedisBench extends Specification with Tags with NoTimeConversions {

  import Common._

  "Rediscala stupid benchmark" should {
    "bench 1" in {

      val n = 10000
      for (i <- 1 to 10) yield {
        val t = System.currentTimeMillis
        println("start")

        val results = for (i <- 1 to n) yield {
          redis.ping()
          //redis.set("mykey", "myvalue") //.map(x => println(x))
          //redis.get("mykey").map(x => println(x))
        }

        Await.result(Future.sequence(results), FiniteDuration(10, "s"))
        val d = System.currentTimeMillis - t
        println("* - number of ops/s: " + n / (d / 1000.0) + "\n")
        Platform.collectGarbage()

        true mustEqual true // TODO remove that hack for spec2
      }

    } tag ("benchmark")
  }
  //step(actorSystem.shutdown())
}
