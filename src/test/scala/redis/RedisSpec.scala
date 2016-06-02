package redis

import java.io.{InputStream, OutputStream}
import java.net.Socket
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit

import org.specs2.mutable.{SpecificationLike, Tags}
import akka.util.Timeout
import org.specs2.time.NoTimeConversions
import akka.testkit.TestKit
import org.specs2.specification.{Fragments, Step}
import akka.actor.ActorSystem
import java.util.concurrent.atomic.AtomicInteger

import scala.io.Source
import scala.collection.JavaConversions._
import scala.util.Try
import scala.reflect.io.File
import scala.sys.process.{ProcessIO, _}

abstract class RedisHelper extends TestKit(ActorSystem()) with SpecificationLike with Tags with NoTimeConversions {

  import scala.concurrent.duration._

  implicit val executionContext = system.dispatchers.lookup(Redis.dispatcher.name)

  implicit val timeout = Timeout(10 seconds)
  val timeOut = 10 seconds
  val longTimeOut = 100 seconds



  // remove stacktrace when we stop the process
  val processLogger = ProcessLogger(line => println(line),line => Console.err.println(line))


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

  val redisServerPath= if ( Option(System.getenv("REDIS_HOME")).isDefined || System.getenv("REDIS_HOME") =="")
                          "/usr/local/bin"
                      else
                          System.getenv("REDIS_HOME")

  val redisServerCmd = s"$redisServerPath/redis-server"
  val redisServerLogLevel = ""
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
    val serverProcess = Process(s"$redisServerCmd --port $serverPort $redisServerLogLevel").run(processLogger)

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
    server = Process(s"$redisServerCmd --port $port $redisServerLogLevel").run(processLogger)
    // on faster machines, this can take some time to start up so wait a bit
    // before executing the first test against it
    Thread.sleep(3000)
  }

  override def cleanup() = {
    server.destroy()
  }
}




abstract class RedisSentinelClients(val masterName: String = "mymaster") extends RedisHelper {

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
  var processes: Seq[Process] = Seq.empty

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

  lazy val slave1 = Process(s"$redisServerCmd --port ${slavePort1} --slaveof $redisHost $masterPort $redisServerLogLevel").run(processLogger)
  lazy val slave2 = Process(s"$redisServerCmd --port ${slavePort2} --slaveof $redisHost $masterPort $redisServerLogLevel").run(processLogger)


  override def setup() = {


    processes =
        Seq(
          Process(s"$redisServerCmd --port $masterPort $redisServerLogLevel").run(processLogger),
          slave1,
          slave2
        ) ++
        sentinelPorts.map(p =>
          Process(s"$redisServerCmd $sentinelConfPath --port $p --sentinel $redisServerLogLevel").run(processLogger)
        )
    Thread.sleep(10000)
  }

  override def cleanup() = {
    processes.foreach(_.destroy())
    Thread.sleep(5000)
  }

  def newSentinelProcess() = {
    val port = portNumber.getAndIncrement()
    val sentinelProcess = Process(s"$redisServerCmd $sentinelConfPath --port $port --sentinel $redisServerLogLevel").run(processLogger)
    processes = processes :+ sentinelProcess
    sentinelProcess
  }


 def newSlaveProcess() = {
    val port = portNumber.getAndIncrement()
    val slaveProcess = Process(s"$redisServerCmd --port $port --slaveof $redisHost $masterPort $redisServerLogLevel").run(processLogger)
    processes = processes :+ slaveProcess
    slaveProcess
  }


}

abstract class RedisClusterClients() extends RedisHelper {

  import RedisServerHelper._


  var processes: Seq[Process] = Seq.empty
  val fileDir = new java.io.File("/tmp/redis"+System.currentTimeMillis())

  def newNode(port:Int) =
    s"$redisServerCmd --port $port --cluster-enabled yes --cluster-config-file nodes-${port}.conf --cluster-node-timeout 30000 --appendonly yes --appendfilename appendonly-${port}.aof --dbfilename dump-${port}.rdb --logfile ${port}.log --daemonize yes"

  val nodePorts = ((0 to 5).map(_ => portNumber.getAndIncrement()))
  override def setup() = {
    println("Setup")
    fileDir.mkdirs()

    processes = nodePorts.map(s => Process(newNode(s),fileDir).run(processLogger))
    Thread.sleep(2000)
    val nodes = nodePorts.map(s=>redisHost+":"+s).mkString(" ")

    println(s"$redisServerPath/redis-trib.rb create --replicas 1 ${nodes}")
    val redisTrib = Process(s"$redisServerPath/redis-trib.rb create --replicas 1 ${nodes}").run(
    
      new ProcessIO(
      (writeInput: OutputStream) => {
        //
        Thread.sleep(2000)
        println ("yes")
        writeInput.write("yes\n".getBytes)
        writeInput.flush
      },
    (processOutput: InputStream) =>{
      Source.fromInputStream(processOutput).getLines().foreach{l => println(l)}
    },
    (processError: InputStream) => {
      Source.fromInputStream(processError).getLines().foreach{l => println(l)}
    },
    daemonizeThreads=false
    )

    ).exitValue()
    Thread.sleep(5000)


  }

  override def cleanup() = {
    println("Stop begin")
    //cluster shutdown
    nodePorts.map { port =>
      val out = new Socket(redisHost, port).getOutputStream
      out.write("SHUTDOWN NOSAVE\n".getBytes)
      out.flush
    }
    Thread.sleep(6000)
      //Await.ready(RedisClient(port = port).shutdown(redis.api.NOSAVE),timeOut) }
    processes.foreach(_.destroy())

    //deleteDirectory()

    println("Stop end")
  }

  def deleteDirectory(): Unit = {
    val fileStream = Files.newDirectoryStream(fileDir.toPath)
    fileStream.iterator().toSeq.foreach(Files.delete)
    Files.delete(fileDir.toPath)
  }
}



object RedisServerHelper {
  val portNumber = new AtomicInteger(10500)
}
