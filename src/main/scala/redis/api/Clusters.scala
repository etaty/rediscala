package redis.api.clusters

import org.apache.pekko.util.ByteString
import redis.RedisCommand
import redis.protocol.{DecodeResult, Bulk, MultiBulk, RedisProtocolReply, RedisReply}




case class ClusterNode(host:String, port:Int, id:String)
case class ClusterSlot(begin:Int, end:Int, master:ClusterNode, slaves:Seq[ClusterNode])  extends Comparable[ClusterSlot] {
  override def compareTo(x: ClusterSlot): Int = {
    this.begin.compare(x.begin)
  }
}


case class ClusterSlots() extends RedisCommand[MultiBulk,Seq[ClusterSlot]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("CLUSTER SLOTS")

  def buildClusterNode(vect:Seq[RedisReply]): ClusterNode = {
    ClusterNode(vect(0).toByteString.utf8String,vect(1).toByteString.utf8String.toInt,vect(2).toByteString.utf8String)
  }

  def decodeReply(mb: MultiBulk): Seq[ClusterSlot] = {
    val clusterSlots: Option[Seq[ClusterSlot]] = mb.responses.map{ vector =>
      vector.collect {
        case MultiBulk(Some(groupSlot)) =>
        //
        // redis response:
        // MultiBulk(begin,end,MultiBulk(masterId,masterPort,masterId),MultiBulk(slave1Id,slave1Port,slave1Id),MultiBulk(slave2Id,slave2Port,slave2Id))...,
        // MultiBulk(begin,end,MultiBulk(masterId,masterPort,masterId),MultiBulk(slave1Id,slave1Port,slave1Id),MultiBulk(slave2Id,slave2Port,slave2Id))
        //
        val begin = groupSlot(0).toByteString.utf8String.toInt
        val end = groupSlot(1).toByteString.utf8String.toInt
        val masterMB = groupSlot(2)

        val masterNode = masterMB match {
          case MultiBulk(Some(vect)) => buildClusterNode(vect)
          case _ => throw new RuntimeException("no master found")
        }

        val slavesNode: Seq[ClusterNode] = groupSlot.lift(3).map {
          case MultiBulk(Some(responses)) =>
              responses.grouped(3).map { vect =>
                buildClusterNode(vect)
              }.toSeq
          case _ => Seq.empty
        }.getOrElse(Seq.empty)
        ClusterSlot(begin,end,masterNode,slavesNode)

      }
    }
    clusterSlots.getOrElse(Seq.empty)
  }

  override val decodeRedisReply: PartialFunction[ByteString, DecodeResult[MultiBulk]] = {
    case bs if bs.head == RedisProtocolReply.MULTIBULK => {
      val multibulk = RedisProtocolReply.decodeReplyMultiBulk(bs)
      multibulk
    }
    case bs if bs.head == RedisProtocolReply.INTEGER => {
      RedisProtocolReply.decodeReplyMultiBulk(bs)
    }
    case bs => {
      RedisProtocolReply.decodeReplyMultiBulk(bs)
    }

  }
}

case class ClusterInfo() extends RedisCommand[Bulk, Map[String, String]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("CLUSTER INFO")
  def decodeReply(b: Bulk): Map[String, String] = {
    b.response.map(_.utf8String.split("\r\n").map(_.split(":")).map(s => (s(0),s(1))).toMap).getOrElse(Map.empty)
  }
  override val decodeRedisReply: PartialFunction[ByteString, DecodeResult[Bulk]] = {
    case s => RedisProtocolReply.decodeReplyBulk(s)
  }
}
case class ClusterNodeInfo(id:String, ip_port:String, flags:String, master:String, ping_sent:Long, pong_recv:Long, config_epoch:Long, link_state:String, slots:Array[String])
case class ClusterNodes() extends RedisCommand[Bulk, Array[ClusterNodeInfo]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("CLUSTER NODES")
  def decodeReply(b: Bulk): Array[ClusterNodeInfo] = {
    b.response.map(_.utf8String.split("\n").map(_.split(" ")).map(s => ClusterNodeInfo(s(0), s(1), s(2), s(3), s(4).toLong, s(5).toLong, s(6).toLong, s(7), s.drop(8)))).getOrElse(Array.empty)
  }
  override val decodeRedisReply: PartialFunction[ByteString, DecodeResult[Bulk]] = {
    case s => RedisProtocolReply.decodeReplyBulk(s)
  }
}