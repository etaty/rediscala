package redis

import scala.concurrent.Await
import scala.util.Success

class RedisTest extends RedisSpec {

  sequential

  import Converter._

  "basic test" should {
    "ping" in {
      Await.result(redis.ping(), timeOut) mustEqual "PONG"
    }
    "set" in {
      Await.result(redis.set("key", "value").map(_.toBoolean), timeOut) mustEqual true
      Await.result(redis.set("key", "value").map(_.asTry[String]), timeOut) mustEqual Success("OK")
    }
    "get" in {
      Await.result(redis.get("key").map(_.asTry[String]), timeOut) mustEqual Success("value")
    }
    "del" in {
      Await.result(redis.del("key"), timeOut) mustEqual 1
    }
    "get not found" in {
      Await.result(redis.get("key").map(_.asTry[String]), timeOut).isFailure mustEqual true //specs2 1.15 will have matchers for Try
    }
  }
}
