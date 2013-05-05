package redis.bench

import scala.compat.Platform
import scala.concurrent._
import scala.concurrent.duration._
import redis.{WriteAck, RedisClientActor, RedisSpec}
import akka.actor.{Props, ActorRef}
import java.net.InetSocketAddress
import akka.io.Tcp._
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import akka.util.ByteString
import akka.pattern.ask


class RedisBenchActor extends RedisSpec {


  "Rediscala stupid benchmark" should {
    "bench 1" in {
      val n = 100000
      for (i <- 1 to 1) yield {
        val ops = n //* i / 10

        val redisConnection: ActorRef = system.actorOf(Props(classOf[RedisClientActorBench]).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

        redisConnection ! new InetSocketAddress("localhost", 6379)

        Thread.sleep(2000)

        timed(s"ping $ops times (run $i)", ops) {
          val f = redisConnection ? ops
          Await.result(f, FiniteDuration(30, "s"))
        }
        Platform.collectGarbage()

      }
      true mustEqual true // TODO remove that hack for spec2

    } tag ("benchmark")
  }

  def timed(desc: String, n: Int)(benchmark: ⇒ Unit) {
    println("* " + desc)
    val start = System.currentTimeMillis
    benchmark
    val stop = System.currentTimeMillis
    val elapsedSeconds = (stop - start) / 1000.0
    val opsPerSec = n / elapsedSeconds

    println(s"* - number of ops/s: $opsPerSec ( $n ops in $elapsedSeconds)")
  }
}

class RedisClientActorBench extends RedisClientActor {
  var receivedData = 0;

  override def receive = {
    case Received(dataByteString) => {
      //println(dataByteString.utf8String)
      receivedData = receivedData - dataByteString.length

      if (receivedData <= 0)
        queue.dequeue() ! "PONG"
    }
    case address: InetSocketAddress => {
      log.info(s"Connect to $address")
      tcp ! Connect(address)
    }
    case Connected(remoteAddr, localAddr) => {
      initConnectedBuffer()
      sender ! Register(self)
      tcpWorker = sender
      log.debug("Connected to " + remoteAddr)
    }
    case crazyPing: Int => {
      crazyWrite(crazyPing)
      queue enqueue (sender)
    }
    case c: CommandFailed => log.error(c.toString) // TODO failed connection
    case e => log.error(e.toString)
  }

  def crazyWrite(i: Int) {
    receivedData = ("+PONG\r\n".length) * i

    val ii = i / 100
    for (_ <- 1 to 100) {
      val write = ByteString("PING\r\n" * ii)

      tcpWorker ! Write(write, WriteAck)
    }
  }

}
