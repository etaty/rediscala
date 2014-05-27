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
  val redisHost = "127.0.0.1"
}

abstract class RedisSpec extends RedisHelper with WithRedisServerLauncher {

  val redis = RedisClient()
}

trait WithRedisServerLauncher extends RedisHelper {
  def withRedisServer[T](block: (Int) => T): T = {
    val serverPort = RedisServerHelper.portNumber.getAndIncrement()
    val serverProcess = Process(s"$redisServerCmd --port $serverPort $redisServerLogLevel").run()

    val result = Try(block(serverPort))

    serverProcess.destroy()

    result.get
  }
}

abstract class RedisStandaloneServer extends RedisHelper with WithRedisServerLauncher {

  import RedisServerHelper._

  val port = portNumber.getAndIncrement()

  lazy val redis = RedisClient(port = port)

  var server: Process = null

  override def setup() = {
    server = Process(s"$redisServerCmd --port $port $redisServerLogLevel").run()
  }

  override def cleanup() = {
    server.destroy()
  }
}


abstract class RedisClusterClients(val masterName: String = "mymaster") extends RedisHelper {

  import RedisServerHelper._

  val masterPort = portNumber.getAndIncrement()
  val slavePort = portNumber.getAndIncrement()
  val sentinelPorts = Seq(portNumber.getAndIncrement(),portNumber.getAndIncrement())

  lazy val redisClient = RedisClient(port = masterPort)
  lazy val sentinelClient = SentinelClient(port = sentinelPorts.head)
  lazy val sentinelMonitoredRedisClient =
      SentinelMonitoredRedisClient(master = masterName,
                                   sentinels = sentinelPorts.map((redisHost, _)))
  var processes: Seq[Process] = null

  lazy val sentinelConfPath = {
      val sentinelConf =
            s"""
              |sentinel monitor $masterName $redisHost $masterPort 2
              |sentinel down-after-milliseconds $masterName 5000
              |sentinel parallel-syncs $masterName 1
              |sentinel failover-timeout $masterName 10000
            """.stripMargin

      val sentinelConfFile = File.makeTemp("rediscala-sentinel", ".conf")
      sentinelConfFile.writeAll(sentinelConf)
      sentinelConfFile.path
    }

  override def setup() = {
    processes =
        Seq(
          Process(s"$redisServerCmd --port $masterPort $redisServerLogLevel").run(),
          Process(s"$redisServerCmd --port $slavePort --slaveof $redisHost $masterPort $redisServerLogLevel").run()
        ) ++
        sentinelPorts.map(p =>
          Process(s"$redisServerCmd $sentinelConfPath --port $p --sentinel $redisServerLogLevel").run()
        )
  }

  override def cleanup() = {
    processes.foreach(_.destroy())
  }

  def newSentinelProcess() = {
    val port = portNumber.getAndIncrement()
    val sentinelProcess = Process(s"$redisServerCmd $sentinelConfPath --port $port --sentinel $redisServerLogLevel").run()
    processes = processes :+ sentinelProcess
    sentinelProcess
  }

}

object RedisServerHelper {
  val portNumber = new AtomicInteger(10500)
}
