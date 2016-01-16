package redis.commands

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._
import redis.{Redis, RedisStateHelper}

import scala.concurrent.{Future, Await}

@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Ping extends RedisStateHelper {

  @Param(Array("10000", "100000", "300000", "500000"))
  var iteration: Int = _

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime))
  def measurePing(): Unit = {
    import scala.concurrent.duration._
    implicit def exec = rs.akkaSystem.dispatchers.lookup(Redis.dispatcher.name)

    val r = for (i <- (0 to iteration).toVector) yield {
      rs.redis.ping()
    }

    val a = Await.ready(Future.sequence(r), 10 seconds)
    ()
  }
}
