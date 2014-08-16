package redis.commands

import redis._
import scala.concurrent.Await
import akka.util.ByteString
import redis.actors.ReplyErrorException
import redis.protocol.{Bulk, Status, MultiBulk}

class PipelineSpec extends RedisClusterClients {

  "Pipeline commands" should {
    "basic" in {
      val redisPipeline = redisClient.pipeline()
      redisPipeline.exec()
      val set = redisPipeline.set("a", "abc")
      val decr = redisPipeline.decr("a")
      val get = redisPipeline.get("a")
      redisPipeline.exec()
      val r = for {
        s <- set
        g <- get
      } yield {
        s mustEqual true
        g mustEqual Some(ByteString("abc"))
      }
      Await.result(decr, timeOut) must throwA[ReplyErrorException]("ERR value is not an integer or out of range")
      Await.result(r, timeOut)
    }
    
    "redis pool" in {

      val pool =  RedisClientPool(Seq(RedisServer(port = slavePort1),RedisServer(port = slavePort2)))

      val redisPipeline = pool.pipeline()

      val get1 = redisPipeline.configGet("port")
      val get2 = redisPipeline.configGet("port")
      redisPipeline.exec()
      val r1 = for {
        g1 <- get1
        g2 <- get2
      } yield {
        g1 mustEqual g2
      }
    
      Await.result(r1, timeOut)


      val redisPipeline2 = pool.pipeline()
      val get3 = redisPipeline2.configGet("port")
      val get4 = redisPipeline2.configGet("port")
      redisPipeline2.exec()
      val r2 = for {
        g3 <- get3
        g4 <- get4
      } yield {
        g3 mustEqual g4
      }
    
      Await.result(r2, timeOut)

      val r3 = for {
        g1 <- get1
        g3 <- get3
      } yield {
        g1 must_!= g3
      }

      Await.result(r3, timeOut)

    }
    

    "redis masterslave: master operation" in {

      val clientMasterSlavesPool = RedisClientMasterSlaves(RedisServer(port = masterPort),Seq(RedisServer(port = slavePort1),RedisServer(port = slavePort2)))
      val redisPipelineMaster = clientMasterSlavesPool.pipeline(true)
      val set = redisPipelineMaster.set("a", "abc")
      val get = redisPipelineMaster.get("a")
      redisPipelineMaster.exec()
      val r = for {
        s <- set
        g <- get
      } yield {
        s mustEqual true
        g mustEqual Some(ByteString("abc"))
      }
      Await.result(r, timeOut)

    }

    "redis masterslave: slave operation" in {

      val clientMasterSlavesPool = RedisClientMasterSlaves(RedisServer(port = masterPort),Seq(RedisServer(port = slavePort1),RedisServer(port = slavePort2)))
      val redisPipelineSlave = clientMasterSlavesPool.pipeline(false)
      redisPipelineSlave.set("a", "abc") must throwA[IllegalArgumentException]
      val get = redisPipelineSlave.get("a")
      redisPipelineSlave.exec()
      val r = for {
        g <- get
      } yield {
        g mustEqual None
      }
      Await.result(r, timeOut)

    }

  }
}
