package redis

import org.specs2.mutable.Specification
import akka.util.ByteString

class ConverterSpec extends Specification {

  import redis.RedisValueConverter._

  "RedisValueConverter" should {
    "String" in {
      StringConverter.from("super string !") mustEqual ByteString("super string !")
    }

    "Short" in {
      ShortConverter.from(123) mustEqual ByteString("123")
    }

    "Int" in {
      IntConverter.from(123) mustEqual ByteString("123")
    }

    "Long" in {
      LongConverter.from(123) mustEqual ByteString("123")
    }

    "Float" in {
      FloatConverter.from(123.123f) mustEqual ByteString("123.123")
    }

    "Double" in {
      DoubleConverter.from(123.123456) mustEqual ByteString("123.123456")
    }

    "Char" in {
      CharConverter.from('a') mustEqual ByteString('a')
    }

    "Byte" in {
      ByteConverter.from(123) mustEqual ByteString(123)
    }

    "ArrayByte" in {
      ArrayByteConverter.from(Array[Byte](1, 2, 3)) mustEqual ByteString(Array[Byte](1, 2, 3))
    }

    "ByteString" in {
      ByteStringConverter.from(ByteString("stupid")) mustEqual ByteString("stupid")
    }
  }

}
