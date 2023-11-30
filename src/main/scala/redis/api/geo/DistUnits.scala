package redis.api.geo

object DistUnits{
  sealed trait Measurement{
    def value:String = {
      this match {git add 
        case Meter => "m"
        case Kilometer => "km"
        case Mile => "mi"
        case Feet => "ft"
      }
    }
  }
  case object Meter extends Measurement
  case object Kilometer extends Measurement
  case object Mile extends Measurement
  case object Feet extends Measurement
}
