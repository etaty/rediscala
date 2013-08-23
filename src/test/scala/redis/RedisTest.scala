package redis

import scala.concurrent._
import akka.util.ByteString

class RedisTest extends RedisSpec {

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

  "sentinel monitored test" should {
    "ping" in {
      Await.result(smRedis.redisClient.ping, timeOut) mustEqual "PONG"
    }
    "auto failover" in {
      val port = smRedis.redisClient.port
      Await.result(smRedis.redisClient.ping, timeOut) mustEqual "PONG"

      Await.result(sentinel.failover(masterName), timeOut) mustEqual true
      Await.result( future { Thread.sleep(15000) }, longTimeOut )

      Await.result(smRedis.redisClient.ping, timeOut) mustEqual "PONG"
      smRedis.redisClient.port mustNotEqual port

      Await.result( future { Thread.sleep(15000) }, longTimeOut )
      Await.result(sentinel.failover(masterName), timeOut) mustEqual true
      Await.result( future { Thread.sleep(15000) }, longTimeOut )

      Await.result(smRedis.redisClient.ping, timeOut) mustEqual "PONG"
      smRedis.redisClient.port mustEqual  port
    }
  }

  "sentinel test" should {
    "masters" in {
      val r = Await.result(sentinel.masters, timeOut)
      r(0)("name") mustEqual masterName
      r(0)("flags") mustEqual "master"
    }
    "no such master" in {
      Await.result(sentinel.getMasterAddr("no-such-master"), timeOut) match {
        case None => ok
        case _ => ko(s"unexpected: master with name '$masterName' was not supposed to be found")
      }
    }
    "unknown master state" in {
      Await.result(sentinel.isMasterDown("no.such.ip.address", 1234), timeOut) match {
        case None => ok
        case _ => ko(s"unexpected: master state should be unknown")
      }
    }
    "master ok" in {
      Await.result(sentinel.getMasterAddr(masterName), timeOut) match {
        case Some((ip, port)) =>
          Await.result(sentinel.isMasterDown(ip, port), timeOut) mustEqual Some(false)
        case _ =>
          ko(s"unexpected: master with name '$masterName' was not found")
      }
    }
    "slaves" in {
      val r = Await.result(sentinel.slaves(masterName), timeOut)
      r(0)("flags") mustNotEqual "master"
    }
    "reset bogus master" in {
      !Await.result(sentinel.resetMaster("no-such-master"), timeOut)
    }
    "reset master" in {
      Await.result(sentinel.resetMaster(masterName), timeOut)
    }
  }

}
