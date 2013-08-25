package redis.commands

import redis.{ByteStringSerializer, Request}
import akka.util.ByteString
import scala.concurrent.Future
import redis.api._
import redis.api.sortedsets._

trait SortedSets extends Request {

  def zadd[K: ByteStringSerializer, V: ByteStringSerializer](key: K, scoreMembers: (Double, V)*): Future[Long] =
    send(Zadd(key, scoreMembers))

  def zcard[K: ByteStringSerializer](key: K): Future[Long] =
    send(Zcard(key))

  def zcount[K: ByteStringSerializer](key: K, min: Limit = Limit(Double.NegativeInfinity), max: Limit = Limit(Double.PositiveInfinity)): Future[Long] =
    send(Zcount(key, min, max))

  def zincrby[K: ByteStringSerializer, V: ByteStringSerializer](key: K, increment: Double, member: V): Future[Double] =
    send(Zincrby(key, increment, member))

  def zinterstore[KD: ByteStringSerializer, K: ByteStringSerializer, KK: ByteStringSerializer]
  (destination: KD, key: K, keys: Seq[KK], aggregate: Aggregate = SUM): Future[Long] =
    send(Zinterstore(destination, key, keys, aggregate))

  def zinterstoreWeighted[KD: ByteStringSerializer, K: ByteStringSerializer](destination: KD, keys: Map[K, Double], aggregate: Aggregate = SUM): Future[Long] =
    send(ZinterstoreWeighted(destination, keys, aggregate))

  def zrange[K: ByteStringSerializer](key: K, start: Long, stop: Long): Future[Seq[ByteString]] =
    send(Zrange(key, start, stop))

  def zrangeWithscores[K: ByteStringSerializer](key: K, start: Long, stop: Long): Future[Seq[(ByteString, Double)]] =
    send(ZrangeWithscores(key, start, stop))

  def zrangebyscore[K: ByteStringSerializer](key: K, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[ByteString]] =
    send(Zrangebyscore(key, min, max, limit))

  def zrangebyscoreWithscores[K: ByteStringSerializer](key: K, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[(ByteString, Double)]] =
    send(ZrangebyscoreWithscores(key, min, max, limit))

  def zrank[K: ByteStringSerializer, V: ByteStringSerializer](key: K, member: V): Future[Option[Long]] =
    send(Zrank(key, member))

  def zrem[K: ByteStringSerializer, V: ByteStringSerializer](key: K, members: V*): Future[Long] =
    send(Zrem(key, members))

  def zremrangebyrank[K: ByteStringSerializer](key: K, start: Long, stop: Long): Future[Long] =
    send(Zremrangebyrank(key, start, stop))

  def zremrangebyscore[K: ByteStringSerializer](key: K, min: Limit, max: Limit): Future[Long] =
    send(Zremrangebyscore(key, min, max))

  def zrevrange[K: ByteStringSerializer](key: K, start: Long, stop: Long): Future[Seq[ByteString]] =
    send(Zrevrange(key, start, stop))

  def zrevrangeWithscores[K: ByteStringSerializer](key: K, start: Long, stop: Long): Future[Seq[(ByteString, Double)]] =
    send(ZrevrangeWithscores(key, start, stop))

  def zrevrangebyscore[K: ByteStringSerializer](key: K, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[ByteString]] =
    send(Zrevrangebyscore(key, min, max, limit))

  def zrevrangebyscoreWithscores[K: ByteStringSerializer](key: K, min: Limit, max: Limit, limit: Option[(Long, Long)] = None): Future[Seq[(ByteString, Double)]] =
    send(ZrevrangebyscoreWithscores(key, min, max, limit))

  def zrevrank[K: ByteStringSerializer, V: ByteStringSerializer](key: K, member: V): Future[Option[Long]] =
    send(Zrevrank(key, member))

  def zscore[K: ByteStringSerializer, V: ByteStringSerializer](key: K, member: V): Future[Option[Double]] =
    send(Zscore(key, member))

  def zunionstore[KD: ByteStringSerializer, K: ByteStringSerializer, KK: ByteStringSerializer]
  (destination: KD, key: K, keys: Seq[KK], aggregate: Aggregate = SUM): Future[Long] =
    send(Zunionstore(destination, key, keys, aggregate))

  def zunionstoreWeighted[KD: ByteStringSerializer, K: ByteStringSerializer]
  (destination: KD, keys: Map[K, Double], aggregate: Aggregate = SUM): Future[Long] =
    send(ZunionstoreWeighted(destination, keys, aggregate))

}

