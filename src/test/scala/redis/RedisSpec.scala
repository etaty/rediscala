package redis

import org.specs2.mutable.{SpecificationLike, Tags}
import akka.util.Timeout
import org.specs2.time.NoTimeConversions
import akka.testkit.TestKit
import org.specs2.specification.{Step, Fragments}
import akka.actor.ActorSystem

class RedisSpec extends TestKit(ActorSystem()) with SpecificationLike with Tags with NoTimeConversions {

  import scala.concurrent._
  import scala.concurrent.duration._

  implicit val ec = ExecutionContext.Implicits.global

  implicit val timeout = Timeout(10 seconds)
  val timeOut = 10 seconds
  val redis = RedisClient()

  override def map(fs: => Fragments) = fs ^ Step(system.shutdown())
}
