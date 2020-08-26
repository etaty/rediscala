package redis.util

import org.specs2.mutable.Specification

class CRC16Spec extends Specification {

  "CRC16" should {
    "crc16" in {
      CRC16.crc16("aaaaa") mustEqual 29740
      CRC16.crc16("someValue") mustEqual 17816
      CRC16.crc16("someValue1234567890") mustEqual 42744
      CRC16.crc16("foo bar baz") mustEqual 44067
    }
  }
}
