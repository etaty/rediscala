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
      Await.result(f, timeOut) mustEqual true
      val fail = redis.ping()
      Await.result(fail, timeOut) must throwA[NoConnectionException.type]
      Thread.sleep(2000) // wait for redis to close the TCP connection
      redis.connect()
    }
    "SELECT" in {
      Await.result(redis.select(1), timeOut) mustEqual true
      Await.result(redis.select(0), timeOut) mustEqual true
      Await.result(redis.select(-1), timeOut) must throwA[ReplyErrorException]("ERR invalid DB index")
      Await.result(redis.select(1000), timeOut) must throwA[ReplyErrorException]("ERR invalid DB index")
    }
  }
}
