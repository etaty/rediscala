package redis

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.testkit.TestProbe
import org.specs2.mutable.SpecificationLike
import redis.api.clusters.{ClusterNode, ClusterSlot}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.concurrent.stm.Ref

class RedisClusterSpec extends TestKit(ActorSystem()) with SpecificationLike {

  var _clusterSlots = Seq(clusterSlot("1", 6000, 0, 16383))

  val redisCluster = new RedisCluster(Seq(RedisServer("127.0.0.1", 6000))) {
    override def clusterSlots() = Future(_clusterSlots)
    override def makeConnection(server: RedisServer) = (server, RedisConnection(TestProbe().ref, Ref(true)))
  }

  def clusterSlot(nodeId: String, port: Int, begin: Int, end: Int) =
    ClusterSlot(begin, end, ClusterNode("127.0.0.1", port, "1"), Nil)

  def checkSlotMaps() = {
    redisCluster.redisServerConnections.keySet.toSet mustEqual _clusterSlots.map(_.master.hostAndPort).toSet
    redisCluster.clusterSlotsRef.single.get.map(_.keySet.toSet) mustEqual Some(_clusterSlots.toSet)
    _clusterSlots.foreach { slots =>
      val connection = redisCluster.redisServerConnections.get(slots.master.hostAndPort)
      (slots.begin to slots.end).foreach { slot =>
        redisCluster.getClusterAndConnection(slot).map(_._2) mustEqual (connection)
      }
    }
    success
  }

  "redis cluster" should {

    "add new nodes" in {
      _clusterSlots = Seq(
        clusterSlot("1", 6000, 0, 4095),
        clusterSlot("2", 6001, 4096, 8191),
        clusterSlot("1", 6000, 8192, 12287),
        clusterSlot("2", 6001, 12288, 16383)
      )
      Await.result(redisCluster.asyncRefreshClusterSlots(true), 10.seconds)
      checkSlotMaps()
    }

    "remove unused nodes" in {
      _clusterSlots = Seq(
        clusterSlot("2", 6001, 0, 16383)
      )
      Await.result(redisCluster.asyncRefreshClusterSlots(true), 10.seconds)
      Await.result(redisCluster.asyncRefreshClusterSlots(true), 10.seconds)
      checkSlotMaps()
    }
  }
}
