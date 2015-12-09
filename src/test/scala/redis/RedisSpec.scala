package redis

import org.specs2.mutable.{SpecificationLike, Tags}
import akka.util.Timeout
import org.specs2.time.NoTimeConversions
import akka.testkit.TestKit
import org.specs2.specification.{Step, Fragments}
import akka.actor.ActorSystem
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Try
import scala.reflect.io.File
import scala.sys.process._

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
  //val redisServerLogLevel = ""
  val redisHost = "127.0.0.1"
}

abstract class RedisSpec extends RedisHelper with WithRedisServerLauncher {

  val redis = RedisClient()
  redis.flushdb()
}

trait WithRedisServerLauncher extends RedisHelper {
  def withRedisServer[T](block: (Int) => T): T = {

    val buffer = new StringBuffer()
    val serverPort = RedisServerHelper.portNumber.getAndIncrement()
    val serverProcess = Process(s"$redisServerCmd --port $serverPort $redisServerLogLevel").run()

    Thread.sleep(3000) // wait for server start
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
    // on faster machines, this can take some time to start up so wait a bit
    // before executing the first test against it
    Thread.sleep(3000)
  }

  override def cleanup() = {
    server.destroy()
  }
}




abstract class RedisClusterClients(val masterName: String = "mymaster") extends RedisHelper {

  import RedisServerHelper._

  val masterPort = portNumber.getAndIncrement()
  val slavePort1 = portNumber.getAndIncrement()
  val slavePort2 = portNumber.getAndIncrement()
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

  lazy val slave1 = Process(s"$redisServerCmd --port ${slavePort1} --slaveof $redisHost $masterPort $redisServerLogLevel").run()
  lazy val slave2 = Process(s"$redisServerCmd --port ${slavePort2} --slaveof $redisHost $masterPort $redisServerLogLevel").run()


  override def setup() = {


    processes =
        Seq(
          Process(s"$redisServerCmd --port $masterPort $redisServerLogLevel").run(),
          slave1,
          slave2
        ) ++
        sentinelPorts.map(p =>
          Process(s"$redisServerCmd $sentinelConfPath --port $p --sentinel $redisServerLogLevel").run()
        )
  }

  override def cleanup() = {
    processes.foreach(_.destroy())
    Thread.sleep(5000)
  }

  def newSentinelProcess() = {
    val port = portNumber.getAndIncrement()
    val sentinelProcess = Process(s"$redisServerCmd $sentinelConfPath --port $port --sentinel $redisServerLogLevel").run()
    processes = processes :+ sentinelProcess
    sentinelProcess
  }


 def newSlaveProcess() = {
    val port = portNumber.getAndIncrement()
    val slaveProcess = Process(s"$redisServerCmd --port $port --slaveof $redisHost $masterPort $redisServerLogLevel").run()
    processes = processes :+ slaveProcess
    slaveProcess
  }


}

object RedisServerHelper {
  val portNumber = new AtomicInteger(10500)
}
