package redis.api.hashes

import redis._
import akka.util.ByteString
import scala.collection.mutable
import scala.annotation.tailrec
import redis.protocol.MultiBulk

case class Hdel(key: String, fields: Seq[String]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("HDEL", ByteString(key) +: fields.map(ByteString.apply))
}

case class Hexists(key: String, field: String) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("HEXISTS", Seq(ByteString(key), ByteString(field)))
}

case class Hget(key: String, field: String) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("HGET", Seq(ByteString(key), ByteString(field)))
}

case class Hgetall(key: String) extends RedisCommandMultiBulk[Map[String, ByteString]] {
  val encodedRequest: ByteString = encode("HGETALL", Seq(ByteString(key)))

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

case class Hincrby(key: String, fields: String, increment: Long) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("HINCRBY", Seq(ByteString(key), ByteString(fields), ByteString(increment.toString)))
}

case class Hincrbyfloat(key: String, fields: String, increment: Double) extends RedisCommandBulkDouble {
  val encodedRequest: ByteString = encode("HINCRBYFLOAT", Seq(ByteString(key), ByteString(fields), ByteString(increment.toString)))
}

case class Hkeys(key: String) extends RedisCommandMultiBulk[Seq[String]] {
  val encodedRequest: ByteString = encode("HKEYS", Seq(ByteString(key)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqString(mb)
}

case class Hlen(key: String) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("HLEN", Seq(ByteString(key)))
}

case class Hmget(key: String, fields: Seq[String]) extends RedisCommandMultiBulk[Seq[Option[ByteString]]] {
  val encodedRequest: ByteString = encode("HMGET", ByteString(key) +: fields.map(ByteString.apply))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqOptionByteString(mb)
}

case class Hmset[A](key: String, keysValues: Map[String, A])(implicit convert: RedisValueConverter[A])
  extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("HMSET", ByteString(key) +: keysValues.foldLeft(Seq.empty[ByteString])({
    case (acc, e) => ByteString(e._1) +: convert.from(e._2) +: acc
  }))
}

case class Hset[A](key: String, field: String, value: A)(implicit convert: RedisValueConverter[A])
  extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("HSET", Seq(ByteString(key), ByteString(field), convert.from(value)))
}

case class Hsetnx[A](key: String, field: String, value: A)(implicit convert: RedisValueConverter[A])
  extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("HSETNX", Seq(ByteString(key), ByteString(field), convert.from(value)))
}

case class Hvals(key: String) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("HVALS", Seq(ByteString(key)))
}