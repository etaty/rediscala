package redis.commands

import redis.api._
import redis.api.streams._
import redis.{ByteStringDeserializer, ByteStringSerializer, Request}

import scala.concurrent.Future

trait Streams extends Request {
  def xadd[V: ByteStringSerializer](key: String, id: RequestStreamId, fields: (String, V)*): Future[StreamId] =
    send(Xadd(key, id, fields, None))

  def xadd[V: ByteStringSerializer](key: String, trimStrategy: TrimStrategy, id: RequestStreamId, fields: (String, V)*): Future[StreamId] =
    send(Xadd(key, id, fields, Some(trimStrategy)))

  def xdel(key: String, ids: StreamId*): Future[Long] =
    send(Xdel(key, ids))

  def xlen(key: String): Future[Long] =
    send(Xlen(key))

  def xrange[V: ByteStringDeserializer](key: String, start: RequestStreamId, end: RequestStreamId, count: Option[Long] = None): Future[Seq[StreamEntry[String, V]]] =
    send(Xrange(key, start, end, count))

  def xrevrange[V: ByteStringDeserializer](key: String, end: RequestStreamId, start: RequestStreamId, count: Option[Long] = None): Future[Seq[StreamEntry[String, V]]] =
    send(Xrevrange(key, end, start, count))

  def xread[V: ByteStringDeserializer](streams: (String, RequestStreamId)*): Future[Option[Seq[(String, Seq[StreamEntry[String, V]])]]] =
    send(Xread(streams, None))

  def xread[V: ByteStringDeserializer](count: Long, streams: (String, RequestStreamId)*): Future[Option[Seq[(String, Seq[StreamEntry[String, V]])]]] =
    send(Xread(streams, count = Some(count)))

  def xtrim(key: String, trimStrategy: TrimStrategy): Future[Long] =
    send(Xtrim(key, trimStrategy))
}
