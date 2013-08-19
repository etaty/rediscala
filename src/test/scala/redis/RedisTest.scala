package redis

import scala.concurrent.Await
import akka.util.ByteString
import redis.api.strings.Get

class RedisTest extends RedisSpec {

  sequential

  "basic test" should {
    "ping" in {
      Await.result(redis.ping(), timeOut) mustEqual "PONG"
    }
    "set" in {
      Await.result(redis.set("key", "value"), timeOut) mustEqual true
    }
    "get" in {
      Await.result(redis.get("key"), timeOut) mustEqual Some(ByteString("value"))
    }
    "del" in {
      Await.result(redis.del("key"), timeOut) mustEqual 1
    }
    "get not found" in {
      Await.result(redis.get("key"), timeOut) mustEqual None
    }
  }
}
