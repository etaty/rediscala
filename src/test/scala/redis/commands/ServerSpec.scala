package redis.commands

import redis._
import scala.concurrent.Await
import akka.util.ByteString
import redis.actors.ReplyErrorException

class ServerSpec extends RedisSpec {

  sequential

  "Server commands" should {

    // TODO launch a redis-server then run the tests

    "BGREWRITEAOF && BGSAVE" in {
      val r = for {
        bgrewriteaof <- redis.bgrewriteaof()
        bgsave <- {
          Thread.sleep(2000) // ERR Can't BGSAVE while AOF log rewriting is in progress
          redis.bgsave()
        }
      } yield {
        bgrewriteaof mustEqual "Background append only file rewriting started"
        bgsave mustEqual "Background saving started"
      }

      Await.result(r, timeOut)
    }

    "CLIENT KILL" in {
      Await.result(redis.clientKill("8.8.8.8", 53), timeOut) must throwA[ReplyErrorException]("ERR No such client")
    }

    "CLIENT LIST" in {
      Await.result(redis.clientList(), timeOut) must beAnInstanceOf[String]
    }

    "CLIENT GETNAME" in {
      Await.result(redis.clientGetname(), timeOut) mustEqual None
    }

    "CLIENT SETNAME" in {
      Await.result(redis.clientSetname("rediscala"), timeOut) mustEqual true
    }

    "CONFIG GET/SET" in {
      todo
    }

    "CONFIG RESETSTAT" in {
      todo
    }

    "DBSIZE" in {
      Await.result(redis.dbsize(), timeOut) must be_>=(0l)
    }

    "DEBUG OBJECT" in {
      todo
    }

    "DEBUG SEGFAULT" in {
      todo
    }

    "FLUSHALL" in {
      todo
    }

    "FLUSHDB" in {
      //TODO new redis connection / change db / set / flushdb
      todo
    }

    "INFO" in {
      val r = for {
        info <- redis.info()
        infoCpu <- redis.info("cpu")
      } yield {
        info must beAnInstanceOf[String]
        infoCpu must beAnInstanceOf[String]
      }
      Await.result(r, timeOut)
    }

    "LASTSAVE" in {
      Await.result(redis.lastsave(), timeOut) must be_>=(0l)
    }

    "SAVE" in {
      //Await.result(redis.save(), timeOut) mustEqual true
    }

    "SHUTDOWN" in {
      todo
    }

  }
}
