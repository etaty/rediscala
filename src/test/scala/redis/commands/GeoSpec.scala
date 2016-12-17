package redis.commands

import redis._
import redis.api.geo.DistUnits._
import scala.concurrent.Await

class GeoSpec extends RedisStandaloneServer  {
  import concurrent.duration._

  val testKey = "Sicily"

  def addPlaces() = {
    Await.result(redis.geoAdd(testKey, 13.361389, 38.115556, "Palermo"), 2 second)
    Await.result(redis.geoAdd(testKey, 15.087269, 37.502669, "Catania"), 2 second)
    Await.result(redis.geoAdd(testKey, 13.583333, 37.316667, "Agrigento"), 2 second)

  }

  "Geo commands " should {

    "GEOADD add key member" in {
      val res =  Await.result(redis.geoAdd(testKey, 23.361389, 48.115556, "SomePlace"), 2 second)
      res shouldEqual 1
    }

    "GEORADIUS By Member" in {
      addPlaces()
      val res = Await.result(redis.geoRadius(testKey,15 , 37, 200, Kilometer), 2 second)
      res shouldEqual Vector("Agrigento","Catania")
    }

    "GEORADIUS By Member" in {
      addPlaces()
      val res = Await.result(redis.geoRadiusByMember(testKey, "Catania", 500, Kilometer), 2 second)
      res shouldEqual Vector("Agrigento" ,"Palermo","Catania")
    }

    "GEORADIUS By Member with opt" in {
      addPlaces()
      val res = Await.result(redis.geoRadiusByMemberWithOpt(testKey, "Agrigento", 100, Kilometer), 2 second)
      res shouldEqual Vector("Agrigento", "0.0000", "Palermo", "89.8694")
    }

    "GEODIST with default unit" in {
      addPlaces()
      val res = Await.result(redis.geoDist(testKey, "Palermo", "Catania"), 2 seconds)
      res shouldEqual 203017.1901
    }

    "GEODIST in km" in {
      addPlaces()
      val res = Await.result(redis.geoDist(testKey, "Palermo", "Catania", Kilometer), 2 seconds)
      res shouldEqual 203.0172
    }

    "GEODIST in mile" in {
      addPlaces()
      val res = Await.result(redis.geoDist(testKey, "Palermo", "Catania", Mile), 2 seconds)
      res shouldEqual 126.1493
    }

    "GEODIST in feet" in {
      addPlaces()
      val res = Await.result(redis.geoDist(testKey, "Palermo", "Catania", Feet), 2 seconds)
      res shouldEqual 666066.8965
    }

    "GEOHASH " in {
      addPlaces()
      val res = Await.result(redis.geoHash(testKey, "Palermo", "Catania"), 2 seconds)
      res shouldEqual Vector("sfdtv6s9ew0", "sf7h526gsz0")
    }

    "GEOPOS " in {
      addPlaces()
      val res = Await.result(redis.geoPos(testKey, "Palermo", "Catania"), 2 seconds)
      res shouldNotEqual Nil
    }
  }
}
