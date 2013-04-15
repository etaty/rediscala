package redis

import akka.util.ByteString
import scala.annotation.tailrec
import scala.collection.mutable

sealed trait Reply

case class Status(status: String) extends Reply {
  def toBoolean: Boolean = status match {
    case "OK" => true
    case _ => false
  }
}

case class Error(error: String) extends Reply

case class Integer(int: Int) extends Reply

case class Bulk(response: Option[ByteString]) extends Reply

case class MultiBulk(responses: Option[Seq[Reply]]) extends Reply

object RedisProtocolReply {
  val ERROR = '-'
  val STATUS = '+'
  val INTEGER = ':'
  val BULK = '$'
  val MULTIBULK = '*'

  val LS = "\r\n".getBytes("UTF-8")

  def decodeReply(bs: ByteString): Option[(Reply, ByteString)] = {
    if (bs.isEmpty) {
      None
    } else {
      bs.head match {
        case ERROR => decodeString(bs.tail).map(r => (Error(r._1.utf8String), r._2))
        case INTEGER => decodeInteger(bs.tail).map(r => (Integer(r._1), r._2))
        case STATUS => decodeString(bs.tail).map(r => (Status(r._1.utf8String), r._2))
        case BULK => decodeBulk(bs.tail)
        case MULTIBULK => decodeMultiBulk(bs.tail)
        case _ => throw new Exception("Redis Protocol error: Got " + bs.head + " as initial reply byte")
      }
    }
  }

  def decodeInteger(bs: ByteString): Option[(Int, ByteString)] = {
    decodeString(bs).map(r => {
      val i = java.lang.Integer.parseInt(r._1.utf8String)
      (i, r._2)
    })
  }

  def decodeString(bs: ByteString): Option[(ByteString, ByteString)] = {
    val index = bs.indexOf('\n')
    if (index >= 0 && bs.length >= index + 1) {
      val reply = bs.take(index + 1 - LS.length)
      val tail = bs.drop(index + 1)
      Some(reply, tail)
    } else {
      None
    }
  }

  def decodeBulk(bs: ByteString): Option[(Bulk, ByteString)] = {
    decodeInteger(bs).flatMap(r => {
      val i = r._1
      val tail = r._2
      if (i < 0) {
        Some(Bulk(None), tail)
      } else if (tail.length < (i + LS.length)) {
        None
      } else {
        val data = tail.take(i)
        Some(Bulk(Some(data)), tail.drop(i).drop(LS.length))
      }
    })
  }

  def decodeMultiBulk(bs: ByteString): Option[(MultiBulk, ByteString)] = {
    decodeInteger(bs).flatMap(r => {
      val i = r._1
      val tail = r._2
      if (i < 0) {
        Some(MultiBulk(None), tail)
      } else if (i == 0) {
        Some(MultiBulk(Some(Nil)), tail)
      } else {
        @tailrec
        def bulks(bs: ByteString, i: Int, acc: mutable.Buffer[Reply]): Option[(MultiBulk, ByteString)] = {
          if (i > 0) {
            val reply = decodeReply(bs)
            if (reply.nonEmpty) {
              acc.append(reply.get._1)
              bulks(reply.get._2, i - 1, acc)
            } else {
              None
            }
          } else {
            Some(MultiBulk(Some(acc.toSeq)), bs)
          }
        }
        bulks(tail, i, mutable.Buffer())
      }
    })
  }
}
