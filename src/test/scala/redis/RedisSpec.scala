package redis

import org.specs2.mutable.{SpecificationLike, Tags}
import akka.util.Timeout
import org.specs2.time.NoTimeConversions
import akka.testkit.TestKit
import org.specs2.specification.{Step, Fragments}
import akka.actor.ActorSystem
import java.util.concurrent.atomic.AtomicInteger
import scala.sys.process.Process
import scala.util.Try
import scala.reflect.io.Path

abstract class RedisSpec extends TestKit(ActorSystem()) with SpecificationLike with Tags with NoTimeConversions {

  import scala.concurrent._
  import scala.concurrent.duration._

  implicit val ec = ExecutionContext.Implicits.global

  implicit val timeout = Timeout(10 seconds)
  val timeOut = 10 seconds
  val longTimeOut = 100 seconds
  val redis = RedisClient()
  val masterName = "mymaster"
  lazy val sentinel = SentinelClient()
  lazy val smRedis = SentinelMonitoredRedisClient(master = masterName)

  override def map(fs: => Fragments) = fs ^ Step(system.shutdown())

  def withRedisCluster[T](block: (Int, Int, Int) => T): T = {
    val masterPort = RedisServerHelper.portNumber.getAndIncrement()
    val slavePort = RedisServerHelper.portNumber.getAndIncrement()
    val sentinelPort = RedisServerHelper.portNumber.getAndIncrement()
    val sentinelConf =
        s"port $sentinelPort\n" +
        s"sentinel monitor $masterName 127.0.0.1 $masterPort 1\n" +
        s"sentinel down-after-milliseconds $masterName 5000\n" +
        s"sentinel can-failover $masterName yes \n" +
        s"sentinel parallel-syncs $masterName 1 \n" +
        s"sentinel failover-timeout $masterName 10000"
    val sentinelConfPath = "/tmp/rediscala-sentinel.conf"
    Path(sentinelConfPath).toFile.writeAll(sentinelConf)

    val redis_server = "redis-server"
    val log_level = "--loglevel verbose"
    val master = Process(s"$redis_server --port $masterPort $log_level").run()
    val slave = Process(s"$redis_server --port $slavePort --slaveof 127.0.0.1 $masterPort $log_level").run()
    val sentinel = Process(s"$redis_server $sentinelConfPath --sentinel $log_level").run()

    val result = Try(block(masterPort, slavePort, sentinelPort))

    sentinel.destroy()
    master.destroy()
    slave.destroy()

    result.get
  }

  def withRedisClusterAndClients[T](block: (RedisClient, SentinelClient, SentinelMonitoredRedisClient) => T): T = {
    withRedisCluster((masterPort, slavePort, sentinelPort) => {
      val redis = RedisClient(port = masterPort)
      val sentinel = SentinelClient(port = sentinelPort)
      val smRedis = SentinelMonitoredRedisClient(
                        sentinelPort = sentinelPort,
                        master = masterName)

      val result = Try(block(redis, sentinel, smRedis))

      redis.disconnect()
      sentinel.disconnect()
      smRedis.redisClient().disconnect()

      result.get
    })
  }
}

object RedisServerHelper {
  val portNumber = new AtomicInteger(10500)
}