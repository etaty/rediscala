import akka.actor.ActorSystem
import akka.util.{ByteString, Timeout}
import org.specs2.mutable.Specification
import redis.{Integer, Bulk, Status, RedisClient}
import scala.compat.Platform
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Await}
import ExecutionContext.Implicits.global

class RedisTest extends Specification {

  implicit val timeout = Timeout(FiniteDuration(5, "s"))
  val timeOut = Duration("5s")
  implicit val actorSystem = ActorSystem("BadShakespearean")
  val redis = new RedisClient()

  sequential

  "basic test" should {
    "ping" in {
      Await.result(redis.ping(), timeOut) mustEqual Status("PONG")
    }
    "set" in {
      Await.result(redis.set("key", "value"), timeOut) mustEqual true
    }
    "get" in {
      Await.result(redis.get("key"), timeOut) mustEqual Bulk(Some(ByteString("value")))
    }
    "del" in {
      Await.result(redis.del("key"), timeOut) mustEqual Integer(1)
    }
    "get not found" in {
      redis.get("key").map(x => println(x.response))
      Await.result(redis.get("key"), timeOut) mustEqual Bulk(None)
    }
  }
  step(actorSystem.shutdown())
}
