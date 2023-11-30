package redis.commands

import java.io.File

import redis._

import scala.concurrent.Await
import org.apache.pekko.util.ByteString
import org.specs2.concurrent.ExecutionEnv
import redis.protocol.{Bulk, MultiBulk}
import redis.actors.ReplyErrorException
import redis.api.scripting.RedisScript

class ScriptingSpec(implicit ee: ExecutionEnv) extends RedisStandaloneServer {

  sequential

  "Scripting commands" should {
    val redisScript = RedisScript("return 'rediscala'")
    val redisScriptKeysArgs = RedisScript("return {KEYS[1],ARGV[1]}")
    val redisScriptConversionObject = RedisScript("return redis.call('get', 'dumbKey')")

    "evalshaOrEval (RedisScript)" in {
      Await.result(redis.scriptFlush(), timeOut) must beTrue
      val r = Await.result(redis.evalshaOrEval(redisScriptKeysArgs, Seq("key"), Seq("arg")), timeOut)
      r mustEqual MultiBulk(Some(Vector(Bulk(Some(ByteString("key"))), Bulk(Some(ByteString("arg"))))))
    }

    "EVAL" in {
      Await.result(redis.eval(redisScript.script), timeOut) mustEqual Bulk(Some(ByteString("rediscala")))
    }

    "EVAL with type conversion" in {
      val dumbObject = new DumbClass("foo", "bar")
      val r = redis.set("dumbKey", dumbObject).flatMap(_ => {
        redis.eval[DumbClass](redisScriptConversionObject.script)
      })

      Await.result(r, timeOut) mustEqual dumbObject
    }

    "EVALSHA" in {
      Await.result(redis.evalsha(redisScript.sha1), timeOut) mustEqual Bulk(Some(ByteString("rediscala")))
    }

    "EVALSHA with type conversion" in {
      val dumbObject = new DumbClass("foo2", "bar2")
      val r = redis.set("dumbKey", dumbObject).flatMap(_ => {
        redis.evalsha[DumbClass](redisScriptConversionObject.sha1)
      })

      Await.result(r, timeOut) mustEqual dumbObject
    }

    "evalshaOrEvalForTypeOf (RedisScript)" in {
      Await.result(redis.scriptFlush(), timeOut) must beTrue
      val dumbObject = new DumbClass("foo3", "bar3")

      val r = redis.set("dumbKey", dumbObject).flatMap(_ => {
        redis.evalshaOrEval[DumbClass](redisScriptConversionObject)
      })

      Await.result(r, timeOut) mustEqual dumbObject
    }

    "SCRIPT FLUSH" in {
      Await.result(redis.scriptFlush(), timeOut) must beTrue
    }

    "SCRIPT KILL" in {

      withRedisServer(serverPort => {
        val redisKiller = RedisClient(port = serverPort)
        val redisScriptLauncher = RedisClient(port = serverPort)
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
          ReplyErrorException("ERR Error running script (call to f_2817d960235dc23d2cea9cc2c716a0b123b56be8): @user_script:3: Script killed by user with SCRIPT KILL... "))
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

    "fromFile" in {
      val testScriptFile = new File(getClass.getResource("/lua/test.lua").getPath)
      RedisScript.fromFile(testScriptFile) mustEqual RedisScript("""return "test"""")
    }

    "fromResource" in {
      val testScriptPath = "/lua/test.lua"
      RedisScript.fromResource(testScriptPath) mustEqual RedisScript("""return "test"""")
    }

  }
}
