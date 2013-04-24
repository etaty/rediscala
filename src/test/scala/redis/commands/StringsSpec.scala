package redis.commands

import org.specs2.mutable.Specification
import redis.{RedisSpec, Common}
import scala.concurrent.Await

class StringsSpec extends RedisSpec {

  sequential

  "Strings commands" should {
    /*
    "APPEND" in {
      val c = redis.set("appendKey", "Hello").flatMap(_ => {
        redis.append("appendKey", " World")
      })
      Await.result(c, timeOut) mustEqual "Hello World".length
    }
    "BITCOUNT" in {
      val c = redis.set("bitcountKey", "foobar").flatMap(_ => {
        redis.append("appendKey", " World")
      })
      Await.result(c, timeOut) mustEqual 26
    }
    */
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
