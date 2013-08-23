package redis.api

import redis._
import akka.util.ByteString
import redis.protocol.MultiBulk

case class SenMasters() extends RedisCommandMultiBulk[Seq[Map[String,String]]] {
  val encodedRequest: ByteString = encode("SENTINEL MASTERS")

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqMapString(mb)
}

case class SenSlaves(master: String) extends RedisCommandMultiBulk[Seq[Map[String,String]]] {
  val encodedRequest: ByteString = encode(s"SENTINEL SLAVES $master")

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqMapString(mb)
}

case class SenIsMasterDown(masterIp: String, port: Int) extends RedisCommandMultiBulk[Seq[String]] {
  val encodedRequest: ByteString = encode(s"SENTINEL is-master-down-by-addr $masterIp $port")

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqString(mb)
}

case class SenGetMasterAddr(master: String) extends RedisCommandMultiBulk[Option[Seq[String]]] {
  val encodedRequest: ByteString = encode(s"SENTINEL get-master-addr-by-name $master")

  def decodeReply(mb: MultiBulk) = mb.responses.map(_.map(_.toString))
}

case class SenResetMaster(pattern: String) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode(s"SENTINEL RESET $pattern")
}

case class SenMasterFailover(master: String) extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode(s"SENTINEL FAILOVER $master")
}
