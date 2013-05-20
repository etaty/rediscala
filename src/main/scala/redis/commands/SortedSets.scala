package redis.commands

import redis.{RedisValueConverter, MultiBulkConverter, Request}
import akka.util.{ByteString, Timeout}
import scala.concurrent.{Future, ExecutionContext}
import redis.protocol.{MultiBulk, Bulk, Integer}
import scala.util.Try

trait SortedSets extends Request {

  def zadd[A](key: String, scoreMembers: (Double, A)*)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Long] =
    send("ZADD", ByteString(key) +: scoreMembers.foldLeft(Seq.empty[ByteString])({
      case (acc, e) => ByteString(e._1.toString) +: convert.from(e._2) +: acc
    })).mapTo[Integer].map(_.toLong)

  def zcard(key: String)(implicit ec: ExecutionContext): Future[Long] =
    send("ZCARD", Seq(ByteString(key))).mapTo[Integer].map(_.toLong)

  def zcount(key: String, min: Limit = Limit(Double.NegativeInfinity), max: Limit = Limit(Double.PositiveInfinity))(implicit ec: ExecutionContext): Future[Long] =
    send("ZCOUNT", Seq(ByteString(key), min.toByteString, max.toByteString)).mapTo[Integer].map(_.toLong)

  def zincrby[A](key: String, increment: Double, member: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Double] =
    send("ZINCRBY", Seq(ByteString(key), ByteString(increment.toString), convert.from(member))).mapTo[Bulk].map(_.response.map(v => java.lang.Double.valueOf(v.utf8String)).get)

  private def zStore(command: String, destination: String, key: String, keys: Seq[String], aggregate: Aggregate = SUM)(implicit convert: MultiBulkConverter[Seq[ByteString]], ec: ExecutionContext): Future[Long] =
    send(command, (ByteString(destination) +: ByteString(key) +: keys.map(ByteString.apply)) ++ Seq(ByteString("AGGREGATE"), ByteString(aggregate.toString))).mapTo[Integer].map(_.toLong)

  def zinterstore(destination: String, key: String, keys: Seq[String], aggregate: Aggregate = SUM)(implicit convert: MultiBulkConverter[Seq[ByteString]], ec: ExecutionContext): Future[Long] =
    zStore("ZINTERSTORE", destination, key, keys, aggregate)

  private def zStoreWeighted(command: String, destination: String, keys: Seq[(String, Double)], aggregate: Aggregate = SUM)(implicit convert: MultiBulkConverter[Seq[ByteString]], ec: ExecutionContext): Future[Long] =
    send(command, (ByteString(destination) +: ByteString(keys.size.toString) +: keys.map {
      k => ByteString(k._1)
    }) ++ (ByteString("WEIGHTS") +: keys.map {
      k => ByteString(k._2.toString)
    }) ++ Seq(ByteString("AGGREGATE"), ByteString(aggregate.toString))).mapTo[Integer].map(_.toLong)

  def zinterstoreWeighted(destination: String, keys: Seq[(String, Double)], aggregate: Aggregate = SUM)(implicit convert: MultiBulkConverter[Seq[ByteString]], ec: ExecutionContext): Future[Long] =
    zStoreWeighted("ZINTERSTORE", destination, keys, aggregate)

  def zrange(key: String, start: Long, stop: Long)(implicit convert: MultiBulkConverter[Seq[ByteString]], ec: ExecutionContext): Future[Try[Seq[ByteString]]] =
    send("ZRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString))).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])

  def zrangeWithscores(key: String, start: Long, stop: Long)(implicit convert: MultiBulkConverter[Seq[(ByteString, Double)]], ec: ExecutionContext): Future[Try[Seq[(ByteString, Double)]]] =
    send("ZRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString), ByteString("WITHSCORES"))).mapTo[MultiBulk].map(_.asTry[Seq[(ByteString, Double)]])

  private def zrangebyscore(command: String, args: Seq[ByteString], limit: Option[(Long, Long)])(implicit ec: ExecutionContext): Future[Any] = {
    val l = limit.map {
      l => Seq(ByteString("LIMIT"), ByteString(l._1.toString), ByteString(l._2.toString))
    }.getOrElse(Seq())
    send(command, args ++ l)
  }

  def zrangebyscore(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)(implicit convert: MultiBulkConverter[Seq[ByteString]], ec: ExecutionContext): Future[Try[Seq[ByteString]]] =
    zrangebyscore("ZRANGEBYSCORE", Seq(ByteString(key), min.toByteString, max.toByteString), limit).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])

  def zrangebyscoreWithscores(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)(implicit convert: MultiBulkConverter[Seq[(ByteString, Double)]], ec: ExecutionContext): Future[Try[Seq[(ByteString, Double)]]] =
    zrangebyscore("ZRANGEBYSCORE", Seq(ByteString(key), min.toByteString, max.toByteString, ByteString("WITHSCORES")), limit).mapTo[MultiBulk].map(_.asTry[Seq[(ByteString, Double)]])

  private def zrank[A](command: String, key: String, member: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Option[Long]] =
    send(command, Seq(ByteString(key), convert.from(member))).map {
      case i: Integer => Some(i.toLong)
      case _ => None
    }

  def zrank[A](key: String, member: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Option[Long]] =
    zrank("ZRANK", key, member)

  def zrem[A](key: String, members: A*)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Long] =
    send("ZREM", ByteString(key) +: members.map(v => convert.from(v))).mapTo[Integer].map(_.toLong)

  def zremrangebyrank(key: String, start: Long, stop: Long)(implicit ec: ExecutionContext): Future[Long] =
    send("ZREMRANGEBYRANK", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString))).mapTo[Integer].map(_.toLong)

  def zremrangebyscore(key: String, min: Limit, max: Limit)(implicit ec: ExecutionContext): Future[Long] =
    send("ZREMRANGEBYSCORE", Seq(ByteString(key), min.toByteString, max.toByteString)).mapTo[Integer].map(_.toLong)

  def zrevrange(key: String, start: Long, stop: Long)(implicit convert: MultiBulkConverter[Seq[ByteString]], ec: ExecutionContext): Future[Try[Seq[ByteString]]] =
    send("ZREVRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString))).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])

  def zrevrangeWithscores(key: String, start: Long, stop: Long)(implicit convert: MultiBulkConverter[Seq[(ByteString, Double)]], ec: ExecutionContext): Future[Try[Seq[(ByteString, Double)]]] =
    send("ZREVRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString), ByteString("WITHSCORES"))).mapTo[MultiBulk].map(_.asTry[Seq[(ByteString, Double)]])

  def zrevrangebyscore(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)(implicit convert: MultiBulkConverter[Seq[ByteString]], ec: ExecutionContext): Future[Try[Seq[ByteString]]] =
    zrangebyscore("ZREVRANGEBYSCORE", Seq(ByteString(key), min.toByteString, max.toByteString), limit).mapTo[MultiBulk].map(_.asTry[Seq[ByteString]])

  def zrevrangebyscoreWithscores(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)(implicit convert: MultiBulkConverter[Seq[(ByteString, Double)]], ec: ExecutionContext): Future[Try[Seq[(ByteString, Double)]]] =
    zrangebyscore("ZREVRANGEBYSCORE", Seq(ByteString(key), min.toByteString, max.toByteString, ByteString("WITHSCORES")), limit).mapTo[MultiBulk].map(_.asTry[Seq[(ByteString, Double)]])

  def zrevrank[A](key: String, member: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Option[Long]] =
    zrank("ZREVRANK", key, member)

  def zscore[A](key: String, member: A)(implicit convert: RedisValueConverter[A], ec: ExecutionContext): Future[Option[Double]] =
    send("ZSCORE", Seq(ByteString(key), convert.from(member))).mapTo[Bulk].map(_.response.map(v => java.lang.Double.valueOf(v.utf8String)))

  def zunionstore(destination: String, key: String, keys: Seq[String], aggregate: Aggregate = SUM)(implicit convert: MultiBulkConverter[Seq[ByteString]], ec: ExecutionContext): Future[Long] =
    zStore("ZUNIONSTORE", destination, key, keys, aggregate)

  def zunionstoreWeighted(destination: String, keys: Seq[(String, Double)], aggregate: Aggregate = SUM)(implicit convert: MultiBulkConverter[Seq[ByteString]], ec: ExecutionContext): Future[Long] =
    zStoreWeighted("ZUNIONSTORE", destination, keys, aggregate)

}

trait Aggregate

case object SUM extends Aggregate

case object MIN extends Aggregate

case object MAX extends Aggregate

case class Limit(value: Double, inclusive: Boolean = true) {
  def toByteString: ByteString = ByteString(if (inclusive) value.toString else "(" + value.toString)
}


