package redis

import scala.concurrent._
import akka.util.ByteString

class RedisTest extends RedisSpec {

  import RedisServerHelper._

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
      Await.result(clSmRedis.ping, timeOut) mustEqual "PONG"
    }
    "auto failover" in {
      val port = clSmRedis.redisClient.port
      Await.result(clSmRedis.ping, timeOut) mustEqual "PONG"

      Await.result(clSentinel.failover(masterName), timeOut) mustEqual true
      Await.result( future { Thread.sleep(20000) }, longTimeOut )

      Await.result(clSmRedis.ping, timeOut) mustEqual "PONG"
      clSmRedis.redisClient.port mustNotEqual port

      Await.result( future { Thread.sleep(20000) }, longTimeOut )
      Await.result(clSentinel.failover(masterName), timeOut) mustEqual true
      Await.result( future { Thread.sleep(20000) }, longTimeOut )

      Await.result(clSmRedis.ping, timeOut) mustEqual "PONG"
      clSmRedis.redisClient.port mustEqual  port
    }
  }

  "sentinel test" should {
    "masters" in {
      val r = Await.result(clSentinel.masters, timeOut)
      r(0)("name") mustEqual masterName
      r(0)("flags").startsWith("master") mustEqual true
    }
    "no such master" in {
      Await.result(clSentinel.getMasterAddr("no-such-master"), timeOut) match {
        case None => ok
        case _ => ko(s"unexpected: master with name '$masterName' was not supposed to be found")
      }
    }
    "unknown master state" in {
      Await.result(clSentinel.isMasterDown("no.such.ip.address", 1234), timeOut) match {
        case None => ok
        case _ => ko(s"unexpected: master state should be unknown")
      }
    }
    "master ok" in {
      Await.result(clSentinel.getMasterAddr(masterName), timeOut) match {
        case Some((ip, port)) =>
          Await.result(clSentinel.isMasterDown(ip, port), timeOut) mustEqual Some(false)
        case _ =>
          ko(s"unexpected: master with name '$masterName' was not found")
      }
    }
    "slaves" in {
      val r = Await.result(clSentinel.slaves(masterName), timeOut)
      r(0)("flags").startsWith("slave") mustEqual true
    }
    "reset bogus master" in {
      !Await.result(clSentinel.resetMaster("no-such-master"), timeOut)
    }
    "reset master" in {
      Await.result(clSentinel.resetMaster(masterName), timeOut)
    }
  }

}
