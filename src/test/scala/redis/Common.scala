package redis

import akka.util.Timeout


object Common {

  import scala.concurrent._
  import scala.concurrent.duration._
  import akka.actor.ActorSystem

  implicit val ec = ExecutionContext.Implicits.global

  implicit val timeout = Timeout(10 seconds)
  val timeOut = 10 seconds
  implicit lazy val actorSystem = ActorSystem("rediscala")
  lazy val redis = new RedisClient()
}