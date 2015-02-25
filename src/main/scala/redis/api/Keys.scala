package redis.api.keys

import redis._
import akka.util.ByteString
import redis.protocol.{Bulk, Status, MultiBulk}
import scala.concurrent.duration.FiniteDuration
import redis.api.Order
import redis.api.LimitOffsetCount
import scala.Some


case class Del[K](keys: Seq[K])(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("DEL", keys.map(redisKey.serialize))
}

case class Dump[K, R](key: K)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R]) extends RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("DUMP", Seq(redisKey.serialize(key)))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Exists[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("EXISTS", Seq(redisKey.serialize(key)))
}

case class Expire[K](key: K, seconds: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("EXPIRE", Seq(redisKey.serialize(key), ByteString(seconds.toString)))
}

case class Expireat[K](key: K, seconds: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("EXPIREAT", Seq(redisKey.serialize(key), ByteString(seconds.toString)))
}

case class Keys(pattern: String) extends RedisCommandMultiBulk[Seq[String]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("KEYS", Seq(ByteString(pattern)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqString(mb)
}

case class Migrate[K](host: String, port: Int, key: K, destinationDB: Int, timeout: FiniteDuration)(implicit redisKey: ByteStringSerializer[K])
  extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString =
    encode("MIGRATE",
      Seq(ByteString(host),
        ByteString(port.toString),
        redisKey.serialize(key),
        ByteString(destinationDB.toString),
        ByteString(timeout.toMillis.toString)
      ))
}

case class Move[K](key: K, db: Int)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("MOVE", Seq(redisKey.serialize(key), ByteString(db.toString)))
}

case class ObjectRefcount[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandRedisReplyOptionLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("OBJECT", Seq(ByteString("REFCOUNT"), redisKey.serialize(key)))
}

case class ObjectIdletime[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandRedisReplyOptionLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("OBJECT", Seq(ByteString("IDLETIME"), redisKey.serialize(key)))
}


case class ObjectEncoding[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandBulk[Option[String]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("OBJECT", Seq(ByteString("ENCODING"), redisKey.serialize(key)))

  def decodeReply(bulk: Bulk) = bulk.toOptString
}

case class Persist[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("PERSIST", Seq(redisKey.serialize(key)))
}

case class Pexpire[K](key: K, milliseconds: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("PEXPIRE", Seq(redisKey.serialize(key), ByteString(milliseconds.toString)))
}

case class Pexpireat[K](key: K, millisecondsTimestamp: Long)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("PEXPIREAT", Seq(redisKey.serialize(key), ByteString(millisecondsTimestamp.toString)))
}

case class Pttl[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("PTTL", Seq(redisKey.serialize(key)))
}

case class Randomkey[R](implicit deserializerR: ByteStringDeserializer[R]) extends RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("RANDOMKEY")
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Rename[K, NK](key: K, newkey: NK)(implicit redisKey: ByteStringSerializer[K], newKeySer: ByteStringSerializer[NK]) extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("RENAME", Seq(redisKey.serialize(key), newKeySer.serialize(newkey)))
}

case class Renamex[K, NK](key: K, newkey: NK)(implicit redisKey: ByteStringSerializer[K], newKeySer: ByteStringSerializer[NK]) extends RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("RENAMENX", Seq(redisKey.serialize(key), newKeySer.serialize(newkey)))
}

case class Restore[K, V](key: K, ttl: Long = 0, serializedValue: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("RESTORE", Seq(redisKey.serialize(key), ByteString(ttl.toString), convert.serialize(serializedValue)))
}

private[redis] object Sort {
  def buildArgs[K, KS](key: K,
                       byPattern: Option[String],
                       limit: Option[LimitOffsetCount],
                       getPatterns: Seq[String],
                       order: Option[Order],
                       alpha: Boolean,
                       store: Option[KS] = None)(implicit redisKey: ByteStringSerializer[K], bsStore: ByteStringSerializer[KS]): Seq[ByteString] = {
    var args = store.map(dest => List(ByteString("STORE"), bsStore.serialize(dest))).getOrElse(List())
    if (alpha) {
      args = ByteString("ALPHA") :: args
    }
    args = order.map(ord => ByteString(ord.toString) :: args).getOrElse(args)
    args = getPatterns.map(pat => List(ByteString("GET"), ByteString(pat))).toList.flatten ++ args
    args = limit.map(_.toByteString).getOrElse(Seq()).toList ++ args
    args = byPattern.map(ByteString("BY") :: ByteString(_) :: args).getOrElse(args)

    redisKey.serialize(key) :: args
  }
}

case class Sort[K: ByteStringSerializer, R](key: K,
                                            byPattern: Option[String],
                                            limit: Option[LimitOffsetCount],
                                            getPatterns: Seq[String],
                                            order: Option[Order],
                                            alpha: Boolean)
                                           (implicit deserializerR: ByteStringDeserializer[R])
  extends RedisCommandMultiBulkSeqByteString[R] {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SORT", Sort.buildArgs(key, byPattern, limit, getPatterns, order, alpha))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class SortStore[K: ByteStringSerializer, KS: ByteStringSerializer](key: K,
                                                                        byPattern: Option[String],
                                                                        limit: Option[LimitOffsetCount],
                                                                        getPatterns: Seq[String],
                                                                        order: Option[Order],
                                                                        alpha: Boolean,
                                                                        store: KS) extends RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("SORT", Sort.buildArgs(key, byPattern, limit, getPatterns, order, alpha, Some(store)))
}

case class Ttl[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("TTL", Seq(redisKey.serialize(key)))
}

case class Type[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandStatusString {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("TYPE", Seq(redisKey.serialize(key)))
}