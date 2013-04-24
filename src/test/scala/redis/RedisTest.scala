package redis

import scala.concurrent.Await
import scala.util.Success

class RedisTest extends RedisSpec {

  sequential

  "basic test" should {
    "ping" in {
      Await.result(redis.ping(), timeOut) mustEqual "PONG"
    }
    "set" in {
      import Redis.Convert._
      Await.result(redis.set("key", "value").map(_.toBoolean), timeOut) mustEqual true
      Await.result(redis.set("key", "value").map(_.asTry[String]), timeOut) mustEqual Success("OK")
    }
    "get" in {
      import Redis.Convert._
      Await.result(redis.get("key").map(_.asTry[String]), timeOut) mustEqual Success("value")
    }
    "del" in {
      Await.result(redis.del("key"), timeOut) mustEqual 1
    }
    "get not found" in {
      import Redis.Convert._
      Await.result(redis.get("key").map(_.asTry[String]), timeOut).isFailure mustEqual true //specs2 1.15 will have matchers for Try
    }
  }
}
