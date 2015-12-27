package redis.protocol

import akka.util.ByteString

object ParseNumber {

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

    var result = 0
    var negative = false
    var i = 0
    val len = byteString.length
    var limit = -java.lang.Integer.MAX_VALUE

    if (len > 0) {
      val firstChar = byteString(0)
      if (firstChar < '0') {
        // Possible leading "+" or "-"
        if (firstChar == '-') {
          negative = true
          limit = java.lang.Integer.MIN_VALUE
        } else if (firstChar != '+')
          throw new NumberFormatException(byteString.toString())

        if (len == 1) // Cannot have lone "+" or "-"
          throw new NumberFormatException(byteString.toString())
        i += 1
      }
      val multmin = limit / 10
      while (i < len) {
        // Accumulating negatively avoids surprises near MAX_VALUE
        val digit = byteString(i) - '0'
        i += 1
        if (digit < 0 || digit > 9) {
          throw new NumberFormatException(byteString.toString())
        }
        if (result < multmin) {
          throw new NumberFormatException(byteString.toString())
        }
        result *= 10
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

  def parseLong(byteString: ByteString): Long = {
    if (byteString == null) {
      throw new NumberFormatException("null")
    }

    var result = 0L
    var negative = false
    var i = 0
    val len = byteString.length
    var limit = -java.lang.Long.MAX_VALUE

    if (len > 0) {
      val firstChar = byteString(0)
      if (firstChar < '0') {
        // Possible leading "+" or "-"
        if (firstChar == '-') {
          negative = true
          limit = java.lang.Long.MIN_VALUE
        } else if (firstChar != '+')
          throw new NumberFormatException(byteString.toString())

        if (len == 1) // Cannot have lone "+" or "-"
          throw new NumberFormatException(byteString.toString())
        i += 1
      }
      val multmin = limit / 10
      while (i < len) {
        // Accumulating negatively avoids surprises near MAX_VALUE
        val digit = byteString(i) - '0'
        i += 1
        if (digit < 0 || digit > 9) {
          throw new NumberFormatException(byteString.toString())
        }
        if (result < multmin) {
          throw new NumberFormatException(byteString.toString())
        }
        result *= 10
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
