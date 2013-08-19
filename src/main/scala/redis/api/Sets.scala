package redis.api.sets

import redis._
import akka.util.ByteString

case class Sadd[A](key: String, members: Seq[A])(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SADD", ByteString(key) +: members.map(v => convert.from(v)))
}

case class Scard(key: String) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SCARD", Seq(ByteString(key)))
}

case class Sdiff(key: String, keys: Seq[String]) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("SDIFF", ByteString(key) +: keys.map(ByteString.apply))
}

case class Sdiffstore(destination: String, key: String, keys: Seq[String]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SDIFFSTORE", ByteString(destination) +: ByteString(key) +: keys.map(ByteString.apply))
}

case class Sinter(key: String, keys: Seq[String]) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("SINTER", ByteString(key) +: keys.map(ByteString.apply))
}

case class Sinterstore(destination: String, key: String, keys: Seq[String]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SINTERSTORE", ByteString(destination) +: ByteString(key) +: keys.map(ByteString.apply))
}

case class Sismember[A](key: String, member: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("SISMEMBER", Seq(ByteString(key), convert.from(member)))
}

case class Smembers(key: String) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("SMEMBERS", Seq(ByteString(key)))
}

case class Smove[A](source: String, destination: String, member: A)(implicit convert: RedisValueConverter[A])
  extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("SMOVE", Seq(ByteString(source), ByteString(destination), convert.from(member)))
}

case class Spop(key: String) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("SPOP", Seq(ByteString(key)))
}

case class Srandmember(key: String) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("SRANDMEMBER", Seq(ByteString(key)))
}

case class Srandmembers(key: String, count: Long) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("SRANDMEMBER", Seq(ByteString(key), ByteString(count.toString)))
}

case class Srem[A](key: String, members: Seq[A])(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SREM", ByteString(key) +: members.map(v => convert.from(v)))
}

case class Sunion(key: String, keys: Seq[String]) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("SUNION", ByteString(key) +: keys.map(ByteString.apply))
}

case class Sunionstore(destination: String, key: String, keys: Seq[String]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SUNIONSTORE", ByteString(destination) +: ByteString(key) +: keys.map(ByteString.apply))
}