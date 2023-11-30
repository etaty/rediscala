package redis.protocol

import org.specs2.mutable._
import org.apache.pekko.util.ByteString

class ParseNumberSpec extends Specification {

  import java.lang.Integer

  "ParseNumber.parseInt" should {
    "ok" in {
      ParseNumber.parseInt(ByteString("0")) mustEqual 0
      ParseNumber.parseInt(ByteString("10")) mustEqual 10
      ParseNumber.parseInt(ByteString("-10")) mustEqual -10
      ParseNumber.parseInt(ByteString("-123456")) mustEqual -123456
      ParseNumber.parseInt(ByteString("1234567890")) mustEqual 1234567890
      ParseNumber.parseInt(ByteString("-1234567890")) mustEqual -1234567890
    }

    "null" in {
      ParseNumber.parseInt(null) must throwA[NumberFormatException]
    }

    "lone \"+\" or \"-\"" in {
      ParseNumber.parseInt(ByteString("+")) must throwA[NumberFormatException]
      ParseNumber.parseInt(ByteString("-")) must throwA[NumberFormatException]
    }

    "invalid first char" in {
      ParseNumber.parseInt(ByteString("$")) must throwA[NumberFormatException]
    }

    "empty" in {
      ParseNumber.parseInt(ByteString.empty) must throwA[NumberFormatException]
    }

    "invalid char" in {
      ParseNumber.parseInt(ByteString("?")) must throwA[NumberFormatException]
    }

    "limit min" in {
      val l1 : Long = Integer.MIN_VALUE
      val l = l1 - 1
      ParseNumber.parseInt(ByteString(l.toString)) must throwA[NumberFormatException]
    }

    "limit max" in {
      val l1 : Long = Integer.MAX_VALUE
      val l = l1 + 1
      ParseNumber.parseInt(ByteString(l.toString)) must throwA[NumberFormatException]
    }

    "not a number" in {
      ParseNumber.parseInt(ByteString("not a number")) must throwA[NumberFormatException]
    }

    "launch exception before integer overflow" in {
      ParseNumber.parseInt(ByteString("-2147483650")) must throwA[NumberFormatException]
    }

  }
}
