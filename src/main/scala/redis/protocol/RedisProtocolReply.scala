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


object RedisProtocolReply {
  val ERROR     = '-'
  val STATUS    = '+'
  val INTEGER   = ':'
  val BULK      = '$'
  val MULTIBULK = '*'

  val LS = "\r\n".getBytes("UTF-8")

  def decodeReply(bs: ByteString): Option[(RedisReply, ByteString)] = {
    if (bs.isEmpty) {
      None
    } else {
      bs.head match {
        case ERROR => decodeString(bs.tail).map(r => (Error(r._1), r._2))
        case INTEGER => decodeInteger(bs.tail)
        case STATUS => decodeString(bs.tail).map(r => (Status(r._1), r._2))
        case BULK => decodeBulk(bs.tail)
        case MULTIBULK => decodeMultiBulk(bs.tail)
        case _ => throw new Exception("Redis Protocol error: Got " + bs.head + " as initial reply byte")
      }
    }
  }

  val decodeReplyPF: PartialFunction[ByteString, Option[(RedisReply, ByteString)]] = {
    case bs if bs.head == INTEGER => decodeInteger(bs.tail)
    case bs if bs.head == STATUS => decodeString(bs.tail).map(r => (Status(r._1), r._2))
    case bs if bs.head == BULK => decodeBulk(bs.tail)
    case bs if bs.head == MULTIBULK => decodeMultiBulk(bs.tail)
  }

  val decodeReplyStatus: PartialFunction[ByteString, Option[(Status, ByteString)]] = {
    case bs if bs.head == STATUS => decodeString(bs.tail).map(r => (Status(r._1), r._2))
  }

  val decodeReplyInteger: PartialFunction[ByteString, Option[(Integer, ByteString)]] = {
    case bs if bs.head == INTEGER => decodeInteger(bs.tail)
  }

  val decodeReplyBulk: PartialFunction[ByteString, Option[(Bulk, ByteString)]] = {
    case bs if bs.head == BULK => decodeBulk(bs.tail)
  }

  val decodeReplyMultiBulk: PartialFunction[ByteString, Option[(MultiBulk, ByteString)]] = {
    case bs if bs.head == MULTIBULK => decodeMultiBulk(bs.tail)
  }

  val decodeReplyError: PartialFunction[ByteString, Option[(Error, ByteString)]] = {
    case bs if bs.head == ERROR => decodeString(bs.tail).map(r => (Error(r._1), r._2))
  }

  def decodeInteger(bs: ByteString): Option[(Integer, ByteString)] = {
    decodeString(bs).map(r => {
      val i = Integer(r._1)
      (i, r._2)
    })
  }

  def decodeString(bs: ByteString): Option[(ByteString, ByteString)] = {
    val index = bs.indexOf('\n')
    if (index >= 0 && bs.length >= index + 1) {
      val reply = bs.take(index + 1 - LS.length)
      val tail = bs.drop(index + 1)
      Some(reply -> tail)
    } else {
      None
    }
  }

  def decodeBulk(bs: ByteString): Option[(Bulk, ByteString)] = {
    decodeInteger(bs).flatMap(r => {
      val i = r._1.toInt
      val tail = r._2
      if (i < 0) {
        Some(Bulk(None) -> tail)
      } else if (tail.length < (i + LS.length)) {
        None
      } else {
        val data = tail.take(i)
        Some(Bulk(Some(data)) -> tail.drop(i).drop(LS.length))
      }
    })
  }

  def decodeMultiBulk(bs: ByteString): Option[(MultiBulk, ByteString)] = {
    decodeInteger(bs).flatMap(r => {
      val i = r._1.toInt
      val tail = r._2
      if (i < 0) {
        Some(MultiBulk(None) -> tail)
      } else if (i == 0) {
        Some(MultiBulk(Some(Vector.empty)) -> tail)
      } else {
        @tailrec
        def bulks(bs: ByteString, i: Int, acc: mutable.Buffer[RedisReply]): Option[(MultiBulk, ByteString)] = {
          if (i > 0) {
            val reply = decodeReply(bs)
            if (reply.nonEmpty) {
              acc.append(reply.get._1)
              bulks(reply.get._2, i - 1, acc)
            } else {
              None
            }
          } else {
            Some(MultiBulk(Some(acc.toVector)) -> bs)
          }
        }
        bulks(tail, i, mutable.Buffer())
      }
    })
  }
}
