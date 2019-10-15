package redis.api.streams

import akka.util.ByteString
import redis._
import redis.api.{RequestStreamId, StreamEntry, StreamId, TrimStrategy}
import redis.protocol.{Bulk, MultiBulk, RedisReply}

case class Xadd[K, F, V](key: K, id: RequestStreamId, fields: Seq[(F, V)], trimStrategy: Option[TrimStrategy])(implicit serializerK: ByteStringSerializer[K], serializerF: ByteStringSerializer[F], serializerV: ByteStringSerializer[V])
  extends SimpleClusterKey[K] with RedisCommandBulk[StreamId] {
  val isMasterOnly = true

  val encodedRequest: ByteString = {
    val builder = Seq.newBuilder[ByteString]

    builder += keyAsString

    if (trimStrategy.isDefined) {
      builder ++= trimStrategy.get.toByteString
    }

    builder += id.serialize

    fields.foreach { f =>
      builder += serializerF.serialize(f._1)
      builder += serializerV.serialize(f._2)
    }

    encode("XADD", builder.result())
  }

  def decodeReply(bulk: Bulk): StreamId = bulk.response.map(StreamId.deserialize).get
}

case class Xdel[K](key: K, ids: Seq[StreamId])(implicit serializerK: ByteStringSerializer[K])
  extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("XDEL", Seq(keyAsString) ++ ids.map(_.serialize))
}

case class Xlen[K](key: K)(implicit redisKey: ByteStringSerializer[K])
  extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("XLEN", Seq(keyAsString))
}

case class Xrange[K, F, V](key: K, start: RequestStreamId, end: RequestStreamId, count: Option[Long] = None)(implicit serializerK: ByteStringSerializer[K], deserializerF: ByteStringDeserializer[F], deserializerV: ByteStringDeserializer[V])
  extends SimpleClusterKey[K] with RedisCommandMultiBulk[Seq[StreamEntry[F, V]]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("XRANGE", Xrange.buildArgs(key, start, end, count))
  def decodeReply(reply: MultiBulk): Seq[StreamEntry[F, V]] = StreamEntryDecoder.toSeqEntry(reply)
}

private [redis] object Xrange {
  def buildArgs[K](key: K, id1: RequestStreamId, id2: RequestStreamId, count: Option[Long])(implicit serializerK: ByteStringSerializer[K]): Seq[ByteString] = {
    val builder = Seq.newBuilder[ByteString]
    builder += serializerK.serialize(key)
    builder += id1.serialize
    builder += id2.serialize
    if (count.isDefined) {
      builder += ByteString("COUNT")
      builder += ByteString(count.get.toString)
    }
    builder.result()
  }
}

case class Xrevrange[K, F, V](key: K, end: RequestStreamId, start: RequestStreamId, count: Option[Long] = None)(implicit serializerK: ByteStringSerializer[K], deserializerF: ByteStringDeserializer[F], deserializerV: ByteStringDeserializer[V])
  extends SimpleClusterKey[K] with RedisCommandMultiBulk[Seq[StreamEntry[F, V]]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("XREVRANGE", Xrange.buildArgs(key, end, start, count))
  def decodeReply(reply: MultiBulk): Seq[StreamEntry[F, V]] = StreamEntryDecoder.toSeqEntry(reply)
}

case class Xtrim[K](key: K, trimStrategy: TrimStrategy)(implicit serializerK: ByteStringSerializer[K])
  extends SimpleClusterKey[K] with RedisCommandIntegerLong {
  val isMasterOnly = true
  val encodedRequest: ByteString = encode("XTRIM", Seq(keyAsString) ++ trimStrategy.toByteString)
}

case class Xread[K, F, V](streams: Seq[(K, RequestStreamId)], count: Option[Long])(implicit serializerK: ByteStringSerializer[K], deserializerK: ByteStringDeserializer[K], deserializerF: ByteStringDeserializer[F], deserializerV: ByteStringDeserializer[V])
  extends RedisCommandMultiBulk[Option[Seq[(K, Seq[StreamEntry[F, V]])]]] {
  val keys = streams.map(_._1)
  val isMasterOnly = false
  val encodedRequest: ByteString = {
    val builder = Seq.newBuilder[ByteString]

    if (count.isDefined) {
      builder += ByteString("COUNT")
      builder += ByteString(count.get.toString)
    }

    builder += ByteString("STREAMS")
    builder ++= streams.map(p => serializerK.serialize(p._1))
    builder ++= streams.map(p => p._2.serialize)

    encode("XREAD", builder.result())
  }

  def decodeReply(r: MultiBulk): Option[Seq[(K, Seq[StreamEntry[F, V]])]] =
    StreamEntryDecoder.toOptionSeqStringSeqEntry(r)(deserializerK, deserializerF, deserializerV)
}

private [redis] object StreamEntryDecoder {
  def toEntry[F, V](mb: MultiBulk)(implicit deserializerF: ByteStringDeserializer[F], deserializerV: ByteStringDeserializer[V]): StreamEntry[F, V] = {
    val r = mb.responses.get
    val id = StreamId.deserialize(r(0).toByteString)
    val fields = r(1).asInstanceOf[MultiBulk].responses.get.map(_.toByteString).grouped(2).map(p => (deserializerF.deserialize(p(0)), deserializerV.deserialize(p(1)))).toSeq
    StreamEntry(id, fields)
  }

  def toSeqEntry[F, V](mb: MultiBulk)(implicit deserializerF: ByteStringDeserializer[F], deserializerV: ByteStringDeserializer[V]): Seq[StreamEntry[F, V]] = {
    mb.responses.map(r => r.map(_.asInstanceOf[MultiBulk]).map(toEntry[F,V])).getOrElse(Seq())
  }

  def toOptionSeqStringSeqEntry[K, F, V](mb: MultiBulk)(implicit deserializerK: ByteStringDeserializer[K], deserializerF: ByteStringDeserializer[F], deserializerV: ByteStringDeserializer[V]): Option[Seq[(K, Seq[StreamEntry[F, V]])]] =
    mb.responses.map { r =>
      r.map(_.asInstanceOf[MultiBulk]).map(toStringSeqEntry[K,F,V])
    }

  def toStringSeqEntry[K, F, V](mb: MultiBulk)(implicit deserializerK: ByteStringDeserializer[K], deserializerF: ByteStringDeserializer[F], deserializerV: ByteStringDeserializer[V]): (K, Seq[StreamEntry[F, V]]) = {
    val r = mb.responses.get
    val key = deserializerK.deserialize(r(0).toByteString)
    val entries = toSeqEntry[F,V](r(1).asInstanceOf[MultiBulk])
    (key, entries)
  }
}
