package redis.api.clusters

import akka.util.ByteString
import redis.{MultiBulkConverter, RedisCommand, RedisCommandMultiBulk, RedisCommandStatusString}
import redis.api.connection.Ping._
import redis.protocol.{DecodeResult, MultiBulk, RedisProtocolReply, RedisReply}

import scala.math.Ordering



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