package redis.commands

import redis._
import scala.concurrent.Await
import akka.util.ByteString
import redis.protocol.{Bulk, MultiBulk}
import redis.actors.ReplyErrorException
import redis.api.scripting.RedisScript

class ScriptingSpec extends RedisSpec {

  sequential

  "Scripting commands" should {
    val redisScript = RedisScript("return 'rediscala'")
    val redisScriptKeysArgs = RedisScript("return {KEYS[1],ARGV[1]}")

    "evalshaOrEval (RedisScript)" in {
      Await.result(redis.scriptFlush(), timeOut) must beTrue
      val r = Await.result(redis.evalshaOrEval(redisScriptKeysArgs, Seq("key"), Seq("arg")), timeOut)
      r mustEqual MultiBulk(Some(Seq(Bulk(Some(ByteString("key"))), Bulk(Some(ByteString("arg"))))))
    }

    "EVAL" in {
      Await.result(redis.eval(redisScript.script), timeOut) mustEqual Bulk(Some(ByteString("rediscala")))
    }

    "EVALSHA" in {
      Await.result(redis.evalsha(redisScript.sha1), timeOut) mustEqual Bulk(Some(ByteString("rediscala")))
    }

    "SCRIPT FLUSH" in {
      Await.result(redis.scriptFlush(), timeOut) must beTrue
    }

    "SCRIPT KILL" in {

      withRedisServer(port => {
        val redisKiller = RedisClient(port = port)
        val redisScriptLauncher = RedisClient(port = port)
        Await.result(redisKiller.scriptKill(), timeOut) must throwA(ReplyErrorException("NOTBUSY No scripts in execution right now."))

        // infinite script (5 seconds)
        val infiniteScript = redisScriptLauncher.eval(
          """
            |local i = 1
            |while(i > 0) do
            |end
            |return 0
          """.stripMargin)
        Thread.sleep(1000)
        redisKiller.scriptKill() must beTrue.await(retries = 3, timeOut)
        Await.result(infiniteScript, timeOut) must throwA(
          ReplyErrorException("ERR Error running script (call to f_2817d960235dc23d2cea9cc2c716a0b123b56be8): Script killed by user with SCRIPT KILL... "))
      })
    }

    "SCRIPT LOAD" in {
      Await.result(redis.scriptLoad("return 'rediscala'"), timeOut) mustEqual "d4cf7650161a37eb55a7e9325f3534cec6fc3241"
    }

    "SCRIPT EXISTS" in {
      val redisScriptNotFound = RedisScript("return 'SCRIPT EXISTS not found'")
      val redisScriptFound = RedisScript("return 'SCRIPT EXISTS found'")
      val scriptsLoaded = redis.scriptLoad(redisScriptFound.script).flatMap(_ =>
        redis.scriptExists(redisScriptFound.sha1, redisScriptNotFound.sha1)
      )
      Await.result(scriptsLoaded, timeOut) mustEqual Seq(true, false)

    }

  }
}
