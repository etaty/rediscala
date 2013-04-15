package redis

import akka.actor._
import akka.io.Tcp._
import akka.io.Tcp.CommandFailed
import akka.io.Tcp.Connect
import akka.io.Tcp.Connected
import akka.io.Tcp.Received
import akka.io.Tcp.Register
import akka.io.Tcp
import akka.util.ByteString
import java.net.{InetAddress, InetSocketAddress}
import redis.commands.{Keys, Connection, Strings}
import scala.annotation.tailrec
import scala.collection.mutable
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent._

class RedisClientActor(host: String, port: Int) extends Actor with Stash {

  import context._

  val tcp = akka.io.IO(Tcp)(context.system)

  var tcpWorker: ActorRef = null
  tcp ! Connect(new InetSocketAddress(InetAddress.getByName(host), port))

  val queue = mutable.Queue[ActorRef]()

  var buffer: ByteString = ByteString()

  override def postStop() {
    tcp ! Close
  }

  def receive = {
    case Connected(remoteAddr, localAddr) => {
      println("connected")
      sender ! Register(self)
      tcpWorker = sender
      become(connected)
      unstashAll()
    }
    case _ => stash()
  }

  def connected: Receive = {
    case Received(dataByteString) => {
      @tailrec
      def decodeReplies(bs: ByteString): ByteString = {
        val r = RedisProtocolReply.decodeReply(bs)
        if (r.nonEmpty) {
          val result = r.get._1 match {
            case Error(error) => akka.actor.Status.Failure(new RuntimeException(error)) // TODO runtime ???
            case _ => r.get._1
          }
          queue.dequeue() ! result
          decodeReplies(r.get._2)
        } else {
          bs
        }
      }
      buffer = decodeReplies(buffer ++ dataByteString).compact
    }
    case CommandFailed(cmd) => println("failed" + cmd)
    case c: ConnectionClosed => println("connection close " + c)
    case write: Write => {
      tcpWorker ! write
      queue enqueue (sender)
    }
    case wtf => print("wtf : " + wtf)
  }

}

trait RedisCommand

class RedisClient(val host: String, val port: Int)(implicit actorSystem: ActorSystem) extends RedisCommand
with Strings
with Connection
with Keys {
  val redisClientActor: ActorRef = actorSystem.actorOf(Props(new RedisClientActor(host, port)).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

  def this()(implicit actorSystem: ActorSystem) = this("localhost", 6379)

  def send(command: String, args: Seq[ByteString])(implicit timeout: Timeout): Future[Any] = {
    (redisClientActor ? Write(multiBulk(command, args)))
  }

  def send(command: String)(implicit timeout: Timeout): Future[Any] = {
    (redisClientActor ? Write(ByteString(command)  ++ RedisProtocolReply.LS))
  }

  def multiBulk(command: String, args: Seq[ByteString]): ByteString = {
    val requestBuilder = ByteString.newBuilder
    requestBuilder.putBytes(("*" + (args.size + 1)).getBytes("UTF-8"))
    requestBuilder.putBytes(RedisProtocolReply.LS)

    val builder = (ByteString(command.getBytes("UTF-8")) +: args).foldLeft(requestBuilder) {
      case (acc, arg) =>
        acc.putBytes(("$" + (arg.size)).getBytes("UTF-8"))
        acc.putBytes(RedisProtocolReply.LS)
        acc ++= (arg)
        acc.putBytes(RedisProtocolReply.LS)

        acc
    }

    builder.result()
  }

}
