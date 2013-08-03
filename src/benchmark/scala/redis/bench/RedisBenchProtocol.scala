package redis.bench

import redis.protocol.RedisProtocolRequest
import akka.util.ByteString
import org.scalameter.api._

object RedisBenchProtocol extends PerformanceTest.Regression {

  override def reporter: Reporter = Reporter.Composite(
    new RegressionReporter(
      RegressionReporter.Tester.Accepter(),
      RegressionReporter.Historian.Complete()),
    HtmlReporter(embedDsv = true)
  )

  def persistor = new SerializationPersistor()

  val sizes = Gen.range("size")(20000, 80000, 10000)

  val ranges = for {
    size <- sizes
  } yield 0 until size


  performance of "Protocol request encode" in {
    val argsBulk = Seq(ByteString("i"), ByteString("abc"), ByteString("iksjdlkgdfgjfdgjdfkgjjqsdqlksdqklsjdqljsdqkjsd"))
    /*
    measure method "stupid" in {
      using(sizes) in {
        i =>
          println(i)
      }
    }*/
    ///*
    measure method "multiBulk (slow)" in {
      using(ranges) in {
        i =>
          for {
            ii <- i
          } yield {
            RedisProtocolRequestSlow.multiBulkSlow("INCR", argsBulk)
          }
      }
    }

    measure method "multiBulk2" in {
      using(ranges) in {
        i =>
          for {
            ii <- i
          } yield {
            RedisProtocolRequest.multiBulk("INCR", argsBulk)
          }
      }
    }

    measure method "inline" in {
      using(ranges) in {
        i =>
          for {
            ii <- i
          } yield {
            RedisProtocolRequest.inline("PING")
          }
      }
    }
    //*/
  }
}

object RedisProtocolRequestSlow {

  import RedisProtocolRequest._

  /**
   * 25% slower
   * @param command
   * @param args
   * @return
   */
  def multiBulkSlow(command: String, args: Seq[ByteString]): ByteString = {
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
}