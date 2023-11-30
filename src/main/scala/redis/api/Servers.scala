package redis.api.servers

import redis._
import org.apache.pekko.util.ByteString
import redis.protocol.{MultiBulk, Bulk}
import redis.api.ShutdownModifier

case object Bgrewriteaof extends RedisCommandStatusString {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("BGREWRITEAOF")
}

case object Bgsave extends RedisCommandStatusString {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("BGSAVE")
}

case class ClientKill(ip: String, port: Int) extends RedisCommandStatusBoolean {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("CLIENT", Seq(ByteString("KILL"), ByteString(ip + ":" + port)))
}

case object ClientList extends RedisCommandBulk[Seq[Map[String, String]]] {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("CLIENT", Seq(ByteString("LIST")))

  def decodeReply(r: Bulk): Seq[Map[String, String]] = r.asOptByteString.map(bs => {
    val s = bs.utf8String
    val r = s.split('\n').map(line => {
      line.split(' ').map(kv => {
        val keyValue = kv.split('=')
        val value = if (keyValue.length > 1) keyValue(1) else ""
        (keyValue(0), value)
      }).toMap
    }).toSeq
    r
  }).getOrElse(Seq.empty)
}

case object ClientGetname extends RedisCommandBulkOptionByteString[String] {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("CLIENT", Seq(ByteString("GETNAME")))
  val deserializer: ByteStringDeserializer[String] = ByteStringDeserializer.String
}


case class ClientSetname(connectionName: String) extends RedisCommandStatusBoolean {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("CLIENT", Seq(ByteString("SETNAME"), ByteString(connectionName)))
}

case class ConfigGet(parameter: String) extends RedisCommandMultiBulk[Map[String, String]] {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("CONFIG", Seq(ByteString("GET"), ByteString(parameter)))

  def decodeReply(r: MultiBulk): Map[String, String] = MultiBulkConverter.toMapString(r)
}

case class ConfigSet(parameter: String, value: String) extends RedisCommandStatusBoolean {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("CONFIG", Seq(ByteString("SET"), ByteString(parameter), ByteString(value)))
}

case object ConfigResetstat extends RedisCommandStatusBoolean {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("CONFIG", Seq(ByteString("RESETSTAT")))
}

case object Dbsize extends RedisCommandIntegerLong {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("DBSIZE")
}

case class DebugObject[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandStatusString {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("DEBUG", Seq(ByteString("OBJECT"), redisKey.serialize(key)))
}

case object DebugSegfault extends RedisCommandStatusString {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("DEBUG SEGFAULT")
}

case class Flushall(async: Boolean = false) extends RedisCommandStatusBoolean {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("FLUSHALL", if (async) Seq(ByteString("ASYNC")) else Seq.empty)
}

case class Flushdb(async: Boolean = false) extends RedisCommandStatusBoolean {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("FLUSHDB", if (async) Seq(ByteString("ASYNC")) else Seq.empty)
}

case class Info(section: Option[String] = None) extends RedisCommandBulk[String] {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("INFO", section.map(s => Seq(ByteString(s))).getOrElse(Seq()))

  def decodeReply(r: Bulk): String = r.toOptString.get
}

case object Lastsave extends RedisCommandIntegerLong {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("LASTSAVE")
}

case object Save extends RedisCommandStatusBoolean {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("SAVE")
}

case class Slaveof(ip: String, port: Int) extends RedisCommandStatusBoolean {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("SLAVEOF", Seq(ByteString(ip), ByteString(port.toString)))
}

case class Shutdown(modifier: Option[ShutdownModifier] = None) extends RedisCommandStatusBoolean {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("SHUTDOWN", modifier.map(m => Seq(ByteString(m.toString))).getOrElse(Seq.empty))
}

case object SlaveofNoOne extends RedisCommandStatusBoolean {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("SLAVEOF NO ONE")
}

case object Time extends RedisCommandMultiBulk[(Long, Long)] {
  val isMasterOnly: Boolean = true
  val encodedRequest: ByteString = encode("TIME")

  def decodeReply(mb: MultiBulk): (Long, Long) = {
    mb.responses.map(r => {
      (r.head.toByteString.utf8String.toLong, r.tail.head.toByteString.utf8String.toLong)
    }).get
  }
}

//case class Log(id: Long, timestamp: Long, duration: Long, command: Seq[ByteString])

//case class Slowlog(subcommand: String, argurment: String)