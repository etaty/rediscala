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
import redis.RedisClient
import org.scalameter.execution

object RedisBench extends PerformanceTest {

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

  def exponential(axisName: String)(from: Int, until: Int, factor: Int): Gen[(Int, RedisBenchContext)] = new Gen[(Int, RedisBenchContext)] {
    def warmupset = {
      Iterator.single(((until - from) / 2, new RedisBenchContext()))
    }

    def dataset = Iterator.iterate(from)(_ * factor).takeWhile(_ <= until).map(x => Parameters(axisName -> x))

    def generate(params: Parameters) = {
      (params[Int](axisName), new RedisBenchContext())
    }
  }

  val sizes = exponential("size")(20000, 400000, 2)

  performance of "RedisBench" in {

    measure method "ping" in {

      using(sizes).setUp(redisSetUp())
        .tearDown(redisTearDown)
        .in {
        case (i: Int, redisBench: RedisBenchContext) =>
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
        case (i: Int, redisBench: RedisBenchContext) =>
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
        case (i: Int, redisBench: RedisBenchContext) =>
          val redis = redisBench.redis
          implicit val ec = redis.executionContext

          val r = for {
            ii <- 0 until i
          } yield {
            redis.get("i")
          }
          Await.result(Future.sequence(r), 30 seconds)
      }
    }

  }

  def redisSetUp(init: RedisClient => Unit = _ => {})(data: (Int, RedisBenchContext)) = data match {
    case (i: Int, redisBench: RedisBenchContext) => {
      redisBench.akkaSystem = akka.actor.ActorSystem()
      redisBench.redis = RedisClient()(redisBench.akkaSystem)
      Await.result(redisBench.redis.ping(), 2 seconds)
    }
  }

  def redisTearDown(data: (Int, RedisBenchContext)) = data match {
    case (i: Int, redisBench: RedisBenchContext) =>
      redisBench.redis.disconnect()
      redisBench.akkaSystem.shutdown()
      redisBench.akkaSystem.awaitTermination()
  }
}

class RedisBenchContext(var redis: RedisClient = null, var akkaSystem: ActorSystem = null)

/*
https://github.com/axel22/scalameter/pull/17
 */
case class FixDefault() extends Warmer {
  def name = "Warmer.FixDefault"

  def warming(ctx: Context, setup: () => Any, teardown: () => Any) = new Foreach[Int] {
    val minwarmups = ctx.goe(exec.minWarmupRuns, 10)
    val maxwarmups = ctx.goe(exec.maxWarmupRuns, 50)
    val covThreshold = ctx.goe(exec.warmupCovThreshold, 0.1)

    def foreach[U](f: Int => U): Unit = {
      val withgc = new utils.SlidingWindow(minwarmups)
      val withoutgc = new utils.SlidingWindow(minwarmups)
      @volatile var nogc = true

      log.verbose(s"Starting warmup.")

      utils.withGCNotification {
        n =>
          nogc = false
          log.verbose("GC detected.")
      } apply {
        var i = 0
        while (i < maxwarmups) {

          setup()
          nogc = true
          val start = System.nanoTime
          f(i)
          val end = System.nanoTime
          val runningtime = (end - start) / 1000000.0

          if (nogc) withoutgc.add(runningtime)
          withgc.add(runningtime)
          teardown()

          val covNoGC = withoutgc.cov
          val covGC = withgc.cov

          log.verbose(f"$i. warmup run running time: $runningtime (covNoGC: ${covNoGC}%.4f, covGC: ${covGC}%.4f)")
          if ((withoutgc.size >= minwarmups && covNoGC < covThreshold) || (withgc.size >= minwarmups && covGC < covThreshold)) {
            log.verbose(s"Steady-state detected.")
            i = maxwarmups
          } else i += 1
        }
        log.verbose(s"Ending warmup.")
      }
    }
  }
}