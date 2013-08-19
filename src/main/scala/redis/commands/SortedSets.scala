package redis.commands

import redis.{RedisValueConverter, Request}
import akka.util.ByteString
import scala.concurrent.Future
import redis.api._
import redis.api.sortedsets._

trait SortedSets extends Request {

  def zadd[A](key: String, scoreMembers: (Double, A)*)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Zadd(key, scoreMembers))

  def zcard(key: String): Future[Long] =
    send(Zcard(key))

  def zcount(key: String, min: Limit = Limit(Double.NegativeInfinity), max: Limit = Limit(Double.PositiveInfinity)): Future[Long] =
    send(Zcount(key, min, max))

  def zincrby[A](key: String, increment: Double, member: A)(implicit convert: RedisValueConverter[A]): Future[Double] =
    send(Zincrby(key, increment, member))

  def zinterstore(destination: String, key: String, keys: Seq[String], aggregate: Aggregate = SUM): Future[Long] =
    send(Zinterstore(destination, key, keys, aggregate))

  def zinterstoreWeighted(destination: String, keys: Seq[(String, Double)], aggregate: Aggregate = SUM): Future[Long] =
    send(ZinterstoreWeighted(destination, keys, aggregate))

  def zrange(key: String, start: Long, stop: Long): Future[Seq[ByteString]] =
    send(Zrange(key, start, stop))

  def zrangeWithscores(key: String, start: Long, stop: Long): Future[Seq[(ByteString, Double)]] =
    send(ZrangeWithscores(key, start, stop))

  def zrangebyscore(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[ByteString]] =
    send(Zrangebyscore(key, min, max, limit))

  def zrangebyscoreWithscores(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[(ByteString, Double)]] =
    send(ZrangebyscoreWithscores(key, min, max, limit))

  def zrank[A](key: String, member: A)(implicit convert: RedisValueConverter[A]): Future[Option[Long]] =
    send(Zrank(key, member))

  def zrem[A](key: String, members: A*)(implicit convert: RedisValueConverter[A]): Future[Long] =
    send(Zrem(key, members))

  def zremrangebyrank(key: String, start: Long, stop: Long): Future[Long] =
    send(Zremrangebyrank(key, start, stop))

  def zremrangebyscore(key: String, min: Limit, max: Limit): Future[Long] =
    send(Zremrangebyscore(key, min, max))

  def zrevrange(key: String, start: Long, stop: Long): Future[Seq[ByteString]] =
    send(Zrevrange(key, start, stop))

  def zrevrangeWithscores(key: String, start: Long, stop: Long): Future[Seq[(ByteString, Double)]] =
    send(ZrevrangeWithscores(key, start, stop))

  def zrevrangebyscore(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[ByteString]] =
    send(Zrevrangebyscore(key, min, max, limit))

  def zrevrangebyscoreWithscores(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[(ByteString, Double)]] =
    send(ZrevrangebyscoreWithscores(key, min, max, limit))

  def zrevrank[A](key: String, member: A)(implicit convert: RedisValueConverter[A]): Future[Option[Long]] =
    send(Zrevrank(key, member))

  def zscore[A](key: String, member: A)(implicit convert: RedisValueConverter[A]): Future[Option[Double]] =
    send(Zscore(key, member))

  def zunionstore(destination: String, key: String, keys: Seq[String], aggregate: Aggregate = SUM): Future[Long] =
    send(Zunionstore(destination, key, keys, aggregate))

  def zunionstoreWeighted(destination: String, keys: Seq[(String, Double)], aggregate: Aggregate = SUM): Future[Long] =
    send(ZunionstoreWeighted(destination, keys, aggregate))

}

