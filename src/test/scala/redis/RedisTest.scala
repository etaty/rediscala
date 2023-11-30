package redis

import org.apache.pekko.ConfigurationException

import scala.concurrent._
import org.apache.pekko.util.ByteString

class RedisTest extends RedisStandaloneServer {

  sequential

  "basic test" should {
    "ping" in {
      Await.result(redis.ping, timeOut) mustEqual "PONG"
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

  "init connection test" should {
    "ok" in {
      withRedisServer(port => {
        val redis = RedisClient(port = port)
        // TODO set password (CONFIG SET requiredpass password)
        val r = for {
          _ <- redis.select(2)
          _ <- redis.set("keyDbSelect", "2")
        } yield {
          val redis = RedisClient(port = port, password = Some("password"), db = Some(2))
          Await.result(redis.get[String]("keyDbSelect"), timeOut) must beSome("2")
        }
        Await.result(r, timeOut)
      })
    }
    "use custom dispatcher" in {
      def test() = withRedisServer(port => {
        implicit val redisDispatcher = RedisDispatcher("no-this-dispatcher")
        RedisClient(port = port)
      })
      test must throwA[ConfigurationException]
    }
  }

}
