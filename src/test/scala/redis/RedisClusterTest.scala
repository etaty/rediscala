package redis

import java.util.Base64

import akka.util.ByteString
import redis.actors.DecodeReplies
import redis.api.clusters.ClusterSlots
import redis.protocol.{DecodeResult, MultiBulk, RedisProtocolReply, RedisReply}

import scala.concurrent.Await

/**
  * Created by npeters on 20/05/16.
  */
class RedisClusterTest extends  RedisHelper {

  sequential

  val redis = RedisClient("localhost",7000)
  "clusterSlots" should {
    "encoding" in {
      val clusterSlotsAsByteString = ByteString(Base64.getDecoder.decode("KjMNCio0DQo6MA0KOjU0NjANCiozDQokOQ0KMTI3LjAuMC4xDQo6NzAwMA0KJDQwDQplNDM1OTlkZmY2ZTNhN2I5ZWQ1M2IxY2EwZGI0YmQwMDlhODUwYmE1DQoqMw0KJDkNCjEyNy4wLjAuMQ0KOjcwMDMNCiQ0MA0KYzBmNmYzOWI2NDg4MTVhMTllNDlkYzQ1MzZkMmExM2IxNDdhOWY1MA0KKjQNCjoxMDkyMw0KOjE2MzgzDQoqMw0KJDkNCjEyNy4wLjAuMQ0KOjcwMDINCiQ0MA0KNDhkMzcxMjBmMjEzNTc4Y2IxZWFjMzhlNWYyYmY1ODlkY2RhNGEwYg0KKjMNCiQ5DQoxMjcuMC4wLjENCjo3MDA1DQokNDANCjE0Zjc2OWVlNmU1YWY2MmZiMTc5NjZlZDRlZWRmMTIxOWNjYjE1OTINCio0DQo6NTQ2MQ0KOjEwOTIyDQoqMw0KJDkNCjEyNy4wLjAuMQ0KOjcwMDENCiQ0MA0KYzhlYzM5MmMyMjY5NGQ1ODlhNjRhMjA5OTliNGRkNWNiNDBlNDIwMQ0KKjMNCiQ5DQoxMjcuMC4wLjENCjo3MDA0DQokNDANCmVmYThmZDc0MDQxYTNhOGQ3YWYyNWY3MDkwM2I5ZTFmNGMwNjRhMjENCg=="))
      val clusterSlotsAsBulk: DecodeResult[RedisReply] = RedisProtocolReply.decodeReply(clusterSlotsAsByteString)
      var decodeValue = ""
      clusterSlotsAsBulk.map({
        case a: MultiBulk =>
          decodeValue = ClusterSlots().decodeReply(a).toString()
          case _ => failure
      })

      decodeValue mustEqual "Vector(ClusterSlot(0,5460,ClusterServer(127.0.0.1,7000,e43599dff6e3a7b9ed53b1ca0db4bd009a850ba5),Stream(ClusterServer(127.0.0.1,7003,c0f6f39b648815a19e49dc4536d2a13b147a9f50), ?)), " +
        "ClusterSlot(10923,16383,ClusterServer(127.0.0.1,7002,48d37120f213578cb1eac38e5f2bf589dcda4a0b),Stream(ClusterServer(127.0.0.1,7005,14f769ee6e5af62fb17966ed4eedf1219ccb1592), ?)), " +
        "ClusterSlot(5461,10922,ClusterServer(127.0.0.1,7001,c8ec392c22694d589a64a20999b4dd5cb40e4201),Stream(ClusterServer(127.0.0.1,7004,efa8fd74041a3a8d7af25f70903b9e1f4c064a21), ?)))"

    }

    "runtime" in {
     val clusterSlots = Await.result(redis.clusterSlots(), timeOut)

      clusterSlots.toString() mustEqual  "Vector(ClusterSlot(0,5460,ClusterServer(127.0.0.1,7000,e43599dff6e3a7b9ed53b1ca0db4bd009a850ba5),Stream(ClusterServer(127.0.0.1,7003,c0f6f39b648815a19e49dc4536d2a13b147a9f50), ?)), " +
        "ClusterSlot(10923,16383,ClusterServer(127.0.0.1,7002,48d37120f213578cb1eac38e5f2bf589dcda4a0b),Stream(ClusterServer(127.0.0.1,7005,14f769ee6e5af62fb17966ed4eedf1219ccb1592), ?)), " +
        "ClusterSlot(5461,10922,ClusterServer(127.0.0.1,7001,c8ec392c22694d589a64a20999b4dd5cb40e4201),Stream(ClusterServer(127.0.0.1,7004,efa8fd74041a3a8d7af25f70903b9e1f4c064a21), ?)))"

    }



  }

}
