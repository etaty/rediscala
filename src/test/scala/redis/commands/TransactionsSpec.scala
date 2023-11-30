package redis.commands

import redis._
import scala.concurrent.Await
import org.apache.pekko.util.ByteString
import redis.actors.ReplyErrorException
import redis.protocol.{Bulk, Status, MultiBulk}

class TransactionsSpec extends RedisStandaloneServer {

  "Transactions commands" should {
    "basic" in {
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

    "function api" in {
      "empty" in {
        val empty = redis.multi().exec()
        Await.result(empty, timeOut) mustEqual MultiBulk(Some(Vector()))
      }
      val redisTransaction = redis.multi(redis => {
        redis.set("a", "abc")
        redis.get("a")
      })
      val exec = redisTransaction.exec()
      "non empty" in {
        Await.result(exec, timeOut) mustEqual MultiBulk(Some(Vector(Status(ByteString("OK")), Bulk(Some(ByteString("abc"))))))
      }
      "reused" in {
        redisTransaction.get("transactionUndefinedKey")
        val exec = redisTransaction.exec()
        Await.result(exec, timeOut) mustEqual MultiBulk(Some(Vector(Status(ByteString("OK")), Bulk(Some(ByteString("abc"))), Bulk(None))))
      }
      "watch" in {
        val transaction = redis.watch("transactionWatchKey")
        transaction.watcher.result() mustEqual Set("transactionWatchKey")
        transaction.unwatch()
        transaction.watcher.result() must beEmpty
        val set = transaction.set("transactionWatch", "value")
        transaction.exec()
        val r = for {
          s <- set
        } yield {
          s must beTrue
        }
        Await.result(r, timeOut)
      }
    }

  }
}
