package redis.api.strings

import redis._
import akka.util.ByteString
import redis.protocol._
import redis.api.BitOperator
import redis.protocol.MultiBulk

case class Append[A](key: String, value: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("APPEND", Seq(ByteString(key), convert.from(value)))
}

case class Bitcount(key: String) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("BITCOUNT", Seq(ByteString(key)))
}

case class BitcountRange(key: String, start: Long, end: Long) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("BITCOUNT", Seq(ByteString(key), ByteString(start.toString), ByteString(end.toString)))
}

case class Bitop(operation: BitOperator, destkey: String, keys: Seq[String]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("BITOP", Seq(ByteString(operation.toString), ByteString(destkey)) ++ keys.map(ByteString.apply))
}

case class Decr(key: String) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("DECR", Seq(ByteString(key)))
}

case class Decrby(key: String, decrement: Long) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("DECRBY", Seq(ByteString(key), ByteString(decrement.toString)))
}

case class Get[T](key: String)(implicit bsDeserializer: ByteStringDeserializer[T]) extends RedisCommandBulk[Option[T]] {
  val encodedRequest: ByteString = RedisProtocolRequest.multiBulk("GET", Seq(ByteString(key)))

  def decodeReply(bulk: Bulk): Option[T] = bulk.response.map(bsDeserializer.deserialize)
}

case class Getbit(key: String, offset: Long) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("GETBIT", Seq(ByteString(key), ByteString(offset.toString)))
}

case class Getrange(key: String, start: Long, end: Long) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("GETRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(end.toString)))
}

case class Getset[A](key: String, value: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandBulkOptionByteString {
  val encodedRequest: ByteString = encode("GETSET", Seq(ByteString(key), convert.from(value)))
}

case class Incr(key: String) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("INCR", Seq(ByteString(key)))
}

case class Incrby(key: String, increment: Long) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("INCRBY", Seq(ByteString(key), ByteString(increment.toString)))
}

case class Incrbyfloat(key: String, increment: Double) extends RedisCommandBulkOptionDouble {
  val encodedRequest: ByteString = encode("INCRBYFLOAT", Seq(ByteString(key), ByteString(increment.toString)))
}

case class Mget(keys: Seq[String]) extends RedisCommandMultiBulk[Seq[Option[ByteString]]] {
  val encodedRequest: ByteString = encode("MGET", keys.map(ByteString.apply))

  def decodeReply(mb: MultiBulk) = mb.responses.map(res => {
    res.map(_.asOptByteString)
  }).get
}

case class Mset[A](keysValues: Map[String, A])(implicit convert: RedisValueConverter[A]) extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("MSET",
    keysValues.foldLeft(Seq[ByteString]())({
      case (acc, e) => ByteString(e._1) +: convert.from(e._2) +: acc
    }))
}

case class Msetnx[A](keysValues: Map[String, A])(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("MSETNX", keysValues.foldLeft(Seq[ByteString]())({
    case (acc, e) => ByteString(e._1) +: convert.from(e._2) +: acc
  }))
}

case class Psetex[A](key: String, milliseconds: Long, value: A)(implicit convert: RedisValueConverter[A])
  extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("PSETEX", Seq(ByteString(key), ByteString(milliseconds.toString), convert.from(value)))
}

case class Set[A](key: String, value: A, exSeconds: Option[Long] = None, pxMilliseconds: Option[Long] = None,
                  NX: Boolean = false, XX: Boolean = false)
                 (implicit convert: RedisValueConverter[A]) extends RedisCommandRedisReply[Boolean] {
  val encodedRequest: ByteString = {
    val seq = if (NX) Seq(ByteString("NX")) else if (XX) Seq(ByteString("XX")) else Seq.empty[ByteString]
    val options: Seq[ByteString] = exSeconds.map(t => Seq(ByteString("EX"), ByteString(t.toString)))
      .orElse(pxMilliseconds.map(t => Seq(ByteString("PX"), ByteString(t.toString))))
      .getOrElse(seq)
    val args = ByteString(key) +: convert.from(value) +: options
    RedisProtocolRequest.multiBulk("SET", args)
  }

  def decodeReply(redisReply: RedisReply) = redisReply match {
    case s: Status => s.toBoolean
    case _ => false
  }
}

case class Setbit(key: String, offset: Long, value: Boolean) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("SETBIT", Seq(ByteString(key), ByteString(offset.toString), ByteString(if (value) "1" else "0")))
}

case class Setex[A](key: String, seconds: Long, value: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandStatusBoolean {
  val encodedRequest: ByteString = encode("SETEX", Seq(ByteString(key), ByteString(seconds.toString), convert.from(value)))
}

case class Setnx[A](key: String, value: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerBoolean {
  val encodedRequest: ByteString = encode("SETNX", Seq(ByteString(key), convert.from(value)))
}

case class Setrange[A](key: String, offset: Long, value: A)(implicit convert: RedisValueConverter[A])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("SETRANGE", Seq(ByteString(key), ByteString(offset.toString), convert.from(value)))
}

case class Strlen(key: String) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("STRLEN", Seq(ByteString(key)))
}