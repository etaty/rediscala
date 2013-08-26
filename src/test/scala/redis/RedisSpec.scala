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
  import RedisServerHelper._

  implicit val ec = ExecutionContext.Implicits.global

  implicit val timeout = Timeout(10 seconds)
  val timeOut = 10 seconds
  val longTimeOut = 100 seconds
  val redis = RedisClient()
  lazy val clRedis = SentinelClient(port = masterPort)
  lazy val clSentinel = SentinelClient(port = sentinelPort)
  lazy val clSmRedis = SentinelMonitoredRedisClient(sentinelPort = sentinelPort, master = masterName)

  override def map(fs: => Fragments) = fs ^ Step(system.shutdown())

  def withRedisCluster[T](block: (Int, Int, Int) => T): T = {
    Try(block(masterPort, slavePort, sentinelPort)).get
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
      smRedis.disconnect()

      result.get
    })
  }
}

object RedisServerHelper {
  val portNumber = new AtomicInteger(10500)
  val masterPort = portNumber.getAndIncrement()
  val slavePort = portNumber.getAndIncrement()
  val sentinelPort = portNumber.getAndIncrement()

  var standAloneMaster: Process = null
  var master: Process = null
  var slave: Process = null
  var sentinel: Process = null

  val masterName = "mymaster"

  def setup () = {
    println("RedisTest.setup()")
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
    standAloneMaster = Process(s"$redis_server --port 6379 $log_level").run()
    master = Process(s"$redis_server --port $masterPort $log_level").run()
    slave = Process(s"$redis_server --port $slavePort --slaveof 127.0.0.1 $masterPort $log_level").run()
    sentinel = Process(s"$redis_server $sentinelConfPath --sentinel $log_level").run()

    val wait = 5000
    println(s"Waiting ${wait}ms for cluster to sync up...")
    Thread.sleep(wait)
  }
  def cleanup ()= {
    println("RedisTest.cleanup()")
    standAloneMaster.destroy()
    sentinel.destroy()
    slave.destroy()
    master.destroy()
  }
}
