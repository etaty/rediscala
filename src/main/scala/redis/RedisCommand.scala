package redis

import org.apache.pekko.util.ByteString
import redis.protocol._


trait RedisCommand[RedisReplyT <: RedisReply, +T] {
  val isMasterOnly: Boolean
  val encodedRequest: ByteString

  def decodeReply(r: RedisReplyT): T

  val decodeRedisReply: PartialFunction[ByteString, DecodeResult[RedisReplyT]]

  def encode(command: String) = RedisProtocolRequest.inline(command)

  def encode(command: String, args: Seq[ByteString]) = RedisProtocolRequest.multiBulk(command, args)
}


trait RedisCommandStatus[T] extends RedisCommand[Status, T] {
  val decodeRedisReply: PartialFunction[ByteString, DecodeResult[Status]] = RedisProtocolReply.decodeReplyStatus
}

trait RedisCommandInteger[T] extends RedisCommand[Integer, T] {
  val decodeRedisReply: PartialFunction[ByteString, DecodeResult[Integer]] = RedisProtocolReply.decodeReplyInteger
}

trait RedisCommandBulk[T] extends RedisCommand[Bulk, T] {
  val decodeRedisReply: PartialFunction[ByteString, DecodeResult[Bulk]] = RedisProtocolReply.decodeReplyBulk
}

trait RedisCommandMultiBulk[T] extends RedisCommand[MultiBulk, T] {
  val decodeRedisReply: PartialFunction[ByteString, DecodeResult[MultiBulk]] = RedisProtocolReply.decodeReplyMultiBulk
}

trait RedisCommandRedisReply[T] extends RedisCommand[RedisReply, T] {
  val decodeRedisReply: PartialFunction[ByteString, DecodeResult[RedisReply]] = RedisProtocolReply.decodeReplyPF
}

trait RedisCommandStatusString extends RedisCommandStatus[String] {
  def decodeReply(s: Status) = s.toString
}

trait RedisCommandStatusBoolean extends RedisCommandStatus[Boolean] {
  def decodeReply(s: Status): Boolean = s.toBoolean
}

trait RedisCommandIntegerBoolean extends RedisCommandInteger[Boolean] {
  def decodeReply(i: Integer): Boolean = i.toBoolean
}

trait RedisCommandIntegerLong extends RedisCommandInteger[Long] {
  def decodeReply(i: Integer) = i.toLong
}

trait RedisCommandBulkOptionByteString[R] extends RedisCommandBulk[Option[R]] {
  val deserializer: ByteStringDeserializer[R]

  def decodeReply(bulk: Bulk) = bulk.response.map(deserializer.deserialize)
}

trait RedisCommandBulkDouble extends RedisCommandBulk[Double] {
  def decodeReply(bulk: Bulk) = bulk.response.map(ByteStringDeserializer.RedisDouble.deserialize).get
}

trait RedisCommandBulkOptionDouble extends RedisCommandBulk[Option[Double]] {
  def decodeReply(bulk: Bulk) = bulk.response.map(ByteStringDeserializer.RedisDouble.deserialize)
}

trait RedisCommandMultiBulkSeqByteString[R] extends RedisCommandMultiBulk[Seq[R]] {
  val deserializer: ByteStringDeserializer[R]

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqByteString(mb)(deserializer)
}

trait RedisCommandMultiBulkSeqByteStringDouble[R] extends RedisCommandMultiBulk[Seq[(R, Double)]] {
  val deserializer: ByteStringDeserializer[R]

  def decodeReply(mb: MultiBulk) = MultiBulkConverter.toSeqTuple2ByteStringDouble(mb)(deserializer)
}

case class Cursor[T](index: Int, data: T)

trait RedisCommandMultiBulkCursor[R] extends RedisCommandMultiBulk[Cursor[R]] {
  def decodeReply(mb: MultiBulk) = {
    mb.responses.map { responses =>
      val cursor = ParseNumber.parseInt(responses.head.toByteString)
      val remainder = responses(1).asInstanceOf[MultiBulk]

      Cursor(cursor, remainder.responses.map(decodeResponses).getOrElse(empty))
    }.getOrElse(Cursor(0, empty))
  }

  def decodeResponses(responses: Seq[RedisReply]): R

  val empty: R
  val count: Option[Int]
  val matchGlob: Option[String]

  def withOptionalParams(params: Seq[ByteString]): Seq[ByteString] = {
    val withCount = count.fold(params)(c => params ++ Seq(ByteString("COUNT"), ByteString(c.toString)))
    matchGlob.fold(withCount)(m => withCount ++ Seq(ByteString("MATCH"), ByteString(m)))
  }
}

trait RedisCommandRedisReplyOptionLong extends RedisCommandRedisReply[Option[Long]] {
  def decodeReply(redisReply: RedisReply): Option[Long] = redisReply match {
    case i: Integer => Some(i.toLong)
    case _ => None
  }
}

trait RedisCommandRedisReplyRedisReply[R] extends RedisCommandRedisReply[R] {
  val deserializer: RedisReplyDeserializer[R]

  def decodeReply(redisReply: RedisReply): R = {
    if (deserializer.deserialize.isDefinedAt(redisReply))
      deserializer.deserialize.apply(redisReply)
    else
      throw new RuntimeException("Could not deserialize") // todo make own type
  }
}