package redis.api.keys

import redis._
import akka.util.ByteString
import redis.protocol.{Bulk, Status, MultiBulk, Integer}
import scala.concurrent.duration.FiniteDuration
import redis.api.{Order, LimitOffsetCount}
import redis.api.LimitOffsetCount
import scala.Some


case class Del(keys: Seq[String]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("DEL", keys.map(ByteString.apply))
}

case class Dump(key: String) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("DUMP", Seq(ByteString(key)))
}

case class Exists(key: String) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("EXISTS", Seq(ByteString(key)))
}

case class Expire(key: String, seconds: Long) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("EXPIRE", Seq(ByteString(key), ByteString(seconds.toString)))
}

case class Expireat(key: String, seconds: Long) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("EXPIREAT", Seq(ByteString(key), ByteString(seconds.toString)))
}

case class Keys(pattern: String) extends RedisCommandMultiBulk[Seq[String]] {
  val encodedRequest: ByteString = encode("KEYS", Seq(ByteString(pattern)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqString(mb)
}

case class Migrate(host: String, port: Int, key: String, destinationDB: Int, timeout: FiniteDuration)
  extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString =
    encode("MIGRATE",
      Seq(ByteString(host),
        ByteString(port.toString),
        ByteString(key),
        ByteString(destinationDB.toString),
        ByteString(timeout.toMillis.toString)
      ))
}

case class Move(key: String, db: Int) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("MOVE", Seq(ByteString(key), ByteString(db.toString)))
}

case class ObjectRefcount(key: String) extends RedisCommandRedisReplyOptionLong {
  val encodedRequest: ByteString = encode("OBJECT", Seq(ByteString("REFCOUNT"), ByteString(key)))
}

case class ObjectIdletime(key: String) extends RedisCommandRedisReplyOptionLong {
  val encodedRequest: ByteString = encode("OBJECT", Seq(ByteString("IDLETIME"), ByteString(key)))
}


case class ObjectEncoding(key: String) extends RedisCommandBulk[Option[String]] {
  val encodedRequest: ByteString = encode("OBJECT", Seq(ByteString("ENCODING"), ByteString(key)))
  def decodeReply(bulk: Bulk) = bulk.toOptString
}

case class Persist(key: String) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("PERSIST", Seq(ByteString(key)))
}

case class Pexpire(key: String, milliseconds: Long) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("PEXPIRE", Seq(ByteString(key), ByteString(milliseconds.toString)))
}

case class Pexpireat(key: String, millisecondsTimestamp: Long) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("PEXPIREAT", Seq(ByteString(key), ByteString(millisecondsTimestamp.toString)))
}

case class Pttl(key: String) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("PTTL", Seq(ByteString(key)))
}

case object Randomkey extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("RANDOMKEY")
}

case class Rename(key: String, newkey: String) extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("RENAME", Seq(ByteString(key), ByteString(newkey)))
}

case class Renamex(key: String, newkey: String) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("RENAMENX", Seq(ByteString(key), ByteString(newkey)))
}

case class Restore[A](key: String, ttl: Long = 0, serializedValue: A)(implicit convert: RedisValueConverter[A])
  extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("RESTORE", Seq(ByteString(key), ByteString(ttl.toString), convert.from(serializedValue)))
}

private[redis] object Sort {
  def buildArgs(key: String,
                byPattern: Option[String],
                limit: Option[LimitOffsetCount],
                getPatterns: Seq[String],
                order: Option[Order],
                alpha: Boolean,
                store: Option[String] = None): Seq[ByteString] = {
    var args = store.map(dest => List(ByteString("STORE"), ByteString(dest))).getOrElse(List())
    if (alpha) {
      args = ByteString("ALPHA") :: args
    }
    args = order.map(ord => ByteString(ord.toString) :: args).getOrElse(args)
    args = getPatterns.map(pat => List(ByteString("GET"), ByteString(pat))).toList.flatten ++ args
    args = limit.map(_.toByteString).getOrElse(Seq()).toList ++ args
    args = byPattern.map(ByteString("BY") :: ByteString(_) :: args).getOrElse(args)

    ByteString(key) :: args
  }
}

case class Sort(key: String,
                byPattern: Option[String],
                limit: Option[LimitOffsetCount],
                getPatterns: Seq[String],
                order: Option[Order],
                alpha: Boolean) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("SORT", Sort.buildArgs(key, byPattern, limit, getPatterns, order, alpha))
}

case class SortStore(key: String,
                     byPattern: Option[String],
                     limit: Option[LimitOffsetCount],
                     getPatterns: Seq[String],
                     order: Option[Order],
                     alpha: Boolean,
                     store: String) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SORT", Sort.buildArgs(key, byPattern, limit, getPatterns, order, alpha, Some(store)))
}

case class Ttl(key: String) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("TTL", Seq(ByteString(key)))
}

case class Type(key: String) extends RedisCommandStatus[String] {
  val encodedRequest: ByteString = encode("TYPE", Seq(ByteString(key)))

  def decodeReply(s: Status) = s.toString
}