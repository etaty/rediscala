package redis.bench

import org.scalameter.api._
import akka.util.ByteString
import redis.protocol.ParseNumber

object ByteStringBench extends PerformanceTest.Microbenchmark {

  val sizes = Gen.range("size")(200000, 800000, 100000)

  val ranges = for {
    size <- sizes
  } yield 0 until size

  performance of "String to Int" in {
    measure method "parseInt" in {
      using(ranges) in {
        i =>
          for {
            ii <- i
          } yield {
            val a = Integer.parseInt(StringToIntData.data(ii % StringToIntData.data.size).utf8String)
            a
          }
      }
    }

    measure method "valueOf" in {
      using(ranges) in {
        i =>
          for {
            ii <- i
          } yield {
            Integer.valueOf(StringToIntData.data(ii % StringToIntData.data.size).utf8String)
          }
      }
    }

    measure method "faster" in {
      using(ranges) in {
        i =>
          for {
            ii <- i
          } yield {
            ParseNumber.parseInt(StringToIntData.data(ii % StringToIntData.data.size))
          }
      }
    }
  }

  performance of "ByteString() == 'OK'" in {
    measure method "utf8String" in {
      using(ranges) in {
        i =>
          for {
            ii <- i
          } yield {
            DataEquality.data(ii % DataEquality.data.size).utf8String == "OK"
          }
      }
    }

    measure method "ByteString == ByteString(\"OK\")" in {
      using(ranges) in {
        i =>
          for {
            ii <- i
          } yield {
            DataEquality.data(ii % DataEquality.data.size) == DataEquality.ok
          }
      }
    }
  }
}

object DataEquality {
  val data = Seq(ByteString("OK"), ByteString("Not ok"), ByteString(""))
  val ok = ByteString("OK")
}

object StringToIntData {
  //val data: Seq[String] = (0 to 1000).toSeq.map(i => i.toString)
  val data: Seq[ByteString] = (0 to 1000).toSeq.map(i => ByteString(i.toString))
}