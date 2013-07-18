package redis.api

import akka.util.ByteString


trait Aggregate

case object SUM extends Aggregate

case object MIN extends Aggregate

case object MAX extends Aggregate

case class Limit(value: Double, inclusive: Boolean = true) {
  def toByteString: ByteString = ByteString(if (inclusive) value.toString else "(" + value.toString)
}