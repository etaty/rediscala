package redis.api.scripting

import java.security.MessageDigest
import redis.protocol.{MultiBulk, Bulk}
import redis._
import akka.util.ByteString

case class RedisScript(script: String) {
  lazy val sha1 = {
    val messageDigestSha1 = MessageDigest.getInstance("SHA-1")
    messageDigestSha1.digest(script.getBytes("UTF-8")).map("%02x".format(_)).mkString
  }
}

trait EvaledScript extends {
  val isMasterOnly = true
  def encodeRequest[KK, KA](
                    encoder: ((String, Seq[ByteString]) => ByteString),
                    command: String,
                    param: String,
                    keys: Seq[KK],
                    args: Seq[KA],
                    keySerializer: ByteStringSerializer[KK],
                    argSerializer: ByteStringSerializer[KA]): ByteString = {
    encoder(command,
      (ByteString(param)
        +: ByteString(keys.length.toString)
        +: keys.map(keySerializer.serialize)) ++ args.map(argSerializer.serialize))
  }
}

case class Eval[R, KK, KA](script: String, keys: Seq[KK] = Seq(), args: Seq[KA] = Seq())(implicit redisKeys: ByteStringSerializer[KK], redisArgs: ByteStringSerializer[KA], deserializerR: RedisReplyDeserializer[R])
  extends RedisCommandRedisReplyRedisReply[R]
  with EvaledScript {
  val encodedRequest: ByteString = encodeRequest(encode, "EVAL", script, keys, args, redisKeys, redisArgs)
  val deserializer: RedisReplyDeserializer[R] = deserializerR
}

case class Evalsha[R, KK, KA](sha1: String, keys: Seq[KK] = Seq(), args: Seq[KA] = Seq())(implicit redisKeys: ByteStringSerializer[KK], redisArgs: ByteStringSerializer[KA], deserializerR: RedisReplyDeserializer[R])
  extends RedisCommandRedisReplyRedisReply[R]
  with EvaledScript {
  val encodedRequest: ByteString = encodeRequest(encode, "EVALSHA", sha1, keys, args, redisKeys, redisArgs)
  val deserializer: RedisReplyDeserializer[R] = deserializerR
}

case class EvalForTypeOf[KK, KA, R](script: String, keys: Seq[KK] = Seq(), args: Seq[KA] = Seq())(implicit redisKeys: ByteStringSerializer[KK], redisArgs: ByteStringSerializer[KA], deserializerR: ByteStringDeserializer[R])
  extends RedisCommandBulkOptionByteString[R]
  with EvaledScript {
    val deserializer: ByteStringDeserializer[R] = deserializerR
    val encodedRequest: ByteString = encodeRequest(encode, "EVAL", script, keys, args, redisKeys, redisArgs)
}

case class EvalshaForTypeOf[KK, KA, R](sha1: String, keys: Seq[KK] = Seq(), args: Seq[KA] = Seq())(implicit redisKeys: ByteStringSerializer[KK], redisArgs: ByteStringSerializer[KA], deserializerR: ByteStringDeserializer[R])
  extends RedisCommandBulkOptionByteString[R]
  with EvaledScript {
    val deserializer: ByteStringDeserializer[R] = deserializerR
    val encodedRequest: ByteString = encodeRequest(encode, "EVALSHA", sha1, keys, args, redisKeys, redisArgs)
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