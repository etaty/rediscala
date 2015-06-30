package redis

import scala.concurrent._
import scala.util.Try
class SentinelMutablePoolSpec extends RedisClusterClients {

  sequential

 lazy val redisPool = RedisClientMutablePool(Seq(RedisServer(redisHost, slavePort1)),masterName)

  "mutable pool" should {
    "add remove" in {

      println("init " + redisPool.redisConnectionPool.size)
      redisPool.redisConnectionPool.size mustEqual 1

      redisPool.addServer(RedisServer(redisHost,slavePort2))
      redisPool.addServer(RedisServer(redisHost,slavePort2))

      redisPool.redisConnectionPool.size mustEqual 2

      val key = "keyPoolDb0"
      val r = redisClient.set(key, "hello")

      Await.result(r, timeOut)
      Await.result(redisPool.get[String](key), timeOut) must beSome("hello")
      Await.result(redisPool.get[String](key), timeOut) must beSome("hello")
      Thread.sleep(1000)
      println("OK")


      Thread.sleep(1000)
      redisPool.removeServer(RedisServer(redisHost,slavePort2))
      redisPool.redisConnectionPool.size mustEqual 1

      Await.result(redisPool.get[String](key), timeOut) must beSome("hello")
      Await.result(redisPool.get[String](key), timeOut) must beSome("hello")

    }
  }

}

class SentinelMonitoredRedisClientMasterSlavesSpec extends RedisClusterClients {

sequential
lazy val redisMasterSlavesPool =
      SentinelMonitoredRedisClientMasterSlaves( master = masterName,
                                   sentinels = sentinelPorts.map((redisHost, _)))
 "sentienl slave pool" should {
    "add and remove" in {
      Thread.sleep(5000)
      Await.result(redisMasterSlavesPool.set("test","value"),timeOut)

      println("*****1 " + redisMasterSlavesPool.slavesClients.redisClients)
      redisMasterSlavesPool.slavesClients.redisConnectionPool.size mustEqual 2

      val newSlave =  newSlaveProcess()

     Thread.sleep(20000)
     println("*****2 " + redisMasterSlavesPool.slavesClients.redisClients)
     redisMasterSlavesPool.slavesClients.redisConnectionPool.size mustEqual 3
     newSlave.destroy()

     Await.result(redisMasterSlavesPool.get[String]("test"),timeOut) mustEqual Some("value")
     slave1.destroy()
     slave2.destroy()
     Thread.sleep(20000)

     println("*****3 " + redisMasterSlavesPool.slavesClients.redisClients)
     redisMasterSlavesPool.slavesClients.redisConnectionPool.size mustEqual 0
     Await.result(redisMasterSlavesPool.get[String]("test"),timeOut) mustEqual Some("value")
     newSlaveProcess()
     Thread.sleep(20000)
     redisMasterSlavesPool.slavesClients.redisConnectionPool.size mustEqual 1

    }

   "changemaster" in {
     Try(Await.result(redisMasterSlavesPool.masterClient.shutdown(), timeOut))
     Thread.sleep(20000)

     println("*****1 " + redisMasterSlavesPool.slavesClients.redisClients)
     redisMasterSlavesPool.slavesClients.redisConnectionPool.size mustEqual 0
     Await.result(redisMasterSlavesPool.get[String]("test"),timeOut) mustEqual Some("value")
   }

  }




}
