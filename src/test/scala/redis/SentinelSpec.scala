package redis

import scala.concurrent._

class SentinelSpec extends RedisClusterClients {

  sequential

  "sentinel monitored test" should {
    "ping" in {
      Await.result(sentinelMonitoredRedisClient.ping(), timeOut) mustEqual "PONG"
      Await.result(redisClient.ping(), timeOut) mustEqual "PONG"
    }
    "auto failover" in {
      val port = sentinelMonitoredRedisClient.redisClient.port
      Await.result(sentinelMonitoredRedisClient.ping(), timeOut) mustEqual "PONG"
      Thread.sleep(10000)

      Await.result(sentinelClient.failover(masterName), timeOut) mustEqual true
      Thread.sleep(20000)

      Await.result(sentinelMonitoredRedisClient.ping(), timeOut) mustEqual "PONG"
      sentinelMonitoredRedisClient.redisClient.port mustNotEqual port
      sentinelMonitoredRedisClient.redisClient.port mustEqual slavePort

    ///*

      Thread.sleep(20000)
      Await.result(sentinelClient.failover(masterName), timeOut) mustEqual true

      Thread.sleep(20000)
      Await.result(sentinelMonitoredRedisClient.ping(), timeOut) mustEqual "PONG"
      sentinelMonitoredRedisClient.redisClient.port mustEqual port
    //*/
    }
  }

  "sentinel test" should {
    "masters" in {
      val r = Await.result(sentinelClient.masters(), timeOut)
      r(0)("name") mustEqual masterName
      r(0)("flags").startsWith("master") mustEqual true
    }
    "no such master" in {
      Await.result(sentinelClient.getMasterAddr("no-such-master"), timeOut) match {
        case None => ok
        case _ => ko(s"unexpected: master with name '$masterName' was not supposed to be found")
      }
    }
    "unknown master state" in {
      Await.result(sentinelClient.isMasterDown("no.such.ip.address", 1234), timeOut) match {
        case None => ok
        case _ => ko(s"unexpected: master state should be unknown")
      }
    }
    "master ok" in {
      Await.result(sentinelClient.getMasterAddr(masterName), timeOut) match {
        case Some((ip, port)) =>
          Await.result(sentinelClient.isMasterDown(ip, port), timeOut) mustEqual Some(false)
        case _ =>
          ko(s"unexpected: master with name '$masterName' was not found")
      }
    }
    "slaves" in {
      val r = Await.result(sentinelClient.slaves(masterName), timeOut)
      r must not be empty
      r(0)("flags").startsWith("slave") mustEqual true
    }
    "reset bogus master" in {
      !Await.result(sentinelClient.resetMaster("no-such-master"), timeOut)
    }
    "reset master" in {
      Await.result(sentinelClient.resetMaster(masterName), timeOut)
    }
  }

}
