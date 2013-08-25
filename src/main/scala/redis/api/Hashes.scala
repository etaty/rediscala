package redis.api.hashes

import redis._
import akka.util.ByteString
import scala.collection.mutable
import scala.annotation.tailrec
import redis.protocol.MultiBulk

case class Hdel[K, KK](key: K, fields: Seq[KK])(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("HDEL", redisKey.serialize(key) +: fields.map(redisFields.serialize))
}

case class Hexists[K, KK](key: K, field: KK)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK]) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("HEXISTS", Seq(redisKey.serialize(key), redisFields.serialize(field)))
}

case class Hget[K, KK](key: K, field: KK)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK]) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("HGET", Seq(redisKey.serialize(key), redisFields.serialize(field)))
}

case class Hgetall[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandMultiBulk[Map[String, ByteString]] {
  val encodedRequest: ByteString = encode("HGETALL", Seq(redisKey.serialize(key)))

  def decodeReply(mb: MultiBulk) = mb.responses.map(r => {
    val seq = r.map(_.toByteString)
    val builder = Map.newBuilder[String, ByteString]
    seqToMap(seq, builder)
    builder.result()
  }).get

  @tailrec
  private def seqToMap(seq: Seq[ByteString], builder: mutable.Builder[(String, ByteString), Map[String, ByteString]]): Unit = {
    if (seq.nonEmpty) {
      builder += (seq.head.utf8String -> seq.tail.head)
      seqToMap(seq.tail.tail, builder)
    }
  }
}

case class Hincrby[K, KK](key: K, fields: KK, increment: Long)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("HINCRBY", Seq(redisKey.serialize(key), redisFields.serialize(fields), ByteString(increment.toString)))
}

case class Hincrbyfloat[K, KK](key: K, fields: KK, increment: Double)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK])
  extends RedisCommandBulkDouble {
  val encodedRequest: ByteString = encode("HINCRBYFLOAT", Seq(redisKey.serialize(key), redisFields.serialize(fields), ByteString(increment.toString)))
}

case class Hkeys[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandMultiBulk[Seq[String]] {
  val encodedRequest: ByteString = encode("HKEYS", Seq(redisKey.serialize(key)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqString(mb)
}

case class Hlen[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("HLEN", Seq(redisKey.serialize(key)))
}

case class Hmget[K, KK](key: K, fields: Seq[KK])(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK])
  extends RedisCommandMultiBulk[Seq[Option[ByteString]]] {
  val encodedRequest: ByteString = encode("HMGET", redisKey.serialize(key) +: fields.map(redisFields.serialize))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqOptionByteString(mb)
}

case class Hmset[K, KK, V](key: K, keysValues: Map[KK, V])(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK], convert: ByteStringSerializer[V])
  extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("HMSET", redisKey.serialize(key) +: keysValues.foldLeft(Seq.empty[ByteString])({
    case (acc, e) => redisFields.serialize(e._1) +: convert.serialize(e._2) +: acc
  }))
}

case class Hset[K, KK, V](key: K, field: KK, value: V)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK], convert: ByteStringSerializer[V])
  extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("HSET", Seq(redisKey.serialize(key), redisFields.serialize(field), convert.serialize(value)))
}

case class Hsetnx[K, KK, V](key: K, field: KK, value: V)(implicit redisKey: ByteStringSerializer[K], redisFields: ByteStringSerializer[KK], convert: ByteStringSerializer[V])
  extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("HSETNX", Seq(redisKey.serialize(key), redisFields.serialize(field), convert.serialize(value)))
}

case class Hvals[K](key: K)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("HVALS", Seq(redisKey.serialize(key)))
}