package redis.commands

import redis._
import scala.concurrent.Await
import akka.util.ByteString
import redis.actors.ReplyErrorException

class TransactionsSpec extends RedisSpec {

  import Converter._

  "Transactions commands" should {
    "Transactions" in {
      val redisTransaction = redis.transaction()
      redisTransaction.exec()
      redisTransaction.watch("a")
      val set = redisTransaction.set("a", "abc")
      val decr = redisTransaction.decr("a")
      val get = redisTransaction.get("a")
      redisTransaction.exec()
      val r = for {
        s <- set
        g <- get
      } yield {
        s mustEqual true
        g mustEqual Some(ByteString("abc"))
      }
      Await.result(decr, timeOut) must throwA[ReplyErrorException]("ERR value is not an integer or out of range")
      Await.result(r, timeOut)
    }

  }
}
