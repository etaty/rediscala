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
    @tailrec
    def recur(s: Seq[ByteString], builder: mutable.Builder[(ByteString, Double), Seq[(ByteString, Double)]]): Unit = {
      if (s.nonEmpty) {
        val double = java.lang.Double.parseDouble(s.tail.head.utf8String)
        builder += ((s.head, double))
        recur(s.tail.tail, builder)
      }
    }

    reply.responses.map(r => {
      val seq = r.map(_.toByteString)
      val builder = Seq.newBuilder[(ByteString, Double)]
      recur(seq, builder)
      builder.result()
    }).get
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

trait ByteStringDeserializer[T] {
  def deserialize(bs: ByteString): T
}


object ByteStringDeserializer extends LowPriorityDefaultByteStringDeserializerImplicits

trait LowPriorityDefaultByteStringDeserializerImplicits {

  implicit object ByteString extends ByteStringDeserializer[ByteString] {
    def deserialize(bs: ByteString): ByteString = bs
  }
/*
  implicit object String extends ByteStringDeserializer[String] {
    def deserialize(bs: ByteString): String = bs.utf8String
  }
*/
}