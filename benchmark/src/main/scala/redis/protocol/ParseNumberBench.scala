package redis.protocol

import akka.util.ByteString
import org.openjdk.jmh.annotations.{Scope, State, Benchmark}

@State(Scope.Benchmark)
class ParseNumberBench {
  val bs = ByteString("123")

  @Benchmark
  def parseLongWithToLong(): Long = {
    bs.utf8String.toLong
  }

  @Benchmark
  def parseLongPositiveOnly(): Long = {
    PositiveLongParser.parse(bs)
  }

  @Benchmark
  def parseLongWithParseNumber(): Long = {
    ParseNumber.parseLong(bs)
  }

  @Benchmark
  def parseIntWithToInt(): Int = {
    bs.utf8String.toInt
  }

  @Benchmark
  def parseIntWithParseNumber(): Int = {
    ParseNumber.parseInt(bs)
  }
}

object PositiveLongParser {

  /**
    * @see https://github.com/undertow-io/undertow/commit/94cd8882a32351f85cdb40df06e939b093751c2e
    * @param byteString
    * @return
    */
  def parse(byteString: ByteString) : Long = {
    var value: Long = 0L
    val length = byteString.length

    if (length == 0) {
      throw new NumberFormatException(byteString.utf8String)
    }

    var multiplier = 1
    var i = length - 1

    while(i >= 0) {
      val c = byteString(i)

      if (c < '0' || c > '9') {
        throw new NumberFormatException(byteString.utf8String)
      }
      val digit = c - '0'
      value += digit * multiplier
      multiplier *= 10

      i -= 1
    }
    value
  }
}
