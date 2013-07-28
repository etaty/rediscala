package redis.actors

import akka.util.{ByteString, ByteStringBuilder}
import redis.protocol.RedisReply
import java.net.InetSocketAddress
import redis.{Transaction, Operation}
import scala.concurrent.Promise
import akka.actor.{Kill, Props}
import scala.collection.mutable

class RedisClientActor(override val address: InetSocketAddress) extends RedisWorkerIO {

  import context._

  var repliesDecoder = initRepliesDecoder

  private def initRepliesDecoder = system.actorOf(Props(classOf[RedisReplyDecoder]).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

  var queuePromises = mutable.Queue[Promise[RedisReply]]()

  def writing: Receive = {
    case Operation(request, promise) =>
      queuePromises enqueue promise
      write(request)
    case Transaction(commands) => {
      val buffer = new ByteStringBuilder
      commands.foreach(operation => {
        buffer.append(operation.request)
        queuePromises enqueue operation.promise
      })
      write(buffer.result())
    }
  }

  def onDataReceived(dataByteString: ByteString) {
    repliesDecoder ! dataByteString
  }

  def onWriteSent() {
    repliesDecoder ! queuePromises
    queuePromises = mutable.Queue[Promise[RedisReply]]()
  }

  def onConnectionClosed() {
    queuePromises.foreach(promise => {
      promise.failure(NoConnectionException)
    })
    queuePromises.clear()
    repliesDecoder ! Kill
    repliesDecoder = initRepliesDecoder
  }
}
