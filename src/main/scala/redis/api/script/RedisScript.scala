package redis.api.script

import java.security.MessageDigest

case class RedisScript(script: String) {
  lazy val sha1 = {
    val messageDigestSha1 = MessageDigest.getInstance("SHA-1")
    messageDigestSha1.digest(script.getBytes("UTF-8")).map("%02x".format(_)).mkString
  }
}
