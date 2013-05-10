package redis.commands

import redis._
import scala.concurrent.Await
import akka.util.ByteString
import redis.actors.{NoConnectionException, ReplyErrorException}

class ConnectionSpec extends RedisSpec {

  sequential

  import Converter._

  "Connection commands" should {
    "AUTH" in {
      Await.result(redis.auth("no password"), timeOut) must throwA[ReplyErrorException]("ERR Client sent AUTH, but no password is set")
    }
    "ECHO" in {
      val hello = "Hello World!"
      Await.result(redis.echo(hello), timeOut) mustEqual Some(ByteString(hello))
    }
    "PING" in {
      Await.result(redis.ping(), timeOut) mustEqual "PONG"
    }
    "QUIT" in {
      val f = redis.quit()
      val fail = redis.ping()
      Await.result(f, timeOut) mustEqual true
      Await.result(fail, timeOut) must throwA[NoConnectionException.type]
      Await.result(redis.ping(), timeOut) mustEqual "PONG"
    }
    "SELECT" in {
      Await.result(redis.select(1), timeOut) mustEqual true
      Await.result(redis.select(0), timeOut) mustEqual true
      Await.result(redis.select(-1), timeOut) must throwA[ReplyErrorException]("ERR invalid DB index")
      Await.result(redis.select(1000), timeOut) must throwA[ReplyErrorException]("ERR invalid DB index")
    }
  }
}
