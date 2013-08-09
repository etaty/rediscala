package redis.commands

import akka.util.ByteString
import redis._
import scala.concurrent.{ExecutionContext, Future}
import redis.protocol._
import redis.protocol.Status
import redis.protocol.Bulk
import redis.api.script.RedisScript
import redis.actors.ReplyErrorException
import scala.util.Try

trait Scripting extends Request {
  /**
   * Try EVALSHA, if NOSCRIPT returned, fallback to EVAL
   */
  def evalshaOrEval(redisScript: RedisScript, keys: Seq[String] = Seq(), args: Seq[String] = Seq())(implicit ec: ExecutionContext): Future[RedisReply] = {
    evalsha(redisScript.sha1, keys, args).recoverWith({
      case ReplyErrorException(message) if message.startsWith("NOSCRIPT") => eval(redisScript.script, keys, args)
    })
  }

  def eval(script: String, keys: Seq[String] = Seq(), args: Seq[String] = Seq())(implicit ec: ExecutionContext): Future[RedisReply] = {
    send("EVAL", (ByteString(script) +: ByteString(keys.length.toString) +: keys.map(ByteString(_))) ++ args.map(ByteString(_)))
  }

  def evalsha(sha1: String, keys: Seq[String] = Seq(), args: Seq[String] = Seq())(implicit ec: ExecutionContext): Future[RedisReply] = {
    send("EVALSHA", (ByteString(sha1) +: ByteString(keys.length.toString) +: keys.map(ByteString(_))) ++ args.map(ByteString(_)))
  }

  def scriptFlush()(implicit ec: ExecutionContext): Future[Boolean] = {
    send("SCRIPT", Seq(ByteString("FLUSH"))).mapTo[Status].map(_.toBoolean)
  }

  def scriptKill()(implicit ec: ExecutionContext): Future[Boolean] = {
    send("SCRIPT", Seq(ByteString("KILL"))).mapTo[Status].map(_.toBoolean)
  }

  def scriptLoad(script: String)(implicit ec: ExecutionContext): Future[String] = {
    send("SCRIPT", Seq(ByteString("LOAD"), ByteString(script))).mapTo[Bulk].map(_.toString)
  }

  def scriptExists(sha1: String*)(implicit convert: MultiBulkConverter[Seq[Boolean]], ec: ExecutionContext): Future[Try[Seq[Boolean]]] = {
    send("SCRIPT", ByteString("EXISTS") +: sha1.map(ByteString(_))).mapTo[MultiBulk].map(_.asTry[Seq[Boolean]])
  }
}



