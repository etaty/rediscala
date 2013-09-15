package redis.api.scripting

import java.security.MessageDigest
import redis.protocol.{MultiBulk, Bulk, RedisProtocolReply, RedisReply}
import redis._
import akka.util.ByteString

case class RedisScript(script: String) {
  lazy val sha1 = {
    val messageDigestSha1 = MessageDigest.getInstance("SHA-1")
    messageDigestSha1.digest(script.getBytes("UTF-8")).map("%02x".format(_)).mkString
  }
}

case class Eval[KK, KA](script: String, keys: Seq[KK] = Seq(), args: Seq[KA] = Seq())(implicit redisKeys: ByteStringSerializer[KK], redisArgs: ByteStringSerializer[KA]) extends RedisCommandRedisReplyRedisReply {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("EVAL",
    (ByteString(script)
      +: ByteString(keys.length.toString)
      +: keys.map(redisKeys.serialize)) ++ args.map(redisArgs.serialize))
}

case class Evalsha[KK, KA](sha1: String, keys: Seq[KK] = Seq(), args: Seq[KA] = Seq())(implicit redisKeys: ByteStringSerializer[KK], redisArgs: ByteStringSerializer[KA]) extends RedisCommandRedisReplyRedisReply {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("EVALSHA",
    (ByteString(sha1)
      +: ByteString(keys.length.toString)
      +: keys.map(redisKeys.serialize)) ++ args.map(redisArgs.serialize))
}

case object ScriptFlush extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SCRIPT", Seq(ByteString("FLUSH")))
}

case object ScriptKill extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SCRIPT", Seq(ByteString("KILL")))
}

case class ScriptLoad(script: String) extends RedisCommandBulk[String] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SCRIPT", Seq(ByteString("LOAD"), ByteString(script)))

  def decodeReply(bulk: Bulk) = bulk.toString
}

case class ScriptExists(sha1: Seq[String]) extends RedisCommandMultiBulk[Seq[Boolean]] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SCRIPT", ByteString("EXISTS") +: sha1.map(ByteString(_)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqBoolean(mb)
}