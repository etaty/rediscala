package redis.commands

import redis.protocol.MultiBulk
import redis.{Cursor, ByteStringDeserializer, ByteStringSerializer, Request}
import scala.concurrent.Future
import redis.api.geo._

trait Geo extends Request {

	def geoadd[K](key: String, lat: Double, lng: Double, loc: String): Future[Boolean] =
		send(GeoAdd(key, lat, lng, loc))

	def georadius[K](key: String, lat: Double, lng: Double, radius: Double, dim: String): Future[Seq[String]] =
		send(GeoRadius(key, lat, lng, radius, dim))
}
