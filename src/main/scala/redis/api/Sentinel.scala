package redis.api

import redis._
import org.apache.pekko.util.ByteString
import redis.protocol.MultiBulk

case class SenMasters() extends RedisCommandMultiBulk[Seq[Map[String,String]]] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SENTINEL MASTERS")

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqMapString(mb)
}

case class SenSlaves(master: String) extends RedisCommandMultiBulk[Seq[Map[String,String]]] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode(s"SENTINEL SLAVES $master")

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqMapString(mb)
}

case class SenMasterInfo(master: String) extends RedisCommandMultiBulk[Map[String, String]] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode(s"SENTINEL master $master")

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toMapString(mb)
}

case class SenGetMasterAddr(master: String) extends RedisCommandMultiBulk[Option[Seq[String]]] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode(s"SENTINEL get-master-addr-by-name $master")

  def decodeReply(mb: MultiBulk) = mb.responses.map(_.map(_.toString))
}

case class SenResetMaster(pattern: String) extends RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode(s"SENTINEL RESET $pattern")
}

case class SenMasterFailover(master: String) extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode(s"SENTINEL FAILOVER $master")
}
