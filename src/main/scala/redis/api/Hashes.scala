package redis.api.hashes

import redis._
import org.apache.pekko.util.ByteString
import scala.collection.mutable
import scala.annotation.tailrec
import redis.protocol.{RedisReply, MultiBulk}

case class Hdel[K, KK](key: K, fields: Seq[KK])(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("HDEL", keyAsString +: fields.map(redisFields.serialize))
}

case class Hexists[K, KK](key: K, field: KK)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK]) extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("HEXISTS", Seq(keyAsString, redisFields.serialize(field)))
}

case class Hget[K, KK, R](key: K, field: KK)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK], deserializerR: ByteStringDeserializer[R])
  extends SimpleClusterKey[K] with RedisCommandBulkOptionByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("HGET", Seq(keyAsString, redisFields.serialize(field)))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class Hgetall[K, R](key: K)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R]) extends SimpleClusterKey[K] with RedisCommandMultiBulk[Map[String, R]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("HGETALL", Seq(keyAsString))

  def decodeReply(mb: MultiBulk) = mb.responses.map(r => {
    val builder = Map.newBuilder[String, R]
    builder.sizeHint(r.length / 2)
    seqToMap(r, builder)
    builder.result()
  }).get

  @tailrec
  private def seqToMap(seq: Vector[RedisReply], builder: mutable.Builder[(String, R), Map[String, R]]): Unit = {
    if (seq.nonEmpty) {
      val head = seq.head.toByteString
      val tail = seq.tail
      builder += (head.utf8String -> deserializerR.deserialize(tail.head.toByteString))
      seqToMap(tail.tail, builder)
    }
  }
}

case class Hincrby[K, KK](key: K, fields: KK, increment: Long)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK])
  extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("HINCRBY", Seq(keyAsString, redisFields.serialize(fields), ByteString(increment.toString)))
}

case class Hincrbyfloat[K, KK](key: K, fields: KK, increment: Double)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK])
  extends SimpleClusterKey[K] with RedisCommandBulkDouble {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("HINCRBYFLOAT", Seq(keyAsString, redisFields.serialize(fields), ByteString(increment.toString)))
}

case class Hkeys[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandMultiBulk[Seq[String]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("HKEYS", Seq(keyAsString))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqString(mb)
}

case class Hlen[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("HLEN", Seq(keyAsString))
}

case class Hmget[K, KK, R](key: K, fields: Seq[KK])(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK], deserializerR: ByteStringDeserializer[R])
  extends SimpleClusterKey[K] with RedisCommandMultiBulk[Seq[Option[R]]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("HMGET", keyAsString +: fields.map(redisFields.serialize))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqOptionByteString(mb)
}

case class Hmset[K, KK, V](key: K, keysValues: Map[KK, V])(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK], convert: ByteStringSerializer[V])
  extends SimpleClusterKey[K] with RedisCommandStatusBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("HMSET", keyAsString +: keysValues.foldLeft(Seq.empty[ByteString])({
    case (acc, e) => redisFields.serialize(e._1) +: convert.serialize(e._2) +: acc
  }))
}

case class Hset[K, KK, V](key: K, field: KK, value: V)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK], convert: ByteStringSerializer[V])
  extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("HSET", Seq(keyAsString, redisFields.serialize(field), convert.serialize(value)))
}

case class Hsetnx[K, KK, V](key: K, field: KK, value: V)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK], convert: ByteStringSerializer[V])
  extends SimpleClusterKey[K] with RedisCommandIntegerBoolean {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("HSETNX", Seq(keyAsString, redisFields.serialize(field), convert.serialize(value)))
}

case class Hvals[K, R](key: K)(implicit redisKey: ByteStringSerializer[K], deserializerR: ByteStringDeserializer[R]) extends SimpleClusterKey[K] with RedisCommandMultiBulkSeqByteString[R] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("HVALS", Seq(keyAsString))
  val deserializer: ByteStringDeserializer[R] = deserializerR
}

case class HScan[K, C, R](key: K, cursor: C, count: Option[Int], matchGlob: Option[String])(implicit redisKey: ByteStringSerializer[K], deserializer: ByteStringDeserializer[R], cursorConverter: ByteStringSerializer[C]) extends SimpleClusterKey[K] with RedisCommandMultiBulkCursor[Map[String, R]] {
  val isMasterOnly: Boolean = false

  val encodedRequest: ByteString = encode("HSCAN", withOptionalParams(Seq(keyAsString, cursorConverter.serialize(cursor))))

  def decodeResponses(responses: Seq[RedisReply]) =
    responses.grouped(2).map { xs =>
      val k = xs.head
      val v = xs(1)

      k.toByteString.utf8String -> deserializer.deserialize(v.toByteString)
    }.toMap

  val empty: Map[String, R] = Map.empty
}