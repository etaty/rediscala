package redis.bench

import scala.concurrent._
import scala.concurrent.duration._
import org.scalameter.api._

import akka.actor.ActorSystem
import scala.collection.Iterator

import org.scalameter._
import org.scalameter.api.Executor
import org.scalameter.api.Aggregator
import org.scalameter.api.Gen
import org.scalameter.api.PerformanceTest
import org.scalameter.api.Reporter
import org.scalameter.Executor.Warmer
import org.scalameter.utils
import redis.{RedisServer, RedisClientPool, RedisClient}
import org.scalameter.execution

object RedisBenchPool extends PerformanceTest {

  override def reporter: Reporter = Reporter.Composite(
    new RegressionReporter(
      RegressionReporter.Tester.Accepter(),
      RegressionReporter.Historian.Complete()),
    HtmlReporter(embedDsv = true)
  )

  def warmer: Executor.Warmer = FixDefault()

  import Executor.Measurer

  def aggregator = Aggregator.complete(Aggregator.average)

  def measurer: Measurer = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation with Measurer.OutlierElimination with Measurer.RelativeNoise

  //def measurer: Measurer = new Executor.Measurer.MemoryFootprint

  def executor: Executor = new execution.SeparateJvmsExecutor(warmer, aggregator, measurer)


  def persistor = new SerializationPersistor()

  def exponential(axisName: String)(from: Int, until: Int, factor: Int): Gen[(Int, RedisBenchContextPool)] = new Gen[(Int, RedisBenchContextPool)] {
    def warmupset = {
      Iterator.single(((until - from) / 2, new RedisBenchContextPool()))
    }

    def dataset = Iterator.iterate(from)(_ * factor).takeWhile(_ <= until).map(x => Parameters(axisName -> x))

    def generate(params: Parameters) = {
      (params[Int](axisName), new RedisBenchContextPool())
    }
  }

  val sizes = exponential("size")(20000, 400000, 2)

  performance of "RedisBench" in {

    measure method "ping" in {

      using(sizes).setUp(redisSetUp())
        .tearDown(redisTearDown)
        .in {
        case (i: Int, redisBench: RedisBenchContextPool) =>
          val redis = redisBench.redis
          implicit val ec = redis.executionContext

          val r = for {
            ii <- 0 until i
          } yield {
            redis.ping()
          }
          Await.result(Future.sequence(r), 30 seconds)
      }
    }

    measure method "set" in {

      using(sizes).setUp(redisSetUp())
        .tearDown(redisTearDown)
        .in {
        case (i: Int, redisBench: RedisBenchContextPool) =>
          val redis = redisBench.redis
          implicit val ec = redis.executionContext

          val r = for {
            ii <- 0 until i
          } yield {
            redis.set("a", ii)
          }
          Await.result(Future.sequence(r), 30 seconds)
      }
    }

    measure method "get" in {

      using(sizes).setUp(redisSetUp(_.set("a", "abc")))
        .tearDown(redisTearDown)
        .in {
        case (i: Int, redisBench: RedisBenchContextPool) =>
          val redis = redisBench.redis
          implicit val ec = redis.executionContext

          val r = for {
            ii <- 0 until i
          } yield {
            redis.get("a")
          }
          Await.result(Future.sequence(r), 30 seconds)
      }
    }

  }

  def redisSetUp(init: RedisClient => Unit = _ => {})(data: (Int, RedisBenchContextPool)) = data match {
    case (i: Int, redisBench: RedisBenchContextPool) => {
      redisBench.akkaSystem = akka.actor.ActorSystem()
      redisBench.redis = RedisClientPool(Seq(RedisServer(), RedisServer(), RedisServer()))(redisBench.akkaSystem)
      Await.result(redisBench.redis.ping(), 2 seconds)
    }
  }

  def redisTearDown(data: (Int, RedisBenchContextPool)) = data match {
    case (i: Int, redisBench: RedisBenchContextPool) =>
      redisBench.redis.stop()
      redisBench.akkaSystem.shutdown()
      redisBench.akkaSystem.awaitTermination()
  }
}

class RedisBenchContextPool(var redis: RedisClientPool = null, var akkaSystem: ActorSystem = null)