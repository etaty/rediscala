package redis.api

import akka.util.ByteString


trait Aggregate

case object SUM extends Aggregate

case object MIN extends Aggregate

case object MAX extends Aggregate

case class Limit(value: Double, inclusive: Boolean = true) {
  def toByteString: ByteString = ByteString(if (inclusive) value.toString else "(" + value.toString)
}

trait Order

case object ASC extends Order

case object DESC extends Order

case class LimitOffsetCount(offset: Long, count: Long) {
  def toByteString: Seq[ByteString] = Seq(ByteString("LIMIT"), ByteString(offset.toString), ByteString(count.toString))
}


sealed trait BitOperator

case object AND extends BitOperator

case object OR extends BitOperator

case object XOR extends BitOperator

case object NOT extends BitOperator


sealed trait ListPivot

case object AFTER extends ListPivot

case object BEFORE extends ListPivot


sealed trait ShutdownModifier

case object SAVE extends ShutdownModifier

case object NOSAVE extends ShutdownModifier


sealed trait ZaddOption {
  def serialize: ByteString
}

object ZaddOption {

  case object XX extends ZaddOption {
    override def serialize: ByteString = ByteString("XX")
  }

  case object NX extends ZaddOption {
    override def serialize: ByteString = ByteString("NX")
  }

  case object CH extends ZaddOption {
    override def serialize: ByteString = ByteString("CH")
  }

  case object INCR extends ZaddOption {
    override def serialize: ByteString = ByteString("INCR")
  }

}

sealed trait RequestStreamId {
  def serialize: ByteString
}

case class StreamId(time: Long, sequence: Long = 0) extends RequestStreamId {
  override def serialize: ByteString = ByteString(toString)
  override def toString: String = s"${time}-${sequence}"
  def next: StreamId = StreamId(time, sequence + 1)
}

protected[redis] class MagicStreamId(s: String) extends RequestStreamId {
  val serialize: ByteString = ByteString(s)
}

object StreamId {
  final val MIN = new MagicStreamId("-")
  final val MAX = new MagicStreamId("+")
  final val AUTOGENERATE = new MagicStreamId("*")
  final val ADDED = new MagicStreamId("$")
  final val UNDELIVERED = new MagicStreamId(">")
  final val MIN_VALID = StreamId(0, 1)

  def deserialize(s: ByteString): StreamId = {
    val f = s.utf8String.split('-')
    StreamId(f(0).toLong, f(1).toLong)
  }
}

case class StreamEntry[F, V](id: StreamId, fields: Seq[(F, V)])

sealed trait TrimStrategy {
  def toByteString: Seq[ByteString]
}

case class MaxLength(count: Long, approximate: Boolean = false) extends TrimStrategy {
  def toByteString: Seq[ByteString] = {
    val builder = Seq.newBuilder[ByteString]
    builder += ByteString("MAXLEN")
    if (approximate)
      builder += ByteString("~")
    builder += ByteString(count.toString)
    builder.result()
  }
}