package redis.protocol

import java.nio.charset.Charset

import akka.util.ByteString

import scala.compat.Platform._

object RedisProtocolRequest {
  val UTF8_CHARSET = Charset.forName("UTF-8")
  val LS_STRING = "\r\n"
  val LS = LS_STRING.getBytes(UTF8_CHARSET)

  def multiBulk(command: String, args: Seq[ByteString]): ByteString = {
    val argsSizeString = (args.size + 1).toString
    var length: Int = 1 + argsSizeString.length + LS.length

    val cmdLenghtString = command.length.toString

    length += 1 + cmdLenghtString.length + LS.length + command.length + LS.length

    args.foreach(arg => {
      val argLengthString = arg.length.toString
      length += 1 + argLengthString.length + LS.length + arg.length + LS.length
    })

    val bytes: Array[Byte] = new Array(length)
    var i: Int = 0
    bytes.update(i, '*')
    i += 1
    arraycopy(argsSizeString.getBytes(UTF8_CHARSET), 0, bytes, i, argsSizeString.length)
    i += argsSizeString.length
    arraycopy(LS, 0, bytes, i, LS.length)
    i += LS.length

    bytes.update(i, '$')
    i += 1
    arraycopy(cmdLenghtString.getBytes(UTF8_CHARSET), 0, bytes, i, cmdLenghtString.length)
    i += cmdLenghtString.length
    arraycopy(LS, 0, bytes, i, LS.length)
    i += LS.length
    arraycopy(command.getBytes(UTF8_CHARSET), 0, bytes, i, command.length)
    i += command.length
    arraycopy(LS, 0, bytes, i, LS.length)
    i += LS.length

    args.foreach(arg => {
      bytes.update(i, '$')
      i += 1

      val argLengthString = arg.length.toString
      arraycopy(argLengthString.getBytes(UTF8_CHARSET), 0, bytes, i, argLengthString.length)
      i += argLengthString.length
      arraycopy(LS, 0, bytes, i, LS.length)
      i += LS.length

      val argArray = arg.toArray
      arraycopy(argArray, 0, bytes, i, argArray.length)
      i += argArray.length

      arraycopy(LS, 0, bytes, i, LS.length)
      i += LS.length
    })
    ByteString(bytes)
  }

  def inline(command: String): ByteString = ByteString(command + LS_STRING)
}
