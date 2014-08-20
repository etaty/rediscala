package redis.commands 

import akka.actor._
import akka.util.ByteString
import redis._
import redis.api.connection.Ping
 
 
import scala.collection.immutable.Queue
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
import redis.protocol.RedisReply
 

trait Pipeline {
  def pipeline(): PipeLineBuilder
}
 
trait PipelineBase extends Pipeline {
  this:ActorRequest =>
  def pipeline(): PipeLineBuilder = PipeLineBuilder(redisConnection,None)
}
 
trait PipelinePool extends Pipeline {
  this:RoundRobinPoolRequest =>
  def pipeline(): PipeLineBuilder = PipeLineBuilder(getNextConnection,None)
}
 
trait PipelineMasterSlaves extends Pipeline {
  this:RedisClientMasterSlaves =>
  def pipeline(): PipeLineBuilder =  pipeline(false)
  def pipeline(isMasterOnly:Boolean): PipeLineBuilder = {
      if (isMasterOnly) {
        PipeLineBuilder(masterClient.redisConnection,Some(true))
      }else{
        PipeLineBuilder(slavesClients.getNextConnection,Some(false))
      }
  }
}
 
case class PipeLineBuilder(redisConnection: ActorRef,isMasterOnly:Option[Boolean])(implicit val executionContext: ExecutionContext) extends BufferedRequest with RedisCommands {
 
  def discard() {
    operations.result().map(operation => {
      operation.completeFailed(PipeLineDiscardedException)
    })
    operations.clear()
  }
 


  override def send[T](redisCommand : redis.RedisCommand[_ <: redis.protocol.RedisReply, T]) : scala.concurrent.Future[T] = {
    isMasterOnly.fold(super.send[T](redisCommand)){ authorize =>
       if ( redisCommand.isMasterOnly && !authorize ) {
          throw new IllegalArgumentException(s"command slave only: $redisCommand is master only ");
       }else{
          super.send[T](redisCommand)    
       }
    }
  }
 

  // todo maybe return a Future for the general state of the transaction ? (Success or Failure)
  def exec(): Future[Unit] = {
    val t = PipeLineExec(operations.result(), redisConnection)
    val p = Promise[Unit]()
    t.process(p)
    p.future
  }
 
}
 
case class PipeLineExec(operations: Queue[Operation[_, _]], redisConnection: ActorRef)(implicit val executionContext: ExecutionContext) {
 
  def process(promise: Promise[Unit]) {
    val commands = Seq.newBuilder[Operation[_, _]]
    commands ++= operations
    redisConnection ! redis.Transaction(commands.result())
  }
 
  def execPromise(promise: Promise[Unit]): Promise[String] = {
    val p = Promise[String]()
    p.future.onComplete{
        case Success(s) =>  promise.success()
        case Failure(e) =>  promise.failure(e)
    }
    p
  }
 
}
 
case object PipeLineDiscardedException  extends Exception