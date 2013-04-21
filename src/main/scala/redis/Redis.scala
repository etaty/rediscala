package redis

import akka.actor._
import akka.io.Tcp._
import akka.io.Tcp
import akka.util.{ByteString, Timeout}
import java.net.{InetAddress, InetSocketAddress}
import redis.commands._
import scala.annotation.tailrec
import scala.collection.mutable
import akka.pattern.ask
import scala.concurrent._
import scala.util.Try
import akka.routing.RoundRobinRouter
import redis.protocol._
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import akka.io.Tcp.Connect
import scala.util.Failure
import akka.io.Tcp.CommandFailed
import scala.util.Success
import akka.io.Tcp.Received

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
   * @param c
   */
  def commandFailed(c: CommandFailed) {
    c.cmd match {
      case write@Write(_, NoAck(sender: ActorRef)) => {
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
  val redisClientRouteur: ActorRef = actorSystem.actorOf(Props(classOf[RedisClientActor], host, port).withDispatcher("rediscala.rediscala-client-worker-dispatcher").withRouter(RoundRobinRouter(5)))

  def this()(implicit actorSystem: ActorSystem) = this("localhost", 6379)

  def send(command: String, args: Seq[ByteString])(implicit timeout: Timeout): Future[Any] = {
    redisClientRouteur ? RedisProtocolRequest.multiBulk(command, args)
  }

  def send(command: String)(implicit timeout: Timeout): Future[Any] = {
    redisClientRouteur ? RedisProtocolRequest.inline(command)
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
        case i: Integer => Success(i.toString)
        case s: Status => Success(s.toString)
        case e: Error => Success(e.toString)
        case Bulk(b) => b.map(x => Success(x.utf8String)).getOrElse(Failure(new NoSuchElementException()))
        case MultiBulk(mb) => Failure(new NoSuchElementException()) // TODO find better ?
      }
    }

  }

}

