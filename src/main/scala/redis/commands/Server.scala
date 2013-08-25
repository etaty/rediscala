package redis.commands

import redis.Request

trait Server extends Request {
/*
  def bgrewriteaof[A](): Future[String] =
    send("BGREWRITEAOF").mapTo[Status].map(_.toString)

  def bgsave(): Future[String] =
    send("BGSAVE").mapTo[Status].map(_.toString)

  def clientKill(ip: String, port: Int): Future[Boolean] =
    send("CLIENT", Seq(ByteString("KILL"), ByteString(ip + ':' + port.toString))).mapTo[Status].map(_.toBoolean)

  def clientList(): Future[String] =
    send("CLIENT", Seq(ByteString("LIST"))).mapTo[Bulk].map(_.toString)

  def clientGetname(): Future[Option[String]] =
    send("CLIENT", Seq(ByteString("GETNAME"))).mapTo[Bulk].map(_.toOptString)

  def clientSetname(connectionName: String): Future[Boolean] =
    send("CLIENT", Seq(ByteString("SETNAME"), ByteString(connectionName))).mapTo[Status].map(_.toBoolean)

  def configGet(parameter: String): Future[Option[String]] =
    send("CONFIG GET", Seq(ByteString(parameter))).mapTo[Bulk].map(_.toOptString)

  def configSet(parameter: String, value: String): Future[Boolean] =
    send("CONFIG SET", Seq(ByteString(parameter), ByteString(value))).mapTo[Status].map(_.toBoolean)

  def configResetstat(parameter: String, value: String): Future[Boolean] =
    send("CONFIG RESETSTAT").mapTo[Status].map(_.toBoolean)

  def dbsize(): Future[Long] =
    send("DBSIZE").mapTo[Integer].map(_.toLong)

  def debugObject(key: String): Future[ByteString] =
    send("DEBUG OBJECT", Seq(ByteString(key))).mapTo[Status].map(_.toByteString)

  def debugSegfault(): Future[ByteString] =
    send("DEBUG SEGFAULT").mapTo[Status].map(_.toByteString)

  def flushall(): Future[Boolean] =
    send("FLUSHALL").mapTo[Status].map(_.toBoolean)

  def flushdb(): Future[Boolean] =
    send("FLUSHDB").mapTo[Status].map(_.toBoolean)

  def info(): Future[String] =
    send("INFO").mapTo[Bulk].map(_.toString)

  def info(section: String): Future[String] =
    send("INFO", Seq(ByteString(section))).mapTo[Bulk].map(_.toString)

  def lastsave(): Future[Long] =
    send("LASTSAVE").mapTo[Integer].map(_.toLong)

  def monitor(): Future[Long] = ??? // TODO blocking!

  def save(): Future[Boolean] =
    send("SAVE").mapTo[Status].map(_.toBoolean)

  def shutdown(): Future[Boolean] =
    send("SHUTDOWN").mapTo[Status].map(_.toBoolean)

  // timeout on success LOL
  def shutdown(modifier: ShutdownModifier): Future[Boolean] =
    send("SHUTDOWN", Seq(ByteString(modifier.toString))).mapTo[Status].map(_.toBoolean)

  def slaveof(host: String, port: Int): Future[String] =
    send("SLAVEOF", Seq(ByteString(host), ByteString(port.toString))).mapTo[Status].map(_.toString)

  def slowlog(subcommand: String, argument: String): Future[String] =
    send("SLOWLOG", Seq(ByteString(subcommand), ByteString(argument))).mapTo[Status].map(_.toString)

  def sync(): Future[RedisReply] =
    send("SYNC").mapTo[RedisReply]

  def time()(implicit convert: MultiBulkConverter[Seq[ByteString]]): Future[Try[Seq[ByteString]]] =
    send("TIME").mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])
*/
}

sealed trait ShutdownModifier

case object NOSAVE extends ShutdownModifier

case object SAVE extends ShutdownModifier