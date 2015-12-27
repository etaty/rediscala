package redis

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import org.openjdk.jmh.annotations._


import scala.concurrent.{Future, Await}

object RedisBench {
  /*
    * Fixtures have different Levels to control when they are about to run.
    * Level.Invocation is useful sometimes to do some per-invocation work,
    * which should not count as payload. PLEASE NOTE the timestamping and
    * synchronization for Level.Invocation helpers might significantly
    * offset the measurement, use with care. See Level.Invocation javadoc
    * for more discussion.
    *
    * Consider this sample:
    */


  @State(Scope.Benchmark)
  class RedisState {
    var akkaSystem: ActorSystem = _
    var redis: RedisClient = _
    val hsetKey = "hsetKey"

    @Setup(Level.Trial)
    def up: Unit = {
      akkaSystem = akka.actor.ActorSystem()
      redis = RedisClient()(akkaSystem)

      implicit val exec = akkaSystem.dispatcher

      import scala.concurrent.duration._
      val r = for(i <- 0 to 10000) yield {
        redis.hset(hsetKey, i.toString, i)
      }

      Await.result(Future.sequence(r), 20 seconds)

      Await.result(redis.ping(), 2 seconds)
    }

    @TearDown(Level.Trial)
    def down: Unit = {
      redis.stop()
      akkaSystem.shutdown
      akkaSystem.awaitTermination()
    }
  }

}

@OutputTimeUnit(TimeUnit.SECONDS)
class RedisBench {

  import RedisBench._

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime))
  def measurePing(rs: RedisState): Unit = {
    import scala.concurrent.duration._
    implicit val exec = rs.akkaSystem.dispatcher

    val r = for(i <- (0 to 500000).toVector) yield {
      rs.redis.ping()
    }

    val a = Await.ready(Future.sequence(r), 10 seconds)
    ()
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime))
  def measureHgetall10000(rs: RedisState): Unit = {
    import scala.concurrent.duration._
    implicit val exec = rs.akkaSystem.dispatcher

    val r = for(i <- 0 to 100) yield {
      rs.redis.hgetall(rs.hsetKey)
    }

    val a = Await.ready(Future.sequence(r), 10 seconds)
    ()
  }
}
