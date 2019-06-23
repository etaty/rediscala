package redis.benchold

import scala.compat.Platform
import scala.concurrent._
import scala.concurrent.duration._
import redis.{RedisStandaloneServer}
import akka.actor.{Props, ActorRef}
import java.net.InetSocketAddress
import akka.io.Tcp._
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import akka.io.Tcp.CommandFailed
import akka.util.{ByteStringBuilder, ByteString}
import akka.pattern.ask
import redis.actors.{WriteAck, RedisClientActor}

/*
class RedisBenchActor extends RedisStandaloneServer {


  "Rediscala stupid benchmark" should {
    "bench 1" in {
      val n = 100000
      for (i <- 1 to 10) yield {
        val ops = n //* i / 10

        val redisConnection: ActorRef = system.actorOf(Props(classOf[RedisClientActorBench], new InetSocketAddress("localhost", 6379)).withDispatcher(Redis.dispatcher))

        //redisConnection ! new InetSocketAddress("localhost", 6379)

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

  def timed(desc: String, n: Int)(benchmark: => Unit) {
    println("* " + desc)
    val start = System.currentTimeMillis
    benchmark
    val stop = System.currentTimeMillis
    val elapsedSeconds = (stop - start) / 1000.0
    val opsPerSec = n / elapsedSeconds

    println(s"* - number of ops/s: $opsPerSec ( $n ops in $elapsedSeconds)")
  }
}

class RedisClientActorBench(addr: InetSocketAddress) extends RedisClientActor(addr) {
  var receivedData = 0

  override def receive = {
    case Received(dataByteString) => {
      //println(dataByteString.utf8String)
      receivedData = receivedData - dataByteString.length

      //if (receivedData <= 0)
        //queue.dequeue() ! "PONG"
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
      //queue enqueue (sender)
    }
    case c: CommandFailed => log.error(c.toString) // TODO failed connection
    case e => log.error(e.toString)
  }

  def crazyWrite(i: Int) {
    receivedData = ("+PONG\r\n".length) * i

    val bs = ByteString("PING\r\n")
    val ii = i / 10
    for (_ <- 1 to 10) {
      val bufferWrite: ByteStringBuilder = new ByteStringBuilder
      for (_ <- 1 to ii) {
        bufferWrite.append(bs)
      }
      val write = bufferWrite.result() //ByteString("PING\r\n" * ii)

      tcpWorker ! Write(write, WriteAck)
    }
  }

}
*/