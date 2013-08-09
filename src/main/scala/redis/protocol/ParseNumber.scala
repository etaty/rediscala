package redis.protocol

import akka.util.ByteString

object ParseNumber {

  import java.lang.Integer

  /**
   * Fast decoder from ByteString to Int
   * Code from openjdk java.lang.Integer parseInt()
   * @see http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7-b147/java/lang/Integer.java#Integer.parseInt%28java.lang.String%2Cint%29
   * 3 times faster than java.lang.Integer.parseInt(ByteString().utf8String))
   * @param byteString
   * @return
   */
  def parseInt(byteString: ByteString): Int = {
    if (byteString == null) {
      throw new NumberFormatException("null")
    }

    val radix = 10
    var result = 0
    var negative = false
    var i = 0
    val len = byteString.length
    var limit = -Integer.MAX_VALUE

    if (len > 0) {
      val firstChar = byteString(0)
      if (firstChar < '0') {
        // Possible leading "+" or "-"
        if (firstChar == '-') {
          negative = true
          limit = Integer.MIN_VALUE
        } else if (firstChar != '+')
          throw new NumberFormatException(byteString.toString())

        if (len == 1) // Cannot have lone "+" or "-"
          throw new NumberFormatException(byteString.toString())
        i += 1
      }
      val multmin = limit / radix
      while (i < len) {
        // Accumulating negatively avoids surprises near MAX_VALUE
        val digit = Character.digit(byteString(i), radix)
        i += 1
        if (digit < 0) {
          throw new NumberFormatException(byteString.toString())
        }
        if (result < multmin) {
          throw new NumberFormatException(byteString.toString())
        }
        result *= radix
        if (result < limit + digit) {
          throw new NumberFormatException(byteString.toString())
        }
        result -= digit
      }
    } else {
      throw new NumberFormatException(byteString.toString())
    }
    if (negative)
      result
    else
      -result
  }
}
