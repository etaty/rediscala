package redis

import akka.actor._
import akka.io.Tcp._
import akka.io.Tcp.CommandFailed
import akka.io.Tcp.Connect
import akka.io.Tcp.Connected
import akka.io.Tcp.Received
import akka.io.Tcp.Register
import akka.io.Tcp
import akka.util.{ByteString, Timeout}
import java.net.{InetAddress, InetSocketAddress}
import redis.commands.{Keys, Connection, Strings}
import scala.annotation.tailrec
import scala.collection.mutable
import akka.pattern.ask
import scala.concurrent._
import scala.util.{Failure, Success, Try}

class RedisClientActor(host: String, port: Int) extends Actor with Stash {

  import context._

  val tcp = akka.io.IO(Tcp)(context.system)

  var tcpWorker: ActorRef = null
  tcp ! Connect(new InetSocketAddress(InetAddress.getByName(host), port))

  val queue = mutable.Queue[ActorRef]()

  var buffer: ByteString = ByteString.empty

  override def postStop() {
    tcpWorker ! Close
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

  @tailrec
  private def decodeReplies(bs: ByteString): ByteString = {
    val r = RedisProtocolReply.decodeReply(bs)
    if (r.nonEmpty) {
      val result = r.get._1 match {
        case Error(error) => akka.actor.Status.Failure(new RuntimeException(error.toString())) // TODO runtime ???
        case _ => r.get._1
      }
      queue.dequeue() ! result
      decodeReplies(r.get._2)
    } else {
      bs
    }
  }

  def connected: Receive = {
    case Received(dataByteString) => {
      buffer = decodeReplies(buffer ++ dataByteString).compact
    }
    case c: CommandFailed => commandFailed(c) // O/S buffer was full
    case c: ConnectionClosed => println("connection close " + c)
    case write: ByteString => {
      tcpWorker ! Write(write, ack = true)
      queue enqueue (sender)
    }
    case _ =>
    //case wtf => print("wtf : " + wtf)
  }

  /**
   * Send back the commandFailed
   *@param c
   */
  def commandFailed(c: CommandFailed) {
    c.cmd match {
      case write @ Write(_, NoAck(sender: ActorRef)) => {
        //queue.dequeueFirst(_ == sender)
        //sender ! akka.actor.Status.Failure(new RuntimeException(c.toString))
        tcpWorker ! write
      }
    }
    println("CommandFailed : " + c)
  }

}

trait RedisCommand

class RedisClient(val host: String, val port: Int)(implicit actorSystem: ActorSystem) extends RedisCommand
with Strings
with Connection
with Keys {
  val redisClientActor: ActorRef = actorSystem.actorOf(Props(classOf[RedisClientActor], host, port).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

  def this()(implicit actorSystem: ActorSystem) = this("localhost", 6379)

  def send(command: String, args: Seq[ByteString])(implicit timeout: Timeout): Future[Any] = {
    redisClientActor ? multiBulk(command, args)
  }

  def send(command: String)(implicit timeout: Timeout): Future[Any] = {
    redisClientActor ? (ByteString(command) ++ RedisProtocolReply.LS)
  }

  def multiBulk(command: String, args: Seq[ByteString]): ByteString = {
    val requestBuilder = ByteString.newBuilder
    requestBuilder.putBytes((s"*${args.size + 1}").getBytes("UTF-8"))
    requestBuilder.putBytes(RedisProtocolReply.LS)

    val builder = (ByteString(command.getBytes("UTF-8")) +: args).foldLeft(requestBuilder) {
      case (acc, arg) =>
        acc.putBytes((s"$$${arg.size}").getBytes("UTF-8"))
        acc.putBytes(RedisProtocolReply.LS)
        acc ++= (arg)
        acc.putBytes(RedisProtocolReply.LS)

        acc
    }

    builder.result()
  }

}

trait RedisValue

trait RedisValueConvert[A] {
  def from(a: A): ByteString
}

trait RedisReplyConvert[A] {
  def to(redisReply: RedisReply): Try[A]
}

object Redis {

  object Convert {

    implicit object StringConvert extends RedisValueConvert[String] {
      def from(s: String): ByteString = ByteString(s)
    }

    implicit object StringReplyConvert extends RedisReplyConvert[String] {
      def to(reply: RedisReply) = reply match {
        case Integer(x) => Success(x.utf8String)
        case Status(s) => Success(s.utf8String)
        case Error(e) => Success(e.utf8String)
        case Bulk(b) => b.map(x => Success(x.utf8String)).getOrElse(Failure(new NoSuchElementException()))
        case MultiBulk(mb) => Failure(new NoSuchElementException()) // TODO find better ?
      }
    }

  }

}

