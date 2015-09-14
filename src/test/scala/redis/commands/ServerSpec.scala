package redis.commands

import redis._
import scala.concurrent.Await
import redis.actors.{InvalidRedisReply, ReplyErrorException}
import redis.api.NOSAVE

class ServerSpec extends RedisStandaloneServer {

  sequential

  "Server commands" should {

    "BGSAVE" in {
      Await.result(redis.bgsave(), timeOut) mustEqual "Background saving started"
    }

    "CLIENT KILL" in {
      Await.result(redis.clientKill("8.8.8.8", 53), timeOut) must throwA[ReplyErrorException]("ERR No such client")
    }

    "CLIENT LIST" in {
      val list = Await.result(redis.clientList(), timeOut)
      list must beAnInstanceOf[Seq[Map[String, String]]]
      list must not beEmpty
    }

    "CLIENT GETNAME" in {
      Await.result(redis.clientGetname(), timeOut) mustEqual None
    }

    "CLIENT SETNAME" in {
      Await.result(redis.clientSetname("rediscala"), timeOut) mustEqual true
    }

    "CONFIG GET" in {
      val map = Await.result(redis.configGet("*"), timeOut)
      map must beAnInstanceOf[Map[String, String]]
      map must not beEmpty

    }
    "CONFIG SET" in {
      val r = for {
        set <- redis.configSet("loglevel", "warning")
        loglevel <- redis.configGet("loglevel")
      } yield {
        set must beTrue
        loglevel.get("loglevel") must beSome("warning")
      }
      Await.result(r, timeOut)
    }

    "CONFIG RESETSTAT" in {
      Await.result(redis.configResetstat(), timeOut) must beTrue
    }

    "DBSIZE" in {
      Await.result(redis.dbsize(), timeOut) must be_>=(0l)
    }

    "DEBUG OBJECT" in {
      Await.result(redis.debugObject("serverDebugObj"), timeOut) must throwA[ReplyErrorException]("ERR no such key")
    }

    "DEBUG SEGFAULT" in {
      todo
    }

    "FLUSHALL" in {
      Await.result(redis.flushall(), timeOut) must beTrue
    }

    "FLUSHDB" in {
      Await.result(redis.flushdb(), timeOut) must beTrue
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
      Await.result(redis.save(), timeOut) must beTrue or throwA(ReplyErrorException("ERR Background save already in progress"))
    }

    "SLAVE OF" in {
      Await.result(redis.slaveof("server", 12345), timeOut) must beTrue
    }

    "SLAVE OF NO ONE" in {
      Await.result(redis.slaveofNoOne(), timeOut) must beTrue
    }

    "TIME" in {
      val result = Await.result(redis.time(), timeOut)
      result must beAnInstanceOf[Tuple2[Long, Long]].setMessage(result.toString())
    }

    "BGREWRITEAOF" in {
      // depending on the redis version, this string could vary, redis 2.8.21 says 'scheduled'
      // but redis 2.8.18 says 'started'
      val r = Await.result(redis.bgrewriteaof(), timeOut)
      (r mustEqual "Background append only file rewriting started") or
        (r mustEqual "Background append only file rewriting scheduled")
    }

    "SHUTDOWN" in {
      Await.result(redis.shutdown(), timeOut) must throwA(InvalidRedisReply)
    }

    "SHUTDOWN (with modifier)" in {
      withRedisServer(port => {
        val redis = RedisClient(port = port)
        Await.result(redis.shutdown(NOSAVE), timeOut) must throwA(InvalidRedisReply)
      })
    }

  }
}
