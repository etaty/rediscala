package redis.protocol

import akka.util.ByteString
import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.Try
import redis.MultiBulkConverter

sealed trait RedisReply {
  def toByteString: ByteString

  def asOptByteString: Option[ByteString]
}

case class Status(status: ByteString) extends RedisReply {
  def toBoolean: Boolean = status == Status.okByteString

  override def toString = status.utf8String

  def toByteString: ByteString = status

  def asOptByteString: Option[ByteString] = Some(status)
}

object Status {
  val okByteString = ByteString("OK")
}

case class Error(error: ByteString) extends RedisReply {
  override def toString = error.utf8String

  def toByteString: ByteString = error

  def asOptByteString: Option[ByteString] = Some(error)
}

case class Integer(i: ByteString) extends RedisReply {
  def toLong: Long = ParseNumber.parseLong(i)

  def toInt: Int = ParseNumber.parseInt(i)

  def toBoolean = i == Integer.trueByteString

  override def toString = i.utf8String

  def toByteString: ByteString = i

  def asOptByteString: Option[ByteString] = Some(i)
}

object Integer {
  val trueByteString = ByteString("1")
}

case class Bulk(response: Option[ByteString]) extends RedisReply {
  // looks wrong
  override def toString = response.map(_.utf8String).get

  def toByteString: ByteString = response.get

  def toOptString: Option[String] = response.map(_.utf8String)

  def asOptByteString: Option[ByteString] = response
}

case class MultiBulk(responses: Option[Vector[RedisReply]]) extends RedisReply {
  def toByteString: ByteString = throw new NoSuchElementException()

  def asOptByteString: Option[ByteString] = throw new NoSuchElementException()

  def asTry[A](implicit convert: MultiBulkConverter[A]): Try[A] = convert.to(this)

  def asOpt[A](implicit convert: MultiBulkConverter[A]): Option[A] = asTry(convert).toOption
}

case class PartialMultiBulk(i: Int, acc: mutable.Buffer[RedisReply]) extends RedisReply {
  override def toByteString: ByteString = throw new NoSuchElementException()

  override def asOptByteString: Option[ByteString] = throw new NoSuchElementException()
}

sealed trait DecodeResult[+A] {
  def rest: ByteString

  def isFullyDecoded: Boolean

  def foreach[B](f: A => Unit): DecodeResult[Unit] = this match {
    case p: PartiallyDecoded[A] => PartiallyDecoded(ByteString(), bs => p.f(p.rest ++ bs).foreach(f))
    case fd: FullyDecoded[A] => FullyDecoded(f(fd.result), fd.rest)
  }

  def map[B](f: A => B): DecodeResult[B] = this match {
    case p: PartiallyDecoded[A] => PartiallyDecoded(ByteString(), bs => p.f(p.rest ++ bs).map(f))
    case fd: FullyDecoded[A] => FullyDecoded(f(fd.result), fd.rest)
  }

  def flatMap[B](f: (A, ByteString) => DecodeResult[B]): DecodeResult[B] = this match {
    case p: PartiallyDecoded[A] => PartiallyDecoded(ByteString(), bs => p.f(p.rest ++ bs).flatMap(f))
    case fd: FullyDecoded[A] => f(fd.result, fd.rest)
  }

  def run(next: ByteString): DecodeResult[A] = this match {
    case p: PartiallyDecoded[A] => p.f(p.rest ++ next)
    case fd: FullyDecoded[A] => FullyDecoded(fd.result, fd.rest ++ next)
  }
}

case class PartiallyDecoded[A](rest: ByteString, f: ByteString => DecodeResult[A]) extends DecodeResult[A] {
  override def isFullyDecoded: Boolean = false
}

case class FullyDecoded[A](result: A, rest: ByteString) extends DecodeResult[A] {
  override def isFullyDecoded: Boolean = true
}

object DecodeResult {
  val unit: DecodeResult[Unit] = FullyDecoded((), ByteString.empty)
}


object RedisProtocolReply {
  val ERROR = '-'
  val STATUS = '+'
  val INTEGER = ':'
  val BULK = '$'
  val MULTIBULK = '*'

  val LS = "\r\n".getBytes("UTF-8")

  def decodeReply(bs: ByteString): DecodeResult[RedisReply] = {
    if (bs.isEmpty) {
      PartiallyDecoded(bs, decodeReply)
    } else {
      bs.head match {
        case ERROR => decodeString(bs.tail).map(Error(_))
        case INTEGER => decodeInteger(bs.tail)
        case STATUS => decodeString(bs.tail).map(Status(_))
        case BULK => decodeBulk(bs.tail)
        case MULTIBULK => decodeMultiBulk(bs.tail)
        case _ => throw new Exception("Redis Protocol error: Got " + bs.head + " as initial reply byte >>"+ bs.tail.utf8String)
      }
    }
  }

  val decodeReplyPF: PartialFunction[ByteString, DecodeResult[RedisReply]] = {
    case bs if bs.head == INTEGER => decodeInteger(bs.tail)
    case bs if bs.head == STATUS => decodeString(bs.tail).map(Status(_))
    case bs if bs.head == BULK => decodeBulk(bs.tail)
    case bs if bs.head == MULTIBULK => decodeMultiBulk(bs.tail)
  }

  val decodeReplyStatus: PartialFunction[ByteString, DecodeResult[Status]] = {
    case bs if bs.head == STATUS => decodeString(bs.tail).map(Status(_))
  }

  val decodeReplyInteger: PartialFunction[ByteString, DecodeResult[Integer]] = {
    case bs if bs.head == INTEGER => decodeInteger(bs.tail)
  }

  val decodeReplyBulk: PartialFunction[ByteString, DecodeResult[Bulk]] = {
    case bs if bs.head == BULK => decodeBulk(bs.tail)
  }

  val decodeReplyMultiBulk: PartialFunction[ByteString, DecodeResult[MultiBulk]] = {
    case bs if bs.head == MULTIBULK => decodeMultiBulk(bs.tail)
  }

  val decodeReplyError: PartialFunction[ByteString, DecodeResult[Error]] = {
    case bs if bs.head == ERROR => decodeString(bs.tail).map(Error(_))
  }

  def decodeInteger(bs: ByteString): DecodeResult[Integer] = {
    decodeString(bs).map { (string) => Integer(string) }
  }

  def decodeString(bs: ByteString): DecodeResult[ByteString] = {
    val index = bs.indexOf('\n')
    if (index >= 0 && bs.length >= index + 1) {
      val reply = bs.take(index + 1 - LS.length)
      val tail = bs.drop(index + 1)
      val r = FullyDecoded(reply, tail)
      r
    } else {
      PartiallyDecoded(bs, decodeString)
    }
  }

  def decodeBulk(bs: ByteString): DecodeResult[Bulk] = {
    def decodeBulkBody(integer: Integer, bsRest: ByteString): DecodeResult[Bulk] = {
      val i = integer.toInt
      if (i < 0) {
        FullyDecoded(Bulk(None), bsRest)
      } else if (bsRest.length < (i + LS.length)) {
        PartiallyDecoded(bsRest, decodeBulkBody(integer, _))
      } else {
        val data = bsRest.take(i)
        FullyDecoded(Bulk(Some(data)), bsRest.drop(i).drop(LS.length))
      }
    }
    decodeInteger(bs).flatMap(decodeBulkBody)
  }

  def decodeMultiBulk(bs: ByteString): DecodeResult[MultiBulk] = {
    decodeInteger(bs).flatMap { (integer, bsRest) =>
      val i = integer.toInt
      if (i < 0) {
        FullyDecoded(MultiBulk(None), bsRest)
      } else if (i == 0) {
        FullyDecoded(MultiBulk(Some(Vector.empty)), bsRest)
      } else {
        val builder = Vector.newBuilder[RedisReply]
        builder.sizeHint(i)
        bulks(i, builder, bsRest)
      }
    }
  }

  def bulks(i: Int, builder: mutable.Builder[RedisReply, Vector[RedisReply]], byteString: ByteString): DecodeResult[MultiBulk] = {

    @tailrec
    def helper(i: Int, bs: ByteString): DecodeResult[Int] = {
      if (i > 0) {
        val reply = decodeReply(bs)
          .map { r =>
            builder += r
            i - 1
          }
        if (reply.isFullyDecoded)
          helper(i - 1, reply.rest)
        else
          reply
      } else {
        FullyDecoded(0, bs)
      }
    }

    helper(i, byteString).flatMap { (i, bs) =>
      if (i > 0) {
        bulks(i, builder, bs)
      } else {
        FullyDecoded[MultiBulk](MultiBulk(Some(builder.result())), bs)
      }
    }
  }
}
