package redis.protocol

import org.apache.pekko.util.ByteString
import org.specs2.mutable._

class RedisProtocolReplySpec extends Specification {

  "Decode reply" should {
    "fail" in {
      val bs = ByteString("!!")
      RedisProtocolReply.decodeReply(bs) must throwA[Exception]
    }
  }

  "Decode String" should {
    "decode simple string" in {
      val ok = ByteString("OK\r\n")
      RedisProtocolReply.decodeString(ok) mustEqual FullyDecoded(ok.dropRight(2), ByteString())
    }
    "wait for more content" in {
      val waitForMore = ByteString("waiting for more")
      val r = RedisProtocolReply.decodeString(waitForMore)
      r.isFullyDecoded must beFalse
      r.rest mustEqual waitForMore
    }
    "decode and keep the tail" in {
      val decode = ByteString("decode\r\n")
      val keepTail = ByteString("keep the tail")
      RedisProtocolReply.decodeString(decode ++ keepTail) mustEqual FullyDecoded(decode.dropRight(2), keepTail)
    }
  }

  "Decode integer" should {
    "decode positive integer" in {
      val int = ByteString("6\r\n")
      RedisProtocolReply.decodeInteger(int) mustEqual FullyDecoded(Integer(ByteString("6")), ByteString())
    }
    "decode negative integer" in {
      val int = ByteString("-6\r\n")
      val decoded = RedisProtocolReply.decodeInteger(int)
      decoded mustEqual FullyDecoded(Integer(ByteString("-6")), ByteString())
    }
  }

  "Decode bulk" should {
    "decode simple bulk" in {
      val bulk = ByteString("6\r\nfoobar\r\n")
      RedisProtocolReply.decodeBulk(bulk) mustEqual FullyDecoded(Bulk(Some(ByteString("foobar"))), ByteString())
    }
    "decode Null Bulk Reply" in {
      val bulk = ByteString("-1\r\n")
      RedisProtocolReply.decodeBulk(bulk) mustEqual FullyDecoded(Bulk(None), ByteString())
    }
  }

  "Decode multi bulk" should {
    "decode simple" in {
      val multibulkString = ByteString("4\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n$5\r\nWorld\r\n")
      val multibulk = Some(Vector(Bulk(Some(ByteString("foo"))), Bulk(Some(ByteString("bar"))), Bulk(Some(ByteString("Hello"))), Bulk(Some(ByteString("World")))))
      RedisProtocolReply.decodeMultiBulk(multibulkString) mustEqual FullyDecoded(MultiBulk(multibulk), ByteString())
    }
    "decode waiting" in {
      val multibulkString = ByteString("4\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$5\r\nHello\r\n$50\r\nWaiting ...")
      val r = RedisProtocolReply.decodeMultiBulk(multibulkString)
      r.isFullyDecoded must beFalse
      r.rest mustEqual ByteString()
    }
    "decode Empty Multi Bulk" in {
      val emptyMultiBulk = ByteString("0\r\n")
      RedisProtocolReply.decodeMultiBulk(emptyMultiBulk) mustEqual FullyDecoded(MultiBulk(Some(Vector())), ByteString())
    }
    "decode Null Multi Bulk" in {
      val nullMultiBulk = ByteString("-1\r\n")
      RedisProtocolReply.decodeMultiBulk(nullMultiBulk) mustEqual FullyDecoded(MultiBulk(None), ByteString())
    }
    "decode Null element in Multi Bulk" in {
      val nullElementInMultiBulk = ByteString("3\r\n$3\r\nfoo\r\n$-1\r\n$3\r\nbar\r\n")
      val multibulk = Some(Vector(Bulk(Some(ByteString("foo"))), Bulk(None), Bulk(Some(ByteString("bar")))))
      RedisProtocolReply.decodeMultiBulk(nullElementInMultiBulk) mustEqual FullyDecoded(MultiBulk(multibulk), ByteString())
    }
    "decode different reply type" in {
      val diff = ByteString("5\r\n:1\r\n:2\r\n:3\r\n:4\r\n$6\r\nfoobar\r\n")
      val multibulk = Some(Vector(Integer(ByteString("1")), Integer(ByteString("2")), Integer(ByteString("3")), Integer(ByteString("4")), Bulk(Some(ByteString("foobar")))))
      RedisProtocolReply.decodeMultiBulk(diff) mustEqual FullyDecoded(MultiBulk(multibulk), ByteString())
    }
  }
}
