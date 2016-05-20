package redis.api.clusters

import akka.util.ByteString
import redis.{MultiBulkConverter, RedisCommand, RedisCommandMultiBulk, RedisCommandStatusString}
import redis.api.connection.Ping._
import redis.protocol.{DecodeResult, MultiBulk, RedisProtocolReply, RedisReply}

import scala.math.Ordering



case class ClusterServer(ip:String,port:Int,id:String)
case class ClusterSlot(begin:Int,end:Int,master:ClusterServer,slaves:Seq[ClusterServer])  extends Comparable[ClusterSlot] {
  override def compareTo(x: ClusterSlot): Int = {
    this.begin.compare(x.begin)
  }
}


case class ClusterSlots() extends RedisCommand[MultiBulk,Seq[ClusterSlot]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("CLUSTER SLOTS")

  def buildClusterServer(vect:Seq[RedisReply]) = {
    ClusterServer(vect(0).toByteString.utf8String,vect(1).toByteString.utf8String.toInt,vect(2).toByteString.utf8String  )
  }

  def decodeReply(mb: MultiBulk) = {
    val clusterSlots: Option[Seq[Option[ClusterSlot]]] = mb.responses.map{ vector =>
      val seq: Seq[RedisReply] = vector.toSeq
      seq.collect { case slot:MultiBulk =>
        slot.responses.map { slot1 =>
          val begin = slot1.toSeq(0).toByteString.utf8String.toInt
          val end = slot1.toSeq(1).toByteString.utf8String.toInt

          val masterMB = slot1.toSeq(2).asInstanceOf[MultiBulk]
          val slavesMB = slot1.toSeq(3).asInstanceOf[MultiBulk]

         val masterClusterServer = masterMB.responses.map(
            vect =>  buildClusterServer(vect.toSeq)
          ).getOrElse(throw new RuntimeException("no master"))

          val slavesClusterServer: Seq[ClusterServer] = slavesMB.responses.map{
            _.grouped(3).map{ vect =>
              buildClusterServer(vect.toSeq)
            }.toSeq
          }.getOrElse(Seq.empty)

          ClusterSlot(begin,end,masterClusterServer,slavesClusterServer)
        }
      }
    }
    clusterSlots.getOrElse(Seq.empty).flatten
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