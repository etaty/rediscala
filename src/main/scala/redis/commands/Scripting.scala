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
  def evalshaOrEval(redisScript: RedisScript, keys: Seq[String] = Seq.empty[String], args: Seq[String] = Seq.empty[String]): Future[RedisReply] = {
    evalsha(redisScript.sha1, keys, args).recoverWith({
      case ReplyErrorException(message) if message.startsWith("NOSCRIPT") => eval(redisScript.script, keys, args)
    })
  }

  def eval[R: RedisReplyDeserializer](script: String, keys: Seq[String] = Seq.empty[String], args: Seq[String] = Seq.empty[String]): Future[R] = {
    send(Eval(script, keys, args))
  }

  def evalsha[R: RedisReplyDeserializer](sha1: String, keys: Seq[String] = Seq.empty[String], args: Seq[String] = Seq.empty[String]): Future[R] = {
    send(Evalsha(sha1, keys, args))
  }

  /**
   * Try EVALSHA, if NOSCRIPT returned, fallback to EVAL
   */
  def evalshaOrEvalForTypeOf[R: ByteStringDeserializer](redisScript: RedisScript, keys: Seq[String] = Seq.empty[String], args: Seq[String] = Seq.empty[String]): Future[Option[R]] = {
    evalshaForTypeOf[R](redisScript.sha1, keys, args).recoverWith({
      case ReplyErrorException(message) if message.startsWith("NOSCRIPT") => evalForTypeOf[R](redisScript.script, keys, args)
    })
  }

  def evalForTypeOf[R: ByteStringDeserializer](script: String, keys: Seq[String] = Seq.empty[String], args: Seq[String] = Seq.empty[String]): Future[Option[R]] = {
    send(EvalForTypeOf(script, keys, args))
  }

  def evalshaForTypeOf[R: ByteStringDeserializer](sha1: String, keys: Seq[String] = Seq.empty[String], args: Seq[String] = Seq.empty[String]): Future[Option[R]] = {
    send(EvalshaForTypeOf(sha1, keys, args))
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



