package redis.protocol

import akka.util.ByteString

object RedisProtocolRequest {
  val LS_STRING = "\r\n"
  val LS = LS_STRING.getBytes("UTF-8")

  def multiBulk(command: String, args: Seq[ByteString]): ByteString = {
    val requestBuilder = ByteString.newBuilder
    requestBuilder.putBytes((s"*${args.size + 1}").getBytes("UTF-8"))
    requestBuilder.putBytes(LS)

    val builder = (ByteString(command.getBytes("UTF-8")) +: args).foldLeft(requestBuilder) {
      case (acc, arg) =>
        acc.putBytes((s"$$${arg.size}").getBytes("UTF-8"))
        acc.putBytes(LS)
        acc ++= (arg)
        acc.putBytes(LS)

        acc
    }

    builder.result()
  }

  def inline(command: String): ByteString = ByteString(command + LS_STRING)
}
