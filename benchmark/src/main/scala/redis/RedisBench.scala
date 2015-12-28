package redis

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import scala.concurrent.{Future, Await}

@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class RedisBench extends RedisStateHelper {

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime))
  def measurePing(): Unit = {
    import scala.concurrent.duration._
    implicit def exec = rs.akkaSystem.dispatcher

    val r = for (i <- (0 to 500000).toVector) yield {
      rs.redis.ping()
    }

    val a = Await.ready(Future.sequence(r), 10 seconds)
    ()
  }
}
