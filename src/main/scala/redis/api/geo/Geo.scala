package redis.api.geo

import akka.util.ByteString
import redis._
import redis.api.geo.DistUnits.Measurement
import redis.api.geo.GeoOptions.WithOption
import redis.protocol._

case class GeoAdd[K](key: K, lat: Double, lng: Double, loc: String)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandIntegerLong  {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GEOADD", Seq(redisKey.serialize(key), ByteString(lng.toString), ByteString(lat.toString), ByteString(loc)))
}

case class GeoRadius[K](key: K, lat: Double, lng: Double, radius: Double, unit: Measurement)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandMultiBulk[Seq[String]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GEORADIUS", Seq(redisKey.serialize(key), ByteString(lng.toString), ByteString(lat.toString), ByteString(radius.toString), ByteString(unit.value)))

  def decodeReply(mb: MultiBulk): Seq[String] = MultiBulkConverter.toSeqString(mb)
}

case class GeoRadiusByMember[K](key: K, member:String, dist:Int, unit: Measurement)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandMultiBulk[Seq[String]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GEORADIUSBYMEMBER", Seq(redisKey.serialize(key), ByteString(member), ByteString(dist.toString), ByteString(unit.value)))

  def decodeReply(mb: MultiBulk): Seq[String] = MultiBulkConverter.toSeqString(mb)
}

case class GeoRadiusByMemberWithOpt[K](key: K, member:String, dist:Int, unit: Measurement, opt:WithOption, count: Int)
                                      (implicit redisKey: ByteStringSerializer[K]) extends RedisCommandMultiBulk[Seq[String]] {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GEORADIUSBYMEMBER", Seq(redisKey.serialize(key), ByteString(member),
    ByteString(dist.toString), ByteString(unit.value), ByteString(opt.value),ByteString("COUNT"), ByteString(count.toString)))

  def decodeReply(mb: MultiBulk): Seq[String] = decodeReplyx(mb)
//  def decodeReply(mb: MultiBulk): Seq[String] =
//    MultiBulkConverter.toSeqString(mb)
  def decodeReplyx(rd : RedisReply ): Seq[String] = rd match {
  case MultiBulk(Some(v)) =>
    v.flatMap(r =>
      decodeReplyx(r)
    )
  case Bulk(b) =>
     b.map(_.decodeString("US-ASCII")).toSeq

  case Error(r) =>
    println(r.decodeString("US-ASCII"))
    Nil

  case Integer(i) =>
    Seq(i.decodeString("US-ASCII"))

  case other => Nil

}


}



case class GeoDist[K](key: K, member1 :String, member2: String, unit: Measurement)(implicit redisKey: ByteStringSerializer[K]) extends RedisCommandBulkDouble {
  val isMasterOnly = false
  val encodedRequest: ByteString = encode("GEODIST", Seq(redisKey.serialize(key), ByteString(member1), ByteString(member2), ByteString(unit.value)))

  def decodeReply(mb: MultiBulk): Seq[String] = MultiBulkConverter.toSeqString(mb)
}

case class GeoHash[K](key: K, member: Seq[String] )(implicit redisKey: ByteStringSerializer[K])
  extends RedisCommandMultiBulk[Seq[String]]{
  val isMasterOnly = false
  val members: Seq[ByteString] = member.foldLeft(Seq.empty[ByteString]){ case (acc, e) =>
    ByteString(e.toString) +: acc
  }
  val keySec: Seq[ByteString] =  Seq(redisKey.serialize(key))
  val encodedRequest: ByteString = encode("GEOHASH", keySec ++ members )
  def decodeReply(mb: MultiBulk): Seq[String] = MultiBulkConverter.toSeqString(mb)
}

case class GeoPos[K](key: K, member: Seq[String] )(implicit redisKey: ByteStringSerializer[K])
  extends RedisCommandMultiBulk[Seq[String]]{
  val isMasterOnly = false
  val members: Seq[ByteString] = member.foldLeft(Seq.empty[ByteString]){ case (acc, e) =>
    ByteString(e.toString) +: acc
  }
  val keySec: Seq[ByteString] =  Seq(redisKey.serialize(key))
  val encodedRequest: ByteString = encode("GEOPOS", keySec ++ members )
  def decodeReply(mb: MultiBulk): Seq[String] = {
    MultiBulkConverter.toSeqString(mb)
  }

}





