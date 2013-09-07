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
import scala.reflect.io.File


abstract class RedisHelper extends TestKit(ActorSystem()) with SpecificationLike with Tags with NoTimeConversions {

  import scala.concurrent.duration._

  implicit val executionContext = system.dispatcher

  implicit val timeout = Timeout(10 seconds)
  val timeOut = 10 seconds
  val longTimeOut = 100 seconds

  override def map(fs: => Fragments) = {
    Step(setup()) ^
      fs ^
      Step({
        system.shutdown()
        cleanup()
      })
  }

  def setup() = {}

  def cleanup() = {}

  val redisServerCmd = "redis-server"
  val redisServerLogLevel = "--loglevel verbose"
}

abstract class RedisSpec extends RedisHelper {

  val redis = RedisClient()

  def withRedisServer[T](block: (Int) => T): T = {
    val serverPort = RedisServerHelper.portNumber.getAndIncrement()
    val serverProcess = Process(s"$redisServerCmd --port $serverPort $redisServerLogLevel").run()

    val result = Try(block(serverPort))

    serverProcess.destroy()

    result.get
  }
}


abstract class RedisClusterClients(val masterName: String = "mymaster") extends RedisHelper {

  import RedisServerHelper._

  val masterPort = portNumber.getAndIncrement()
  val slavePort = portNumber.getAndIncrement()
  val sentinelPort = portNumber.getAndIncrement()

  lazy val redisClient = RedisClient(port = masterPort)
  lazy val sentinelClient = SentinelClient(port = sentinelPort)
  lazy val sentinelMonitoredRedisClient = SentinelMonitoredRedisClient(sentinelPort = sentinelPort, master = masterName)

  var master: Process = null
  var slave: Process = null
  var sentinel: Process = null

  override def setup() = {
    val sentinelConf =
      s"""
        |port $sentinelPort
        |sentinel monitor $masterName 127.0.0.1 $masterPort 1
        |sentinel down-after-milliseconds $masterName 5000
        |sentinel can-failover $masterName yes
        |sentinel parallel-syncs $masterName 1
        |sentinel failover-timeout $masterName 10000
      """.stripMargin

    val sentinelConfFile = File.makeTemp("rediscala-sentinel", ".conf")
    sentinelConfFile.writeAll(sentinelConf)
    val sentinelConfPath = sentinelConfFile.path

    master = Process(s"$redisServerCmd --port $masterPort $redisServerLogLevel").run()
    slave = Process(s"$redisServerCmd --port $slavePort --slaveof 127.0.0.1 $masterPort $redisServerLogLevel").run()
    sentinel = Process(s"$redisServerCmd $sentinelConfPath --sentinel $redisServerLogLevel").run()
  }

  override def cleanup() = {
    sentinel.destroy()
    slave.destroy()
    master.destroy()
  }
}

object RedisServerHelper {
  val portNumber = new AtomicInteger(10500)
}
