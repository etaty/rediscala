package redis.commands

import redis._
import scala.concurrent.Await

class ConnectionSpec extends RedisSpec {

  sequential

  import Redis.Convert._

  "Connection commands" should {
    "AUTH" in {
      Await.result(redis.auth("no password"), timeOut) must throwA[RedisReplyErrorException]("ERR Client sent AUTH, but no password is set")
    }
    "ECHO" in {
      val hello = "Hello World!"
      Await.result(redis.echo(hello), timeOut).asTry[String].get mustEqual hello
    }
    "PING" in {
      Await.result(redis.ping(), timeOut) mustEqual "PONG"
    }
    "QUIT" in {
      todo
    }
    "SELECT" in {
      Await.result(redis.select(1), timeOut) mustEqual true
      Await.result(redis.select(0), timeOut) mustEqual true
      Await.result(redis.select(-1), timeOut) must throwA[RedisReplyErrorException]("ERR invalid DB index")
      Await.result(redis.select(1000), timeOut) must throwA[RedisReplyErrorException]("ERR invalid DB index")
    }
  }
}
