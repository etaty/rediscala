package redis.commands

import redis._
import scala.concurrent.Future
import redis.protocol._
import redis.api.scripting._
import redis.actors.ReplyErrorException

trait Scripting extends Request {
  /**
   * Try EVALSHA, if NOSCRIPT returned, fallback to EVAL
   */
  def evalshaOrEval[KK: ByteStringSerializer, KA: ByteStringSerializer](redisScript: RedisScript, keys: Seq[KK] = Seq.empty[String], args: Seq[KA] = Seq.empty[String]): Future[RedisReply] = {
    evalsha(redisScript.sha1, keys, args).recoverWith({
      case ReplyErrorException(message) if message.startsWith("NOSCRIPT") => eval(redisScript.script, keys, args)
    })
  }

  def eval[KK: ByteStringSerializer, KA: ByteStringSerializer](script: String, keys: Seq[KK] = Seq.empty[String], args: Seq[KA] = Seq.empty[String]): Future[RedisReply] = {
    send(Eval(script, keys, args))
  }

  def evalsha[KK: ByteStringSerializer, KA: ByteStringSerializer](sha1: String, keys: Seq[KK] = Seq.empty[String], args: Seq[KA] = Seq.empty[String]): Future[RedisReply] = {
    send(Evalsha(sha1, keys, args))
  }

  def scriptFlush(): Future[Boolean] = {
    send(ScriptFlush)
  }

  def scriptKill(): Future[Boolean] = {
    send(ScriptKill)
  }

  def scriptLoad(script: String): Future[String] = {
    send(ScriptLoad(script))
  }

  def scriptExists(sha1: String*): Future[Seq[Boolean]] = {
    send(ScriptExists(sha1))
  }
}



