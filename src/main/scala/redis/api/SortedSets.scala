package redis.api.sortedsets

import redis._
import akka.util.ByteString
import redis.api.{SUM, Aggregate, Limit}
import redis.protocol.Integer

case class Zadd[A](key: String, scoreMembers: Seq[(Double, A)])(implicit convert: RedisValueConverter[A])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZADD", ByteString(key) +: scoreMembers.foldLeft(Seq.empty[ByteString])({
    case (acc, e) => ByteString(e._1.toString) +: convert.from(e._2) +: acc
  }))
}

case class Zcard(key: String) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZCARD", Seq(ByteString(key)))
}

case class Zcount(key: String, min: Limit = Limit(Double.NegativeInfinity), max: Limit = Limit(Double.PositiveInfinity))
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZCOUNT", Seq(ByteString(key), min.toByteString, max.toByteString))
}

case class Zincrby[A](key: String, increment: Double, member: A)(implicit convert: RedisValueConverter[A])
  extends RedisCommandBulkDouble {
  val encodedRequest: ByteString = encode("ZINCRBY", Seq(ByteString(key), ByteString(increment.toString), convert.from(member)))
}

private[redis] object Zstore {
  def buildArgs(destination: String, key: String, keys: Seq[String], aggregate: Aggregate = SUM): Seq[ByteString] = {
    (ByteString(destination)
      +: ByteString((1 + keys.size).toString)
      +: ByteString(key)
      +: keys.map(ByteString.apply)) ++ Seq(ByteString("AGGREGATE"), ByteString(aggregate.toString))
  }
}

case class Zinterstore(destination: String, key: String, keys: Seq[String], aggregate: Aggregate = SUM)
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZINTERSTORE", Zstore.buildArgs(destination, key, keys, aggregate))
}

private[redis] object ZstoreWeighted {
  def buildArgs(destination: String, keys: Seq[(String, Double)], aggregate: Aggregate = SUM): Seq[ByteString] = {
    (ByteString(destination) +: ByteString(keys.size.toString) +: keys.map {
      k => ByteString(k._1)
    }) ++ (ByteString("WEIGHTS") +: keys.map {
      k => ByteString(k._2.toString)
    }) ++ Seq(ByteString("AGGREGATE"), ByteString(aggregate.toString))
  }
}

case class ZinterstoreWeighted(destination: String, keys: Seq[(String, Double)], aggregate: Aggregate = SUM)
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZINTERSTORE", ZstoreWeighted.buildArgs(destination, keys, aggregate))
}

case class Zrange(key: String, start: Long, stop: Long) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("ZRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString)))
}

case class ZrangeWithscores(key: String, start: Long, stop: Long) extends RedisCommandMultiBulkSeqByteStringDouble {
  val encodedRequest: ByteString = encode("ZRANGE",
    Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString), ByteString("WITHSCORES")))
}

private[redis] object Zrangebyscore {
  def buildArgs(key: String, min: Limit, max: Limit, withscores: Boolean, limit: Option[(Long, Long)]): Seq[ByteString] = {
    val builder = Seq.newBuilder[ByteString]
    builder ++= Seq(ByteString(key), min.toByteString, max.toByteString)
    if (withscores) {
      builder += ByteString("WITHSCORES")
    }
    limit.foreach(l => {
      builder ++= Seq(ByteString("LIMIT"), ByteString(l._1.toString), ByteString(l._2.toString))
    })
    builder.result()
  }
}

case class Zrangebyscore(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)
  extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("ZRANGEBYSCORE", Zrangebyscore.buildArgs(key, min, max, withscores = false, limit))
}

case class ZrangebyscoreWithscores(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)
  extends RedisCommandMultiBulkSeqByteStringDouble {
  val encodedRequest: ByteString = encode("ZRANGEBYSCORE", Zrangebyscore.buildArgs(key, min, max, withscores = true, limit))
}

case class Zrank[A](key: String, member: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandRedisReplyOptionLong {
  val encodedRequest: ByteString = encode("ZRANK", Seq(ByteString(key), convert.from(member)))
}

case class Zrem[A](key: String, members: Seq[A])(implicit convert: RedisValueConverter[A]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZREM", ByteString(key) +: members.map(v => convert.from(v)))
}

case class Zremrangebyrank(key: String, start: Long, stop: Long) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZREMRANGEBYRANK", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString)))
}

case class Zremrangebyscore(key: String, min: Limit, max: Limit) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZREMRANGEBYSCORE", Seq(ByteString(key), min.toByteString, max.toByteString))
}

case class Zrevrange(key: String, start: Long, stop: Long) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("ZREVRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString)))
}

case class ZrevrangeWithscores(key: String, start: Long, stop: Long) extends RedisCommandMultiBulkSeqByteStringDouble {
  val encodedRequest: ByteString = encode("ZREVRANGE", Seq(ByteString(key), ByteString(start.toString), ByteString(stop.toString), ByteString("WITHSCORES")))
}

case class Zrevrangebyscore(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)
  extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("ZREVRANGEBYSCORE", Zrangebyscore.buildArgs(key, min, max, withscores = false, limit))
}

case class ZrevrangebyscoreWithscores(key: String, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)
  extends RedisCommandMultiBulkSeqByteStringDouble {
  val encodedRequest: ByteString = encode("ZREVRANGEBYSCORE", Zrangebyscore.buildArgs(key, min, max, withscores = true, limit))
}

case class Zrevrank[A](key: String, member: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandRedisReplyOptionLong {
  val encodedRequest: ByteString = encode("ZREVRANK", Seq(ByteString(key), convert.from(member)))
}

case class Zscore[A](key: String, member: A)(implicit convert: RedisValueConverter[A]) extends RedisCommandBulkOptionDouble {
  val encodedRequest: ByteString = encode("ZSCORE", Seq(ByteString(key), convert.from(member)))
}

case class Zunionstore(destination: String, key: String, keys: Seq[String], aggregate: Aggregate = SUM)
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZUNIONSTORE", Zstore.buildArgs(destination, key, keys, aggregate))
}

case class ZunionstoreWeighted(destination: String, keys: Seq[(String, Double)], aggregate: Aggregate = SUM)
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZUNIONSTORE", ZstoreWeighted.buildArgs(destination, keys, aggregate))
}
