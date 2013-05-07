package redis.commands

import redis.{RedisValueConverter, MultiBulkConverter, Request}
import akka.util.{ByteString, Timeout}
import scala.concurrent.{Future, ExecutionContext}
import redis.protocol.{MultiBulk, Bulk, Integer}
import scala.util.Try

trait Sets extends Request {

  def sadd[A](key: String, members: A*)(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("SADD", ByteString(key) +: members.map(v => convert.from(v))).mapTo[Integer].map(_.toLong)

  def scard(key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("SCARD", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def sdiff(key: String, keys: String*)(implicit convert: MultiBulkConverter[Seq[ByteString]], timeout: Timeout, ec: ExecutionContext): Future[Try[Seq[ByteString]]] =
    send("SDIFF", ByteString(key) +: keys.map(ByteString.apply)).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])

  def sdiffstore(destination: String, key: String, keys: String*)(implicit convert: MultiBulkConverter[Seq[ByteString]], timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("SDIFFSTORE", ByteString(destination) +: ByteString(key) +: keys.map(ByteString.apply)).mapTo[Integer].map(_.toLong)

  def sinter(key: String, keys: String*)(implicit convert: MultiBulkConverter[Seq[ByteString]], timeout: Timeout, ec: ExecutionContext): Future[Try[Seq[ByteString]]] =
    send("SINTER", ByteString(key) +: keys.map(ByteString.apply)).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])

  def sinterstore(destination: String, key: String, keys: String*)(implicit convert: MultiBulkConverter[Seq[ByteString]], timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("SINTERSTORE", ByteString(destination) +: ByteString(key) +: keys.map(ByteString.apply)).mapTo[Integer].map(_.toLong)

  def sismember[A](key: String, member: A)(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("SISMEMBER", Seq(ByteString(key), convert.from(member))).mapTo[Integer].map(_.toBoolean)

  def smembers(key: String)(implicit convert: MultiBulkConverter[Seq[ByteString]], timeout: Timeout, ec: ExecutionContext): Future[Try[Seq[ByteString]]] =
    send("SMEMBERS", Seq(ByteString(key))).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])

  def smove[A](source: String, destination: String, member: A)(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Boolean] =
    send("SMOVE", Seq(ByteString(source), ByteString(destination), convert.from(member))).mapTo[Integer].map(_.toBoolean)

  def spop[A](key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[ByteString]] =
    send("SPOP", Seq(ByteString(key))).mapTo[Bulk].map(_.response)

  def srandmember[A](key: String)(implicit timeout: Timeout, ec: ExecutionContext): Future[Option[ByteString]] =
    send("SRANDMEMBER", Seq(ByteString(key))).mapTo[Bulk].map(_.response)

  def srandmember[A](key: String, count: Long)(implicit convert: MultiBulkConverter[Seq[ByteString]], timeout: Timeout, ec: ExecutionContext): Future[Try[Seq[ByteString]]] =
    send("SRANDMEMBER", Seq(ByteString(key), ByteString(count.toString))).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])

  def srem[A](key: String, members: A*)(implicit convert: RedisValueConverter[A], timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("SREM", ByteString(key) +: members.map(v => convert.from(v))).mapTo[Integer].map(_.toLong)

  def sunion(key: String, keys: String*)(implicit convert: MultiBulkConverter[Seq[ByteString]], timeout: Timeout, ec: ExecutionContext): Future[Try[Seq[ByteString]]] =
    send("SUNION", ByteString(key) +: keys.map(ByteString.apply)).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])

  def sunionstore(destination: String, key: String, keys: String*)(implicit timeout: Timeout, ec: ExecutionContext): Future[Long] =
    send("SUNIONSTORE", ByteString(destination) +: ByteString(key) +: keys.map(ByteString.apply)).mapTo[Integer].map(_.toLong)

}
