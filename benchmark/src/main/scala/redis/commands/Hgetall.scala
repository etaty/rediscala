package redis.commands

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._
import redis.RedisStateHelper

import scala.concurrent.{Future, Await}

@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Hgetall extends RedisStateHelper {

  val hsetKey = "hsetKey"

  var scalaRedis : com.redis.RedisClient = _

  @Setup(Level.Trial)
  def upScalaRedis(): Unit = {
    scalaRedis = new com.redis.RedisClient("localhost", 6379)
    scalaRedis.ping
  }

  @TearDown(Level.Trial)
  def downScalaRedis(): Unit = {
    scalaRedis.disconnect
  }

  override def initRedisState(): Unit = {
    import scala.concurrent.duration._
    implicit val exec = rs.akkaSystem.dispatcher

    val r = for (i <- 0 to 10000) yield {
      rs.redis.hset(hsetKey, i.toString, i)
    }

    Await.result(Future.sequence(r), 20 seconds)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime))
  def measureHgetall10000(): Unit = {
    import scala.concurrent.duration._
    implicit def exec = rs.akkaSystem.dispatcher

    val r = for (i <- 0 to 100) yield {
      rs.redis.hgetall(hsetKey)
    }

    val a = Await.ready(Future.sequence(r), 10 seconds)
    ()
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime))
  def measureHgetall10000ScalaRedis(): Unit = {
    //import scala.concurrent.duration._
    //implicit def exec = rs.akkaSystem.dispatcher

    val r = for (i <- 0 to 100) yield {
      scalaRedis.hgetall(hsetKey)
    }

    //val a = Await.ready(Future.sequence(r), 10 seconds)
    ()
  }
}
