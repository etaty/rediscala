package redis

import redis.RedisServerHelper.redisHost

import scala.concurrent._
import scala.concurrent.duration._
class SentinelMutablePoolSpec extends RedisSentinelClients("SentinelMutablePoolSpec") {

  sequential

 var redisPool:RedisClientMutablePool = null

  override def setup(): Unit = {
    super.setup()
    redisPool = RedisClientMutablePool(Seq(RedisServer(redisHost, slavePort1)),masterName)
  }

  "mutable pool" should {
    "add remove" in {
      Thread.sleep(1000)
      redisPool.redisConnectionPool.size mustEqual 1

      redisPool.addServer(RedisServer(redisHost,slavePort2))
      redisPool.addServer(RedisServer(redisHost,slavePort2))
      Thread.sleep(5000)
      redisPool.redisConnectionPool.size mustEqual 2

      val key = "keyPoolDb0"
      val r = redisClient.set(key, "hello")

      Await.result(r, timeOut)
      within(500 millisecond) {
        Await.result(redisPool.get[String](key), timeOut) must beSome("hello")
        Await.result(redisPool.get[String](key), timeOut) must beSome("hello")
      }

      within(1 second) {
        redisPool.removeServer(RedisServer(redisHost, slavePort2))
      }


      awaitAssert(redisPool.redisConnectionPool.size mustEqual 1,5 second)


      Await.result(redisPool.get[String](key), timeOut) must beSome("hello")
      Await.result(redisPool.get[String](key), timeOut) must beSome("hello")

    }
  }

}

class SentinelMonitoredRedisClientMasterSlavesSpec extends RedisSentinelClients("SentinelMonitoredRedisClientMasterSlavesSpec") {

sequential
lazy val redisMasterSlavesPool =
      SentinelMonitoredRedisClientMasterSlaves( master = masterName,
                                   sentinels = sentinelPorts.map((redisHost, _)))
 "sentinel slave pool" should {
    "add and remove" in {
      if (scala.util.Properties.versionNumberString.startsWith("2.13")) {
        throw new org.specs2.execute.PendingException(org.specs2.execute.Pending("TODO"))
      } else {
        Thread.sleep(10000)
        Await.result(redisMasterSlavesPool.set("test", "value"), timeOut)
        awaitAssert(redisMasterSlavesPool.slavesClients.redisConnectionPool.size mustEqual 2,20 second)

        val newSlave =  newSlaveProcess()

        awaitAssert(redisMasterSlavesPool.slavesClients.redisConnectionPool.size mustEqual 3,20 second)
        newSlave.stop()

        Await.result(redisMasterSlavesPool.get[String]("test"),timeOut) mustEqual Some("value")
        slave1.stop()
        slave2.stop()

        awaitAssert(  redisMasterSlavesPool.slavesClients.redisConnectionPool.size mustEqual 0,20 second)
        Await.result(redisMasterSlavesPool.get[String]("test"), timeOut) mustEqual Some("value")
        newSlaveProcess()
        //println("************************** newSlaveProcess "+RedisServerHelper.portNumber.get())

        //within(30 second) {
        awaitAssert(  redisMasterSlavesPool.slavesClients.redisConnectionPool.size mustEqual 1,20 second)
        redisMasterSlavesPool.slavesClients.redisConnectionPool.size mustEqual 1
        //}
      }
    }
/*
   "changemaster" in {
     Try(Await.result(redisMasterSlavesPool.masterClient.shutdown(), timeOut))
       awaitAssert( redisMasterSlavesPool.slavesClients.redisConnectionPool.size mustEqual 0, 20 second )
       Await.result(redisMasterSlavesPool.get[String]("test"), timeOut) mustEqual Some("value")
   }*/

  }
}
