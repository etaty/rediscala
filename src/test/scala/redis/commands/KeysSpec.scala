package redis.commands

import org.specs2.mutable.Specification
import redis._

class KeysSpec extends RedisSpec {

  sequential

  "basic test" should {
    "del" in {
      //Await.result(redis.ping(), timeOut) mustEqual Status("PONG")
    }
    "dump" in {
      //Await.result(redis.set("key", "value"), timeOut) mustEqual true
    }
    "exists" in {
      //Await.result(redis.get("key"), timeOut) mustEqual Bulk(Some(ByteString("value")))
    }
    "expire" in {
      //Await.result(redis.del("key"), timeOut) mustEqual Integer(1)
    }
    "expireat" in {
      //redis.get("key").map(x => println(x))
      //Await.result(redis.get("key"), timeOut) mustEqual Bulk(None)
    }
  }
}
