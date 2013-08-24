package redis

import akka.util.ByteString
import redis.protocol._
import scala.util.Try
import scala.annotation.tailrec
import scala.collection.mutable

trait RedisValueConverter[A] {
  def from(a: A): ByteString
}

object RedisValueConverter {

  implicit object StringConverter extends RedisValueConverter[String] {
    def from(s: String): ByteString = ByteString(s)
  }

  implicit object ShortConverter extends RedisValueConverter[Short] {
    def from(i: Short): ByteString = ByteString(i.toString)
  }

  implicit object IntConverter extends RedisValueConverter[Int] {
    def from(i: Int): ByteString = ByteString(i.toString)
  }

  implicit object LongConverter extends RedisValueConverter[Long] {
    def from(i: Long): ByteString = ByteString(i.toString)
  }

  implicit object FloatConverter extends RedisValueConverter[Float] {
    def from(f: Float): ByteString = ByteString(f.toString)
  }

  implicit object DoubleConverter extends RedisValueConverter[Double] {
    def from(d: Double): ByteString = ByteString(d.toString)
  }

  implicit object CharConverter extends RedisValueConverter[Char] {
    def from(c: Char): ByteString = ByteString(c)
  }

  implicit object ByteConverter extends RedisValueConverter[Byte] {
    def from(b: Byte): ByteString = ByteString(b)
  }

  implicit object ArrayByteConverter extends RedisValueConverter[Array[Byte]] {
    def from(b: Array[Byte]): ByteString = ByteString(b)
  }

  implicit object ByteStringConverter extends RedisValueConverter[ByteString] {
    def from(bs: ByteString): ByteString = bs
  }

}

trait MultiBulkConverter[A] {
  def to(redisReply: MultiBulk): Try[A]
}

object MultiBulkConverter {

  def toSeqString(reply: MultiBulk): Seq[String] = {
    reply.responses.map(r => {
      r.map(_.toString)
    }).get
  }

  def toSeqByteString(reply: MultiBulk): Seq[ByteString] = {
    reply.responses.map(r => {
      r.map(_.toByteString)
    }).get
  }

  def toSeqOptionByteString(reply: MultiBulk): Seq[Option[ByteString]] = {
    reply.responses.map(r => {
      r.map(_.asOptByteString)
    }).get
  }

  def toSeqTuple2ByteStringDouble(reply: MultiBulk): Seq[(ByteString, Double)] = {
    reply.responses.map { r => {
      val s = r.map(_.toByteString)
      val builder = Seq.newBuilder[(ByteString, Double)]
      s.grouped(2).foreach { case Seq(a, b) => builder += ((a, b.utf8String.toDouble)) }
      builder.result()
    }}.get
  }

  def toSeqMapString(reply: MultiBulk): Seq[Map[String, String]] = {
    reply.responses.map { s =>
          s.map ({
            case m: MultiBulk => {
              m.responses.map { s =>
                val builder = Seq.newBuilder[(String, String)]
                s.grouped(2).foreach { case Seq(a, b) => builder += ((a.toString, b.toString)) }
                builder.result()
              }.getOrElse(Seq())
            }
            case _ => Seq()
          }).map { _.toMap }
      }.get
  }

  def toOptionStringByteString(reply: MultiBulk): Option[(String, ByteString)] = {
    reply.responses.map(r => {
      Some(r.head.toString, r.tail.head.toByteString)
    }).getOrElse(None)
  }

  def toSeqBoolean(reply: MultiBulk): Seq[Boolean] = {
    reply.responses.map(r => {
      r.map(_.toString == "1")
    }).get
  }

}
