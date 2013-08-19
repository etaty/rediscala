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

case class Eval(script: String, keys: Seq[String] = Seq(), args: Seq[String] = Seq()) extends RedisCommandRedisReplyRedisReply {
  val encodedRequest: ByteString = encode("EVAL",
    (ByteString(script)
      +: ByteString(keys.length.toString)
      +: keys.map(ByteString(_))) ++ args.map(ByteString(_)))
}

case class Evalsha(sha1: String, keys: Seq[String] = Seq(), args: Seq[String] = Seq()) extends RedisCommandRedisReplyRedisReply {
  val encodedRequest: ByteString = encode("EVALSHA",
    (ByteString(sha1)
      +: ByteString(keys.length.toString)
      +: keys.map(ByteString(_))) ++ args.map(ByteString(_)))
}

case object ScriptFlush extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("SCRIPT", Seq(ByteString("FLUSH")))
}

case object ScriptKill extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("SCRIPT", Seq(ByteString("KILL")))
}

case class ScriptLoad(script: String) extends RedisCommandBulk[String] {
  val encodedRequest: ByteString = encode("SCRIPT", Seq(ByteString("LOAD"), ByteString(script)))

  def decodeReply(bulk: Bulk) = bulk.toString
}

case class ScriptExists(sha1: Seq[String]) extends RedisCommandMultiBulk[Seq[Boolean]] {
  val encodedRequest: ByteString = encode("SCRIPT", ByteString("EXISTS") +: sha1.map(ByteString(_)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqBoolean(mb)
}