package redis

import scala.concurrent._

class SentinelSpec extends RedisClusterClients {

  sequential

  "sentinel monitored test" should {
    "ping" in {
      Await.result(sentinelMonitoredRedisClient.ping(), timeOut) mustEqual "PONG"
      Await.result(redisClient.ping(), timeOut) mustEqual "PONG"
    }
    "master auto failover" in {
      val port = sentinelMonitoredRedisClient.redisClient.port
      Await.result(sentinelMonitoredRedisClient.ping(), timeOut) mustEqual "PONG"
      Thread.sleep(10000)

      Await.result(sentinelClient.failover(masterName), timeOut) mustEqual true
      Thread.sleep(20000)

      Await.result(sentinelMonitoredRedisClient.ping(), timeOut) mustEqual "PONG"
      sentinelMonitoredRedisClient.redisClient.port mustNotEqual port
      (sentinelMonitoredRedisClient.redisClient.port == slavePort1) ||
       (sentinelMonitoredRedisClient.redisClient.port == slavePort2 )  mustEqual true

    ///*

      Thread.sleep(20000)
      Await.result(sentinelClient.failover(masterName), timeOut) mustEqual true

      Thread.sleep(20000)
      Await.result(sentinelMonitoredRedisClient.ping(), timeOut) mustEqual "PONG"
        (sentinelMonitoredRedisClient.redisClient.port == slavePort1) ||
        (sentinelMonitoredRedisClient.redisClient.port == slavePort2 )  mustEqual true

    }
    "sentinel nodes auto discovery" in {
      val sentinelCount = sentinelMonitoredRedisClient.sentinelClients.size
      val sentinel = newSentinelProcess()

      Thread.sleep(10000)
      val sentinelCount2 = sentinelMonitoredRedisClient.sentinelClients.size

      sentinel.destroy()
      Thread.sleep(10000)
      val sentinelCount3 = sentinelMonitoredRedisClient.sentinelClients.size

      sentinelCount2 mustEqual (sentinelCount + 1)
      sentinelCount3 mustEqual sentinelCount
    }
  }

  "sentinel test" should {
    "masters" in {
      val r = Await.result(sentinelClient.masters(), timeOut)
      r(0)("name") mustEqual masterName
      r(0)("flags").startsWith("master") mustEqual true
    }
    "no such master" in {
      val opt = Await.result(sentinelClient.getMasterAddr("no-such-master"), timeOut)
      opt must beNone.setMessage(s"unexpected: master with name '$masterName' was not supposed to be found")
    }
    "unknown master state" in {
      val opt = Await.result(sentinelClient.isMasterDown("no-such-master"), timeOut)
      opt must beNone.setMessage("unexpected: master state should be unknown")
    }
    "master ok" in {
      Await.result(sentinelClient.isMasterDown(masterName), timeOut) must beSome(false).setMessage(s"unexpected: master with name '$masterName' was not found")
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
