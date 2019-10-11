package redis.commands

import redis._
import scala.concurrent.Await
import akka.util.ByteString
import redis.actors.ReplyErrorException

class ConnectionSpec extends RedisStandaloneServer {

  sequential

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
      // todo test that the TCP connection is reset.
      val f = redis.quit()
      Thread.sleep(1000)
      val ping = redis.ping()
      Await.result(f, timeOut) mustEqual true
      Await.result(ping, timeOut) mustEqual "PONG"
    }
    "SELECT" in {
      Await.result(redis.select(1), timeOut) mustEqual true
      Await.result(redis.select(0), timeOut) mustEqual true
      Await.result(redis.select(-1), timeOut) must throwA[ReplyErrorException]("ERR DB index is out of range")
      Await.result(redis.select(1000), timeOut) must throwA[ReplyErrorException]("ERR DB index is out of range")
    }
  }
}
