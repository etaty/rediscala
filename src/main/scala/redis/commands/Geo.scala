package redis.commands

import redis.Request
import redis.api.geo.DistUnits.{Kilometer, Measurement, Meter}
import redis.api.geo.GeoOptions.{WithDist, WithOption}
import redis.api.geo._

import scala.concurrent.Future

trait Geo extends Request {

  def geoAdd[K](key: String, lat: Double, lng: Double, loc: String) =
    send(GeoAdd(key, lat, lng, loc))

  def geoRadius[K](key: String, lat: Double, lng: Double, radius: Double, dim: Measurement = Kilometer): Future[Seq[String]] =
    send(GeoRadius(key, lat, lng, radius, dim))

  def geoRadiusByMember[K](key: String, member: String, dist:Int, dim: Measurement = Meter): Future[Seq[String]] =
    send(GeoRadiusByMember(key, member, dist, dim))

  def geoRadiusByMemberWithOpt[K](key: String, member: String, dist:Int, dim: Measurement = Meter, opt: WithOption = WithDist, count: Int = Int.MaxValue): Future[Seq[String]] =
    send(GeoRadiusByMemberWithOpt(key, member, dist, dim, opt,count))

  def geoDist[K] (key: String ,member1: String, member2: String, unit: Measurement = Meter ) =
    send(GeoDist(key, member1, member2, unit))

  def geoHash[K] (key: String ,members: String *) = send(GeoHash(key, members))

  def geoPos[K] (key: String ,members: String *) = send(GeoPos(key, members))

}
