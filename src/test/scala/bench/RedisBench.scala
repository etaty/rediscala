package bench

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor.ActorSystem
import scala.collection.Iterator

import org.scalameter._
import org.scalameter.api.{Executor,Aggregator,Gen,Reporter,RegressionReporter,HtmlReporter,SerializationPersistor}
import redis.RedisClient
import org.scalameter.execution

import org.scalameter.picklers.noPickler._

object RedisBench extends Bench[Double] {

  override def reporter: Reporter[Double] = Reporter.Composite(
    new RegressionReporter[Double](
      RegressionReporter.Tester.Accepter(),
      RegressionReporter.Historian.Complete()),
    HtmlReporter(embedDsv = true)
  )

  import Executor.Measurer

  def aggregator = Aggregator.average

  def measurer: Measurer[Double] = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation[Double] with Measurer.OutlierElimination[Double] with Measurer.RelativeNoise {
    def numeric: Numeric[Double] = implicitly[Numeric[Double]]
  }

  def executor: Executor[Double] = new execution.SeparateJvmsExecutor(warmer, aggregator, measurer)

  def persistor = new SerializationPersistor()

  def exponential(axisName: String)(from: Int, until: Int, factor: Int): Gen[(Int, RedisBenchContext)] = new Gen[(Int, RedisBenchContext)] {
    def warmupset = {
      Iterator.single(((until - from) / 2, new RedisBenchContext()))
    }

    def dataset = Iterator.iterate(from)(_ * factor).takeWhile(_ <= until).map(x => Parameters(new Parameter[String](axisName) -> x))

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
      redisBench.redis.stop()
      redisBench.akkaSystem.terminate()
      Await.result(redisBench.akkaSystem.whenTerminated, Duration.Inf)
  }
}

class RedisBenchContext(var redis: RedisClient = null, var akkaSystem: ActorSystem = null)
