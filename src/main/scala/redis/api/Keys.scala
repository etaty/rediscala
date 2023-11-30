package redis.api.keys

import redis._
import org.apache.pekko.util.ByteString
import redis.protocol._
import scala.concurrent.duration.FiniteDuration
import redis.api.Order
import redis.api.LimitOffsetCount


case class Del[K](keys: Seq[K])(implicit redisKey: ByteStringSerializer[K]) extends MultiClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("DEL", keys.map(redisKey.serialize))
}

case class Dump[K, R](key: K)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R]) extends SimpleClusterKey[K] with RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("DUMP", Seq(keyAsString))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Exists[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("EXISTS", Seq(keyAsString))
}

case class ExistsMany[K](keys: Seq[K])(implicit redisKey: ByteStringSerializer[K]) extends MultiClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("EXISTS", keys.map(redisKey.serialize))
}

case class Expire[K](key: K, seconds: Long)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("EXPIRE", Seq(keyAsString, ByteString(seconds.toString)))
}

case class Expireat[K](key: K, seconds: Long)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("EXPIREAT", Seq(keyAsString, ByteString(seconds.toString)))
}

case class Keys(pattern: String) extends RedisCommandMultiBulk[Seq[String]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("KEYS", Seq(ByteString(pattern)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqString(mb)
}

case class Migrate[K](host: String, port: Int, keys: Seq[K], destinationDB: Int, timeout: FiniteDuration, copy: Boolean = false, replace: Boolean = false, password: Option[String])(implicit redisKey: ByteStringSerializer[K])
  extends RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = {
    val builder = Seq.newBuilder[ByteString]

    builder += ByteString(host)
    builder += ByteString(port.toString)
    builder += ByteString("")
    builder += ByteString(destinationDB.toString)
    builder += ByteString(timeout.toMillis.toString)

    if (copy)
      builder += ByteString("COPY")
    if (replace)
      builder += ByteString("REPLACE")
    if (password.isDefined) {
      builder += ByteString("AUTH")
      builder += ByteString(password.get)
    }

    builder += ByteString("KEYS")
    builder ++= keys.map(redisKey.serialize)

    RedisProtocolRequest.multiBulk("MIGRATE", builder.result())
  }
}

case class Move[K](key: K, db: Int)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("MOVE", Seq(keyAsString, ByteString(db.toString)))
}

case class ObjectRefcount[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandRedisReplyOptionLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("OBJECT", Seq(ByteString("REFCOUNT"), keyAsString))
}

case class ObjectIdletime[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandRedisReplyOptionLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("OBJECT", Seq(ByteString("IDLETIME"), keyAsString))
}


case class ObjectEncoding[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandBulk[Option[String]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("OBJECT", Seq(ByteString("ENCODING"), keyAsString))

  def decodeReply(bulk: Bulk) = bulk.toOptString
}

case class Persist[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("PERSIST", Seq(keyAsString))
}

case class Pexpire[K](key: K, milliseconds: Long)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("PEXPIRE", Seq(keyAsString, ByteString(milliseconds.toString)))
}

case class Pexpireat[K](key: K, millisecondsTimestamp: Long)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("PEXPIREAT", Seq(keyAsString, ByteString(millisecondsTimestamp.toString)))
}

case class Pttl[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("PTTL", Seq(keyAsString))
}

case class Randomkey[R]()(implicit deserializerR: ByteStringDeserializer[R]) extends RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("RANDOMKEY")
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Rename[K, NK](key: K, newkey: NK)(implicit redisKey: ByteStringSerializer[K], newKeySer: ByteStringSerializer[NK]) extends SimpleClusterKey[K] with RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("RENAME", Seq(keyAsString, newKeySer.serialize(newkey)))
}

case class Renamex[K, NK](key: K, newkey: NK)(implicit redisKey: ByteStringSerializer[K], newKeySer: ByteStringSerializer[NK]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("RENAMENX", Seq(keyAsString, newKeySer.serialize(newkey)))
}

case class Restore[K, V](key: K, ttl: Long = 0, serializedValue: V)(implicit redisKey: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends SimpleClusterKey[K] with RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("RESTORE", Seq(keyAsString, ByteString(ttl.toString), convert.serialize(serializedValue)))
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

case class Ttl[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("TTL", Seq(keyAsString))
}

case class Type[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandStatusString {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("TYPE", Seq(keyAsString))
}

case class Scan[C](cursor: C, count: Option[Int], matchGlob: Option[String])(implicit redisCursor: ByteStringSerializer[C], deserializer: ByteStringDeserializer[String]) extends RedisCommandMultiBulkCursor[Seq[String]] {
  val encodedRequest: ByteString = encode("SCAN", withOptionalParams(Seq(redisCursor.serialize(cursor))))

  val isMasterOnly = false

  def decodeResponses(responses: Seq[RedisReply]) =
    responses.map(response => deserializer.deserialize(response.toByteString))

  val empty: Seq[String] = Seq.empty
}

case class Unlink[K](keys: Seq[K])(implicit redisKey: ByteStringSerializer[K]) extends MultiClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("UNLINK", keys.map(redisKey.serialize))
}


