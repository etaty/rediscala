package redis.api.geo

/**
  * Created by avilevi on 08/12/2016.
  */
object GeoOptions{
  sealed trait WithOption{
    def value:String = {
      this match {
        case WithDist => "WITHDIST"
        case WithCoord => "WITHCOORD"
        case WithHash => "WITHHASH"
      }
    }
  }
  case object WithDist extends WithOption
  case object WithCoord extends WithOption
  case object WithHash extends WithOption

}
