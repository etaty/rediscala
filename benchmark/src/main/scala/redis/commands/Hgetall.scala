package redis.commands

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._
import redis.{Redis, RedisStateHelper}

import scala.concurrent.{Future, Await}

@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Hgetall extends RedisStateHelper {

  @Param(Array("1000", "5000", "10000"))
  var hashSize: Int = _

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
    implicit val exec = rs.akkaSystem.dispatchers.lookup(Redis.dispatcher.name)

    Await.result(rs.redis.flushall(), 20 seconds)
    val r = for (i <- 0 to hashSize) yield {
      rs.redis.hset(hsetKey, i.toString, i)
    }

    Await.result(Future.sequence(r), 20 seconds)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime))
  def measureHgetall(): Seq[Map[String, String]] = {
    import scala.concurrent.duration._
    implicit def exec = rs.akkaSystem.dispatchers.lookup(Redis.dispatcher.name)

    val r = for (i <- 0 to 100) yield {
      rs.redis.hgetall[String](hsetKey)
    }

    Await.result(Future.sequence(r), 30 seconds)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.SingleShotTime))
  def measureHgetallScalaRedis(): Seq[Option[Map[String, String]]] = {
    val r = for (i <- 0 to 100) yield {
      scalaRedis.hgetall(hsetKey)
    }
    r
  }
}
