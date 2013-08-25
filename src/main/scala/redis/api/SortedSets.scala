package redis.api.sortedsets

import redis._
import akka.util.ByteString
import redis.api.{SUM, Aggregate, Limit}

case class Zadd[K, V](key: K, scoreMembers: Seq[(Double, V)])(implicit keySeria: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZADD", keySeria.serialize(key) +: scoreMembers.foldLeft(Seq.empty[ByteString])({
    case (acc, e) => ByteString(e._1.toString) +: convert.serialize(e._2) +: acc
  }))
}

case class Zcard[K](key: K)(implicit keySeria: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZCARD", Seq(keySeria.serialize(key)))
}

case class Zcount[K](key: K, min: Limit = Limit(Double.NegativeInfinity), max: Limit = Limit(Double.PositiveInfinity))
                    (implicit keySeria: ByteStringSerializer[K])
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZCOUNT", Seq(keySeria.serialize(key), min.toByteString, max.toByteString))
}

case class Zincrby[K, V](key: K, increment: Double, member: V)(implicit keySeria: ByteStringSerializer[K], convert: ByteStringSerializer[V])
  extends RedisCommandBulkDouble {
  val encodedRequest: ByteString = encode("ZINCRBY", Seq(keySeria.serialize(key), ByteString(increment.toString), convert.serialize(member)))
}

private[redis] object Zstore {
  def buildArgs[KD, K, KK](destination: KD, key: K, keys: Seq[KK], aggregate: Aggregate = SUM)
                          (implicit keyDestSeria: ByteStringSerializer[KD], keySeria: ByteStringSerializer[K], keysSeria: ByteStringSerializer[KK]): Seq[ByteString] = {
    (keyDestSeria.serialize(destination)
      +: ByteString((1 + keys.size).toString)
      +: keySeria.serialize(key)
      +: keys.map(keysSeria.serialize)) ++ Seq(ByteString("AGGREGATE"), ByteString(aggregate.toString))
  }
}

case class Zinterstore[KD: ByteStringSerializer, K: ByteStringSerializer, KK: ByteStringSerializer](destination: KD, key: K, keys: Seq[KK], aggregate: Aggregate = SUM)
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZINTERSTORE", Zstore.buildArgs(destination, key, keys, aggregate))
}

private[redis] object ZstoreWeighted {
  def buildArgs[KD, K](destination: KD, keys: Map[K, Double], aggregate: Aggregate = SUM)
                      (implicit keyDestSeria: ByteStringSerializer[KD], keySeria: ByteStringSerializer[K]): Seq[ByteString] = {
    (keyDestSeria.serialize(destination) +: ByteString(keys.size.toString) +: keys.keys.map(keySeria.serialize).toSeq
      ) ++ (ByteString("WEIGHTS") +: keys.values.map(v => ByteString(v.toString)).toSeq
      ) ++ Seq(ByteString("AGGREGATE"), ByteString(aggregate.toString))
  }
}

case class ZinterstoreWeighted[KD: ByteStringSerializer, K: ByteStringSerializer](destination: KD, keys: Map[K, Double], aggregate: Aggregate = SUM)
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZINTERSTORE", ZstoreWeighted.buildArgs(destination, keys, aggregate))
}

case class Zrange[K](key: K, start: Long, stop: Long)(implicit keySeria: ByteStringSerializer[K]) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("ZRANGE", Seq(keySeria.serialize(key), ByteString(start.toString), ByteString(stop.toString)))
}

case class ZrangeWithscores[K](key: K, start: Long, stop: Long)(implicit keySeria: ByteStringSerializer[K]) extends RedisCommandMultiBulkSeqByteStringDouble {
  val encodedRequest: ByteString = encode("ZRANGE",
    Seq(keySeria.serialize(key), ByteString(start.toString), ByteString(stop.toString), ByteString("WITHSCORES")))
}

private[redis] object Zrangebyscore {
  def buildArgs[K](key: K, min: Limit, max: Limit, withscores: Boolean, limit: Option[(Long, Long)])
                  (implicit keySeria: ByteStringSerializer[K]): Seq[ByteString] = {
    val builder = Seq.newBuilder[ByteString]
    builder ++= Seq(keySeria.serialize(key), min.toByteString, max.toByteString)
    if (withscores) {
      builder += ByteString("WITHSCORES")
    }
    limit.foreach(l => {
      builder ++= Seq(ByteString("LIMIT"), ByteString(l._1.toString), ByteString(l._2.toString))
    })
    builder.result()
  }
}

case class Zrangebyscore[K: ByteStringSerializer](key: K, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)
  extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("ZRANGEBYSCORE", Zrangebyscore.buildArgs(key, min, max, withscores = false, limit))
}

case class ZrangebyscoreWithscores[K: ByteStringSerializer](key: K, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)
  extends RedisCommandMultiBulkSeqByteStringDouble {
  val encodedRequest: ByteString = encode("ZRANGEBYSCORE", Zrangebyscore.buildArgs(key, min, max, withscores = true, limit))
}

case class Zrank[K, V](key: K, member: V)(implicit keySeria: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandRedisReplyOptionLong {
  val encodedRequest: ByteString = encode("ZRANK", Seq(keySeria.serialize(key), convert.serialize(member)))
}

case class Zrem[K, V](key: K, members: Seq[V])(implicit keySeria: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZREM", keySeria.serialize(key) +: members.map(v => convert.serialize(v)))
}

case class Zremrangebyrank[K](key: K, start: Long, stop: Long)(implicit keySeria: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZREMRANGEBYRANK", Seq(keySeria.serialize(key), ByteString(start.toString), ByteString(stop.toString)))
}

case class Zremrangebyscore[K](key: K, min: Limit, max: Limit)(implicit keySeria: ByteStringSerializer[K]) extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZREMRANGEBYSCORE", Seq(keySeria.serialize(key), min.toByteString, max.toByteString))
}

case class Zrevrange[K](key: K, start: Long, stop: Long)(implicit keySeria: ByteStringSerializer[K]) extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("ZREVRANGE", Seq(keySeria.serialize(key), ByteString(start.toString), ByteString(stop.toString)))
}

case class ZrevrangeWithscores[K](key: K, start: Long, stop: Long)(implicit keySeria: ByteStringSerializer[K]) extends RedisCommandMultiBulkSeqByteStringDouble {
  val encodedRequest: ByteString = encode("ZREVRANGE", Seq(keySeria.serialize(key), ByteString(start.toString), ByteString(stop.toString), ByteString("WITHSCORES")))
}

case class Zrevrangebyscore[K: ByteStringSerializer](key: K, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)
  extends RedisCommandMultiBulkSeqByteString {
  val encodedRequest: ByteString = encode("ZREVRANGEBYSCORE", Zrangebyscore.buildArgs(key, min, max, withscores = false, limit))
}

case class ZrevrangebyscoreWithscores[K: ByteStringSerializer](key: K, min: Limit, max: Limit, limit: Option[(Long, Long)] = None)
  extends RedisCommandMultiBulkSeqByteStringDouble {
  val encodedRequest: ByteString = encode("ZREVRANGEBYSCORE", Zrangebyscore.buildArgs(key, min, max, withscores = true, limit))
}

case class Zrevrank[K, V](key: K, member: V)(implicit keySeria: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandRedisReplyOptionLong {
  val encodedRequest: ByteString = encode("ZREVRANK", Seq(keySeria.serialize(key), convert.serialize(member)))
}

case class Zscore[K, V](key: K, member: V)(implicit keySeria: ByteStringSerializer[K], convert: ByteStringSerializer[V]) extends RedisCommandBulkOptionDouble {
  val encodedRequest: ByteString = encode("ZSCORE", Seq(keySeria.serialize(key), convert.serialize(member)))
}

case class Zunionstore[KD: ByteStringSerializer, K: ByteStringSerializer, KK: ByteStringSerializer]
(destination: KD, key: K, keys: Seq[KK], aggregate: Aggregate = SUM)
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZUNIONSTORE", Zstore.buildArgs(destination, key, keys, aggregate))
}

case class ZunionstoreWeighted[KD: ByteStringSerializer, K: ByteStringSerializer](destination: KD, keys: Map[K, Double], aggregate: Aggregate = SUM)
  extends RedisCommandIntegerLong {
  val encodedRequest: ByteString = encode("ZUNIONSTORE", ZstoreWeighted.buildArgs(destination, keys, aggregate))
}
