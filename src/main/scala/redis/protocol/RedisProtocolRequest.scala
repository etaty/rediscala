package redis.protocol

import akka.util.ByteString
import java.nio.charset.Charset

object RedisProtocolRequest {
  val UTF8_CHARSET = Charset.forName("UTF-8")
  val LS_STRING = "\r\n"
  val LS = LS_STRING.getBytes(UTF8_CHARSET)


  def multiBulk(command: String, args: Seq[ByteString]): ByteString = {
    val requestBuilder = ByteString.newBuilder
    requestBuilder.putByte('*')
    requestBuilder.putBytes((args.size + 1).toString.getBytes(UTF8_CHARSET))
    requestBuilder.putBytes(LS)

    requestBuilder.putByte('$')
    requestBuilder.putBytes(command.length.toString.getBytes(UTF8_CHARSET))
    requestBuilder.putBytes(LS)
    requestBuilder.putBytes(command.getBytes(UTF8_CHARSET))
    requestBuilder.putBytes(LS)

    args.foreach(arg => {
      requestBuilder.putByte('$')
      requestBuilder.putBytes(arg.length.toString.getBytes(UTF8_CHARSET))
      requestBuilder.putBytes(LS)
      requestBuilder ++= arg
      requestBuilder.putBytes(LS)
    })

    requestBuilder.result()
  }

  def inline(command: String): ByteString = ByteString(command + LS_STRING)
}
