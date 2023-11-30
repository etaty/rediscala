package redis.protocol

import org.apache.pekko.util.ByteString
import org.specs2.mutable._

class RedisProtocolRequestSpec extends Specification {

  "Encode request" should {
    "inline" in {
      RedisProtocolRequest.inline("PING") mustEqual ByteString("PING\r\n")
    }
    "multibulk" in {
      val encoded = RedisProtocolRequest.multiBulk("SET", Seq(ByteString("mykey"), ByteString("myvalue")))
      encoded mustEqual ByteString("*3\r\n$3\r\nSET\r\n$5\r\nmykey\r\n$7\r\nmyvalue\r\n")
    }
  }

}
