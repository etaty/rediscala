package redis


import akka.util.ByteString
import org.specs2.execute.Result
import redis.api.clusters.ClusterSlots
import redis.protocol._

import scala.concurrent.Await

/**
  * Created by npeters on 20/05/16.
  */
class RedisClusterTest extends RedisClusterClients {


  var redisCluster:RedisCluster = null
  override def setup(): Unit = {
    super.setup()
    redisCluster = RedisCluster(nodePorts.map(p=>RedisServer("127.0.0.1",p)))
  }

  sequential

  "RedisComputeSlot" should {
    "simple" in {
      RedisComputeSlot.hashSlot("foo") mustEqual 12182
      RedisComputeSlot.hashSlot("somekey") mustEqual 11058
      RedisComputeSlot.hashSlot("somekey3452345325453532452345") mustEqual 15278
      RedisComputeSlot.hashSlot("rzarzaZERAZERfqsfsdQSFD") mustEqual 14258
      RedisComputeSlot.hashSlot("{foo}46546546546") mustEqual 12182
      RedisComputeSlot.hashSlot("foo_312312") mustEqual 5839
      RedisComputeSlot.hashSlot("aazaza{aa") mustEqual 11473
    }
  }

  "clusterSlots" should {
    "encoding" in {
      val clusterSlotsAsByteString = ByteString(new sun.misc.BASE64Decoder().decodeBuffer("KjMNCio0DQo6MA0KOjU0NjANCiozDQokOQ0KMTI3LjAuMC4xDQo6NzAwMA0KJDQwDQplNDM1OTlkZmY2ZTNhN2I5ZWQ1M2IxY2EwZGI0YmQwMDlhODUwYmE1DQoqMw0KJDkNCjEyNy4wLjAuMQ0KOjcwMDMNCiQ0MA0KYzBmNmYzOWI2NDg4MTVhMTllNDlkYzQ1MzZkMmExM2IxNDdhOWY1MA0KKjQNCjoxMDkyMw0KOjE2MzgzDQoqMw0KJDkNCjEyNy4wLjAuMQ0KOjcwMDINCiQ0MA0KNDhkMzcxMjBmMjEzNTc4Y2IxZWFjMzhlNWYyYmY1ODlkY2RhNGEwYg0KKjMNCiQ5DQoxMjcuMC4wLjENCjo3MDA1DQokNDANCjE0Zjc2OWVlNmU1YWY2MmZiMTc5NjZlZDRlZWRmMTIxOWNjYjE1OTINCio0DQo6NTQ2MQ0KOjEwOTIyDQoqMw0KJDkNCjEyNy4wLjAuMQ0KOjcwMDENCiQ0MA0KYzhlYzM5MmMyMjY5NGQ1ODlhNjRhMjA5OTliNGRkNWNiNDBlNDIwMQ0KKjMNCiQ5DQoxMjcuMC4wLjENCjo3MDA0DQokNDANCmVmYThmZDc0MDQxYTNhOGQ3YWYyNWY3MDkwM2I5ZTFmNGMwNjRhMjENCg=="))
      val clusterSlotsAsBulk: DecodeResult[RedisReply] = RedisProtocolReply.decodeReply(clusterSlotsAsByteString)
      var decodeValue = ""
      val dr: DecodeResult[String] = clusterSlotsAsBulk.map({
        case a: MultiBulk =>
          ClusterSlots().decodeReply(a).map(c => c.copy(slaves = c.slaves.toList)).toString()
        case _ => "fail"
      })


      val r: Result = dr match {
        case FullyDecoded(decodeValue, _) => decodeValue mustEqual "Vector(ClusterSlot(0,5460,ClusterNode(127.0.0.1,7000,e43599dff6e3a7b9ed53b1ca0db4bd009a850ba5),List(ClusterNode(127.0.0.1,7003,c0f6f39b648815a19e49dc4536d2a13b147a9f50))), " +
          "ClusterSlot(10923,16383,ClusterNode(127.0.0.1,7002,48d37120f213578cb1eac38e5f2bf589dcda4a0b),List(ClusterNode(127.0.0.1,7005,14f769ee6e5af62fb17966ed4eedf1219ccb1592))), " +
          "ClusterSlot(5461,10922,ClusterNode(127.0.0.1,7001,c8ec392c22694d589a64a20999b4dd5cb40e4201),List(ClusterNode(127.0.0.1,7004,efa8fd74041a3a8d7af25f70903b9e1f4c064a21))))"

        case _ => failure
      }

      r
    }

  }

  "Strings" should {
    "set-get" in {
      println("set")
      Await.result(redisCluster.set[String]("foo","FOO"), timeOut)
      println("exists")
      Await.result(redisCluster.exists("foo"), timeOut) mustEqual(true)

      println("get")
      val value = Await.result(redisCluster.get[String]("foo"), timeOut)  mustEqual Some("FOO")

      println("del")
      Await.result(redisCluster.del("foo","foo"), timeOut)

      println("exists")
      Await.result(redisCluster.exists("foo"), timeOut) mustEqual(false)

    }

    "mset-mget" in {
      println("mset")
      Await.result(redisCluster.mset[String](Map("{foo}1"->"FOO1","{foo}2"->"FOO2")), timeOut)
      println("exists")
      Await.result(redisCluster.exists("{foo}1"), timeOut) mustEqual(true)
      Await.result(redisCluster.exists("{foo}2"), timeOut) mustEqual(true)

      println("mget")
      Await.result(redisCluster.mget[String]("{foo}1","{foo}2"), timeOut) mustEqual Seq(Some("FOO1"),Some("FOO2"))

      println("del")
      Await.result(redisCluster.del("{foo}1","{foo}2"), timeOut)

      println("exists")
      Await.result(redisCluster.exists("{foo}1"), timeOut) mustEqual(false)

    }
  }

  "tools" should {
    "groupby" in {
      redisCluster.groupByCluserServer(Seq("{foo1}1","{foo2}1","{foo1}2","{foo2}2")).sortBy(_.head).toList mustEqual( Seq(Seq("{foo2}1","{foo2}2"),Seq("{foo1}1","{foo1}2")).sortBy(_.head)  )
    }
  }


  "long run" should {
    "wait" in {
      println("set "+ redisCluster.getClusterAndConnection(RedisComputeSlot.hashSlot("foo1")).get._1.master.toString)
      Await.result(redisCluster.set[String]("foo1","FOO"), timeOut)
      Await.result(redisCluster.get[String]("foo1"), timeOut)
      println("wait...")
     // Thread.sleep(15000)
      println("get")
      Await.result(redisCluster.get[String]("foo1"), timeOut) mustEqual Some("FOO")

    }


  }

  "clusterInfo" should {
    "just work" in {
      val res = Await.result(redisCluster.clusterInfo(), timeOut)
      res must not be empty
      for (v <- res) {
        println(s"Key  ${v._1} value ${v._2}")
      }
      res("cluster_state") mustEqual "ok"
      res("cluster_slots_ok") mustEqual "16384"
      res("cluster_known_nodes") mustEqual "6"
      res("cluster_size") mustEqual "3"
    }
  }

  "clusterNodes" should {
    "just work" in {
      val res = Await.result(redisCluster.clusterNodes(), timeOut)
      res must not be empty
      for (m <- res) {
        println(m.toString)
      }
      res.size mustEqual 6
      res.count(_.master != "-") mustEqual 3
      res.count(_.link_state == "connected") mustEqual 6
    }
  }
}
