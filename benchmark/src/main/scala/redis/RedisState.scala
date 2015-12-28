package redis

import org.openjdk.jmh.annotations.{Setup, Level, TearDown}

import scala.concurrent.Await

case class RedisState(initF: () => Unit = () => ()) {
  val akkaSystem = akka.actor.ActorSystem()
  val redis = RedisClient()(akkaSystem)

  implicit val exec = akkaSystem.dispatcher

  import scala.concurrent.duration._

  Await.result(redis.ping(), 2 seconds)

  @TearDown(Level.Trial)
  def down: Unit = {
    redis.stop()
    akkaSystem.shutdown
    akkaSystem.awaitTermination()
  }
}

trait RedisStateHelper {
  var rs: RedisState = _

  @Setup(Level.Trial)
  def up() = {
    rs = RedisState()
    initRedisState()
  }

  @TearDown(Level.Trial)
  def down() = {
    rs.down
  }

  def initRedisState(): Unit = {}
}
