package redis.api.lists

import redis._
import akka.util.ByteString
import redis.api.ListPivot
import redis.protocol.MultiBulk

case class Lindex(key: String, index: Long) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("LINDEX", Seq(ByteString(key), ByteString(index.toString)))
}

case class Linsert[A](key: String, beforeAfter: ListPivot, pivot: String, value: A)
                     (implicit convert: RedisValueConverter[A])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("LINSERT", Seq(ByteString(key), ByteString(beforeAfter.toString), ByteString(pivot), convert.from(value)))
}

case class Llen(key: String) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("LLEN", Seq(ByteString(key)))
}

case class Lpop(key: String) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("LPOP", Seq(ByteString(key)))
}

case class Lpush[A](key: String, values: Seq[A])(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("LPUSH", ByteString(key) +: values.map(v => convert.from(v)))
}


case class Lpushx[A](key: String, value: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("LPUSHX", Seq(ByteString(key), convert.from(value)))
}

case class Lrange(key: String, start: Long, stop: Long) extends RedisCommandMultiBulk[Seq[ByteString]] {
  val encodedRequest: ByteString = encode("LRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString)))

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqByteString(mb)
}

case class Lrem[A](key: String, count: Long, value: A)(implicit convert: RedisValueConverter[A])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("LREM", Seq(ByteString(key), ByteString(count.toString), convert.from(value)))
}

case class Lset[A](key: String, index: Long, value: A)(implicit convert: RedisValueConverter[A])
  extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("LSET", Seq(ByteString(key), ByteString(index.toString), convert.from(value)))
}

case class Ltrim(key: String, start: Long, stop: Long) extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("LTRIM", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString)))
}

case class Rpop(key: String) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("RPOP", Seq(ByteString(key)))
}

case class Rpoplpush(source: String, destination: String) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("RPOPLPUSH", Seq(ByteString(source), ByteString(destination)))
}

case class Rpush[A](key: String, values: Seq[A])(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("RPUSH", ByteString(key) +: values.map(v => convert.from(v)))
}

case class Rpushx[A](key: String, value: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("RPUSHX", Seq(ByteString(key), convert.from(value)))
}