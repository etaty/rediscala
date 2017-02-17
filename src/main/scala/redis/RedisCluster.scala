package redis

import java.util.concurrent.{ThreadLocalRandom, TimeUnit}

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.util.ByteString
import redis.api.clusters.{ClusterNode, ClusterSlot}
import redis.commands.Transactions
import redis.protocol.RedisReply
import redis.util.CRC16

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.stm.Ref
import scala.concurrent.{Await, Future, Promise}
import scala.util.control.NonFatal


case class RedisCluster(redisServers: Seq[RedisServer],
                           name: String = "RedisClientPool",
                           password: Option[String] = None)
                          (implicit _system: ActorSystem,
                           redisDispatcher: RedisDispatcher = Redis.dispatcher
                          ) extends RedisClientPoolLike(_system, redisDispatcher)  with RedisCommands {

  val log = Logging.getLogger(_system, this)

  override val redisServerConnections = collection.mutable.Map {
    redisServers.map(makeConnection): _*
  }
  refreshConnections()

  def makeConnection(server: RedisServer) = {
    makeRedisConnection(
      server = server.copy(password = password, db = None),
      defaultActive = true
    )
  }

  def equalsHostPort(clusterNode:ClusterNode,server:RedisServer) = {
    clusterNode.host == server.host &&  clusterNode.port == server.port
  }

  override def onConnectStatus(server: RedisServer, active: Ref[Boolean]): (Boolean) => Unit = {
    (status: Boolean) => {
      if (active.single.compareAndSet(!status, status)) {
        refreshConnections()
      }
      
      clusterSlotsRef.single.get.map { clusterSlots =>
        if (clusterSlots.keys.exists( cs => equalsHostPort(cs.master,server) )){
          log.info("one master is still dead => refresh clusterSlots")
          asyncRefreshClusterSlots()
        }
      }

    }
  }

  val clusterSlotsRef:Ref[Option[Map[ClusterSlot, RedisConnection]]] = Ref(Option.empty[Map[ClusterSlot, RedisConnection]])
  val lockClusterSlots = Ref(true)
  Await.result(asyncRefreshClusterSlots(force=true), 10.seconds)

  def getClusterSlots(): Future[Map[ClusterSlot, RedisConnection]] = {

    def resolveClusterSlots(retry:Int): Future[Map[ClusterSlot, RedisConnection]] = {
      clusterSlots().map { clusterSlots =>
        clusterSlots.map { clusterSlot =>
          val server = clusterSlot.master.hostAndPort
          val connection = redisServerConnections
            .getOrElseUpdate(server, makeConnection(server)._2)
          (clusterSlot, connection)
        }.toMap
      }.recoverWith {
        case e =>
          if (retry-1 == 0){
            Future.failed(e)
          }else {
            resolveClusterSlots(retry - 1)
          }
      }
    }
    resolveClusterSlots(3) //retry 3 times
  }

  def asyncRefreshClusterSlots(force:Boolean=false): Future[Unit] = {
    if( force || lockClusterSlots.single.compareAndSet(false,true) ) {
     try {
       getClusterSlots().map { clusterSlot =>
         log.info("refreshClusterSlots: " + clusterSlot.toString())
         clusterSlotsRef.single.set(Some(clusterSlot))
         val serverSet = clusterSlot.keysIterator.map(_.master.hostAndPort).toSet
         redisServerConnections.keys.foreach { server =>
           if (!serverSet.contains(server)) {
             redisServerConnections.remove(server)
               .map(connection => _system.scheduler.scheduleOnce(1.second)(_system.stop(connection.actor)))
           }
         }
         lockClusterSlots.single.compareAndSet(true, false)
         ()
       }.recoverWith {
         case NonFatal(e) =>
           log.error("refreshClusterSlots:",e)
           lockClusterSlots.single.compareAndSet(true, false)
           Future.failed(e)
       }
     }catch{
       case NonFatal(e) =>
         lockClusterSlots.single.compareAndSet(true, false)
         throw e
     }
    }else{

     Future.successful(clusterSlotsRef.single.get)
    }
  }

  protected def send[T](redisConnection: ActorRef, redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    val promise = Promise[T]()
    redisConnection ! Operation(redisCommand, promise)
    promise.future
  }

  def getRedisConnection(slot:Int):Option[RedisConnection] = {
        getClusterAndConnection(slot)
          .map{ case ( _,redisConnection ) => redisConnection  }

  }

  def getClusterAndConnection(slot:Int): Option[(ClusterSlot, RedisConnection)] = {
    clusterSlotsRef.single.get.flatMap { clusterSlots =>
      clusterSlots
        .find { case (clusterSlot, _) =>
          val result = clusterSlot.begin <= slot && slot <= clusterSlot.end
          if (result) {
            log.debug(s"slot $slot => " + clusterSlot.master.toString)
          }
          result
        }
    }
  }

  val redirectMessagePattern = """(MOVED|ASK) \d+ (\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)""".r
  override def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {

    val maybeRedisActor:Option[ActorRef]  = getRedisActor(redisCommand)

    maybeRedisActor.map{ redisConnection =>
      send(redisConnection,redisCommand).recoverWith {
        case e: redis.actors.ReplyErrorException if e.message.startsWith("MOVED")||e.message.startsWith("ASK") =>
          e.message match {
              // folow the redirect
            case redirectMessagePattern(opt,host, port) =>
              log.debug("Redirect:" + e.message)

              if (opt == "MOVED") {
                redisCommand match {
                  case _: ClusterKey => asyncRefreshClusterSlots()
                  case _ => log.info(s"Command do not implement ClusterKey : ${redisCommand}")
                }
              }

              redisServerConnections.find { case (server, redisConnection) =>
                server.host== host && server.port.toString == port && redisConnection.active.single.get
              }.map { case (_, redisConnection) =>
                  send(redisConnection.actor, redisCommand)
              }.getOrElse(Future.failed(new Exception(s"server not found: $host:$port")))

            case _ => Future.failed(new Exception("bad exception format:" +e.message))
          }
        case error => Future.failed(error)
      }

    }.getOrElse(Future.failed(new RuntimeException("server not found: no server available")))
  }

  def getRedisActor[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Option[ActorRef] = {
    redisCommand match {
      case clusterKey: ClusterKey =>
        getRedisConnection(clusterKey.getSlot())
          .filter{_.active.single.get
          }.map(_.actor)
      case _ =>
        val redisActors = redisConnectionPool
        if (redisActors.nonEmpty) {
          //if it is not a cluster command => random connection
          //TODO use RoundRobinPoolRequest
          Some(redisActors(ThreadLocalRandom.current().nextInt(redisActors.length)))
        } else {
          None
        }
    }
  }

  def groupByCluserServer(keys:Seq[String]): Seq[Seq[String]] = {
    keys.groupBy{
      key => getRedisConnection(RedisComputeSlot.hashSlot(key))
    }.values.toSeq
  }

}


object RedisComputeSlot {
  val MAX_SLOTS = 16384

  def hashSlot(key:String) = {
    val indexBegin  = key.indexOf("{")
    val keytag=if (indexBegin != -1) {
      val indexEnd = key.indexOf("}",indexBegin)
      if (indexEnd != -1) {
        key.substring(indexBegin + 1, indexEnd)
      }else{
        key
      }
    }else{
      key
    }
    CRC16.crc16(keytag) % MAX_SLOTS
  }

}

trait ClusterKey{
  def getSlot():Int
}

object MultiClusterKey {
  def getHeadSlot[K](redisKey: ByteStringSerializer[K], keys:Seq[K]):Int =  {
    RedisComputeSlot.hashSlot(
      redisKey.serialize(keys.headOption.getOrElse(throw new RuntimeException("operation has not keys"))).utf8String)
  }
}

abstract class SimpleClusterKey[K](implicit redisKey: ByteStringSerializer[K]) extends ClusterKey{
  val key:K
  val keyAsString:ByteString = redisKey.serialize(key)
  def getSlot():Int = RedisComputeSlot.hashSlot(keyAsString.utf8String)
}

abstract class MultiClusterKey[K](implicit redisKey: ByteStringSerializer[K]) extends ClusterKey{
  val keys:Seq[K]
  def getSlot():Int = MultiClusterKey.getHeadSlot(redisKey,keys)
}

