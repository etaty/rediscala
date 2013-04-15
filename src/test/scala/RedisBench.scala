import akka.actor.ActorSystem
import akka.util.Timeout
import org.specs2.mutable.{Specification, Tags}
import redis.{Status, RedisClient}
import scala.compat.Platform
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Await}
import ExecutionContext.Implicits.global

class RedisBench extends Specification with Tags {

  "Rediscala stupid benchmark" should {
    "bench 1" in {
      implicit val timeout = Timeout(FiniteDuration(5, "s"))
      implicit val actorSystem = ActorSystem("BadShakespearean")
      val redis = new RedisClient()

      val n = 100000
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
      }
      println("shutdown!!")
      actorSystem.shutdown()
    } tag ("benchmark")
  }
}
