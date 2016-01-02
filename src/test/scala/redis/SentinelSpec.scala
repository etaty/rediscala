package redis

import scala.concurrent._
import scala.concurrent.duration._

class SentinelSpec extends RedisClusterClients {

  sequential

  "sentinel monitored test" should {


    "master auto failover" in {
      val port = sentinelMonitoredRedisClient.redisClient.port

      awaitAssert({
        sentinelMonitoredRedisClient.ping() must beEqualTo("PONG").await
      }, 10 seconds)
      sentinelClient.failover(masterName) must beTrue.await

      awaitAssert({
        sentinelMonitoredRedisClient.ping() must beEqualTo("PONG").await
        sentinelMonitoredRedisClient.redisClient.port mustNotEqual port
        sentinelMonitoredRedisClient.redisClient.port must beOneOf(slavePort1, slavePort2)
      }, 10 seconds)

      val firstFailover = sentinelMonitoredRedisClient.redisClient.port

      sentinelClient.failover(masterName) must beTrue.await
      awaitAssert({
        sentinelMonitoredRedisClient.ping() must beEqualTo("PONG").await.not
      }, 5 seconds)

      awaitAssert({
        sentinelMonitoredRedisClient.ping() must beEqualTo("PONG").await
        sentinelMonitoredRedisClient.redisClient.port mustNotEqual port
        sentinelMonitoredRedisClient.redisClient.port must beOneOf(slavePort1, slavePort2, masterPort)
      }, 10 seconds)
      sentinelMonitoredRedisClient.redisClient.port mustNotEqual firstFailover
    }

    "ping" in {
      Await.result(sentinelMonitoredRedisClient.ping(), timeOut) mustEqual "PONG"
      Await.result(redisClient.ping(), timeOut) mustEqual "PONG"
    }

    "sentinel nodes auto discovery" in {
      val sentinelCount = sentinelMonitoredRedisClient.sentinelClients.size
      val sentinel = newSentinelProcess()

      awaitAssert(sentinelMonitoredRedisClient.sentinelClients.size mustEqual sentinelCount + 1, 10 second)

      sentinel.destroy()
      awaitAssert({
        sentinelMonitoredRedisClient.sentinelClients.size mustEqual sentinelCount
      }, 10 seconds)
      success
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
