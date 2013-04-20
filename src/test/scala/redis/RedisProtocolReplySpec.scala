package redis

import akka.util.ByteString
import org.specs2.mutable._

class RedisProtocolReplySpec extends Specification {

  "Decode String" should {
    "decode simple string" in {
      val ok = ByteString("OK\r\n")
      RedisProtocolReply.decodeString(ok) mustEqual Some(ok.dropRight(2), ByteString())
    }
    "wait for more content" in {
      val waitForMore = ByteString("waiting for more")
      RedisProtocolReply.decodeString(waitForMore) mustEqual None
    }
    "decode and keep the tail" in {
      val decode = ByteString("decode\r\n")
      val keepTail = ByteString("keep the tail")
      RedisProtocolReply.decodeString(decode ++ keepTail) mustEqual Some(decode.dropRight(2), keepTail)
    }
  }

  "Decode integer" should {
    "decode positive integer" in {
      val int = ByteString("6\r\n")
      RedisProtocolReply.decodeInteger(int) mustEqual Some(Integer(ByteString("6")), ByteString())
    }
    "decode negative integer" in {
      val int = ByteString("-6\r\n")
      val decoded = RedisProtocolReply.decodeInteger(int)
      decoded mustEqual Some(Integer(ByteString("-6")), ByteString())
      decoded.get._1.toInt mustEqual -6
    }
  }

  "Decode bulk" should {
    "decode simple bulk" in {
      val bulk = ByteString("6\r\nfoobar\r\n")
      RedisProtocolReply.decodeBulk(bulk) mustEqual Some(Bulk(Some(ByteString("foobar"))), ByteString())
    }
    "decode Null Bulk Reply" in {
      val bulk = ByteString("-1\r\n")
      RedisProtocolReply.decodeBulk(bulk) mustEqual Some(Bulk(None), ByteString())
    }
  }

  "Decode multi bulk" should {
    "decode simple" in {
      val multibulkString = ByteString("4\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n$5\r\nWorld\r\n")
      val multibulk = Some(Seq(Bulk(Some(ByteString("foo"))), Bulk(Some(ByteString("bar"))), Bulk(Some(ByteString("Hello"))), Bulk(Some(ByteString("World")))))
      RedisProtocolReply.decodeMultiBulk(multibulkString) mustEqual Some(MultiBulk(multibulk), ByteString())
    }
    "decode waiting" in {
      val multibulkString = ByteString("4\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n$50\r\nWaiting ...")
      RedisProtocolReply.decodeMultiBulk(multibulkString) mustEqual None
    }
    "decode Empty Multi Bulk" in {
      val emptyMultiBulk = ByteString("0\r\n")
      RedisProtocolReply.decodeMultiBulk(emptyMultiBulk) mustEqual Some(MultiBulk(Some(Seq())), ByteString())
    }
    "decode Null Multi Bulk" in {
      val nullMultiBulk = ByteString("-1\r\n")
      RedisProtocolReply.decodeMultiBulk(nullMultiBulk) mustEqual Some(MultiBulk(None), ByteString())
    }
    "decode Null element in Multi Bulk" in {
      val nullElementInMultiBulk = ByteString("3\r\n$3\r\nfoo\r\n$-1\r\n$3\r\nbar\r\n")
      val multibulk = Some(Seq(Bulk(Some(ByteString("foo"))), Bulk(None), Bulk(Some(ByteString("bar")))))
      RedisProtocolReply.decodeMultiBulk(nullElementInMultiBulk) mustEqual Some(MultiBulk(multibulk), ByteString())
    }
    "decode different reply type" in {
      val diff = ByteString("5\r\n:1\r\n:2\r\n:3\r\n:4\r\n$6\r\nfoobar\r\n")
      val multibulk = Some(Seq(Integer(ByteString("1")), Integer(ByteString("2")), Integer(ByteString("3")), Integer(ByteString("4")), Bulk(Some(ByteString("foobar")))))
      RedisProtocolReply.decodeMultiBulk(diff) mustEqual Some(MultiBulk(multibulk), ByteString())
    }
  }
}
