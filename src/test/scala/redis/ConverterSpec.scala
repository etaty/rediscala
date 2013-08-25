package redis

import org.specs2.mutable.Specification
import akka.util.ByteString

class ConverterSpec extends Specification {

  import redis.ByteStringSerializer._

  "RedisValueConverter" should {
    "String" in {
      String.serialize("super string !") mustEqual ByteString("super string !")
    }

    "Short" in {
      ShortConverter.serialize(123) mustEqual ByteString("123")
    }

    "Int" in {
      IntConverter.serialize(123) mustEqual ByteString("123")
    }

    "Long" in {
      LongConverter.serialize(123) mustEqual ByteString("123")
    }

    "Float" in {
      FloatConverter.serialize(123.123f) mustEqual ByteString("123.123")
    }

    "Double" in {
      DoubleConverter.serialize(123.123456) mustEqual ByteString("123.123456")
    }

    "Char" in {
      CharConverter.serialize('a') mustEqual ByteString('a')
    }

    "Byte" in {
      ByteConverter.serialize(123) mustEqual ByteString(123)
    }

    "ArrayByte" in {
      ArrayByteConverter.serialize(Array[Byte](1, 2, 3)) mustEqual ByteString(Array[Byte](1, 2, 3))
    }

    "ByteString" in {
      ByteStringConverter.serialize(ByteString("stupid")) mustEqual ByteString("stupid")
    }
  }

}
