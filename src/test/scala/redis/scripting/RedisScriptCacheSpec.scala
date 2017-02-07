package redis.scripting


import redis._
import redis.api.scripting.RedisScript
import scala.collection.mutable


class RedisScriptCacheSpec extends RedisSpec {
  sequential

  /*
  Mock class for reaching into the protected cache map
   */
  class TestRedisScriptCache(mockCache: mutable.Map[String, RedisScript]) extends RedisScriptCache(redis) {
    override protected val scriptCache = mockCache
  }

  "RedisScriptCache" should {
    "translate a script name to its appropriate path" in {
      val mockCache = mutable.Map.empty[String, RedisScript]
      val scriptCache = new TestRedisScriptCache(mockCache)
      val scriptName = "test1"
      val expectedScriptPath = s"/lua/$scriptName.lua"

      scriptCache.nameToPath(scriptName) mustEqual expectedScriptPath
    }

    "load a lua script and cache its name" in {
      val mockCache = mutable.Map.empty[String, RedisScript]
      val scriptCache = new TestRedisScriptCache(mockCache)

      val script1Name = "test1"
      val script2Name = "test2"
      val testScript2Path = s"/lua/$script2Name.lua"

      scriptCache.loadByName(script1Name)
      scriptCache.loadByPath(testScript2Path)

      mockCache(script1Name) must beAnInstanceOf[RedisScript]
      mockCache(script2Name) must beAnInstanceOf[RedisScript]
    }

    "register all scripts in a directory" in {
      val mockCache = mutable.Map.empty[String, RedisScript]
      val scriptCache = new TestRedisScriptCache(mockCache)

      scriptCache.registerScripts()
      val script1Name = "test1"
      val script2Name = "test2"

      mockCache(script1Name) must beAnInstanceOf[RedisScript]
      mockCache(script2Name) must beAnInstanceOf[RedisScript]
    }
  }
}

