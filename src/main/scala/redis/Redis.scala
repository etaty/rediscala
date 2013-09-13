package redis

import akka.actor._
import akka.util.Helpers
import redis.commands._
import scala.concurrent._
import java.net.InetSocketAddress
import redis.actors.{RedisSubscriberActorWithCallback, RedisClientActor}
import redis.api.pubsub._
import java.util.concurrent.atomic.AtomicLong
import akka.event.Logging
import redis.protocol.RedisReply

trait Request {
  implicit val executionContext: ExecutionContext

  def redisConnection: ActorRef

  def send[T](redisCommand: RedisCommand[_ <: RedisReply, T]): Future[T] = {
    val promise = Promise[T]()
    redisConnection ! Operation(redisCommand, promise)
    promise.future
  }
}

trait RedisCommands
  extends Keys
  with Strings
  with Hashes
  with Lists
  with Sets
  with SortedSets
  with Publish
  with Scripting
  with Connection
  with Server

abstract class RedisClientActorLike(system: ActorSystem) {
  var host: String
  var port: Int
  val name: String
  implicit val executionContext = system.dispatcher

  val redisConnection: ActorRef = system.actorOf(
    Props(classOf[RedisClientActor], new InetSocketAddress(host, port))
      .withDispatcher(Redis.dispatcher),
    name + '-' + Redis.tempName()
  )

  def reconnect(host: String = host, port: Int = port) = {
    if (this.host != host || this.port != port) {
      this.host = host
      this.port = port
      redisConnection ! new InetSocketAddress(host, port)
    }
  }

  /**
   * Disconnect from the server (stop the actor)
   */
  def stop() {
    system stop redisConnection
  }
}

case class RedisClient(var host: String = "localhost",
                       var port: Int = 6379,
                       name: String = "RedisClient")
                      (implicit _system: ActorSystem) extends RedisClientActorLike(_system) with RedisCommands with Transactions {

}

case class RedisBlockingClient(var host: String = "localhost",
                               var port: Int = 6379,
                               name: String = "RedisBlockingClient")
                              (implicit _system: ActorSystem) extends RedisClientActorLike(_system) with BLists {
}

case class RedisPubSub(
                        host: String = "localhost",
                        port: Int = 6379,
                        channels: Seq[String],
                        patterns: Seq[String],
                        onMessage: Message => Unit = _ => {},
                        onPMessage: PMessage => Unit = _ => {},
                        name: String = "RedisPubSub"
                        )(implicit system: ActorSystem) {

  val redisConnection: ActorRef = system.actorOf(
    Props(classOf[RedisSubscriberActorWithCallback],
      new InetSocketAddress(host, port), channels, patterns, onMessage, onPMessage)
      .withDispatcher(Redis.dispatcher),
    name + '-' + Redis.tempName()
  )

  /**
   * Disconnect from the server (stop the actor)
   */
  def stop() {
    system stop redisConnection
  }

  def subscribe(channels: String*) {
    redisConnection ! SUBSCRIBE(channels: _*)
  }

  def unsubscribe(channels: String*) {
    redisConnection ! UNSUBSCRIBE(channels: _*)
  }

  def psubscribe(patterns: String*) {
    redisConnection ! PSUBSCRIBE(patterns: _*)
  }

  def punsubscribe(patterns: String*) {
    redisConnection ! PUNSUBSCRIBE(patterns: _*)
  }
}

trait SentinelCommands
  extends Sentinel

case class SentinelClient(var host: String = "localhost",
                          var port: Int = 26379,
                          onMasterChange: (String, String, Int) => Unit = (masterName: String, ip: String, port: Int) => {},
                          onNewSentinel:  (String, String, Int) => Unit = (masterName: String, sentinelip: String, sentinelport: Int) => {},
                          onSentinelDown: (String, String, Int) => Unit = (masterName: String, sentinelip: String, sentinelport: Int) => {},
                          name: String = "SentinelClient")
                         (implicit _system: ActorSystem) extends RedisClientActorLike(_system) with SentinelCommands {
  val system: ActorSystem = _system

  val log = Logging.getLogger(system, this)

  val channels = Seq("+switch-master", "+sentinel", "+sdown", "+failover-state-send-slaveof-noone")

  val onMessage = (message: Message) => {
    if (log.isDebugEnabled)
      log.debug(s"SentinelClient.onMessage: message received: $message")

    message match {
      case Message("+switch-master", data) => {
        data.split(" ") match {
          case Array(master, oldip, oldport, newip, newport) =>
            onMasterChange(master, newip, newport.toInt)
          case _ => {}
        }
      }
      case Message("+failover-state-send-slaveof-noone", data) => {
        data.split(" ") match {
          case Array("slave", slaveName, slaveip, slaveport, "@", master, masterip, masterport) =>
            onMasterChange(master, slaveip, slaveport.toInt)
          case _ => {}
        }
      }
      case Message("+sentinel", data) => {
        data.split(" ") match {
          case Array("sentinel", sentName, sentinelip, sentinelport, "@", master, masterip, masterport) =>
            onNewSentinel(master, sentinelip, sentinelport.toInt)
          case _ => {}
        }
      }
      case Message("+sdown", data) => {
        data.split(" ") match {
          case Array("sentinel", sentName, sentinelip, sentinelport, "@", master, masterip, masterport) =>
            onSentinelDown(master, sentinelip, sentinelport.toInt)
          case _ => {}
        }
      }
      case _ => {
        log.warning(s"SentinelClient.onMessage: unexpected message received: $message")
      }
    }
  }

  val redisPubSubConnection: ActorRef = system.actorOf(
    Props(classOf[RedisSubscriberActorWithCallback],
      new InetSocketAddress(host, port), channels, Seq(), onMessage, (pmessage: PMessage) => {})
      .withDispatcher(Redis.dispatcher),
    name + '-' + Redis.tempName()
  )

  /**
   * Disconnect from the server (stop the actors)
   */
  override def stop() {
    system stop redisConnection
    system stop redisPubSubConnection
  }

}

abstract class SentinelMonitored(system: ActorSystem) {
  val sentinels: Seq[(String, Int)]
  val master: String
  val onMasterChange: (String, Int) => Unit

  implicit val executionContext = system.dispatcher

  val sentinelClients =
        collection.mutable.Map(
          sentinels.map(hp =>
            (makeSentinelClientKey(hp._1, hp._2), makeSentinelClient(hp._1, hp._2))
          ):_*
        )

  def makeSentinelClientKey(host: String, port: Int) = s"$host:$port"

  def makeSentinelClient(host: String, port: Int): SentinelClient = {
    new SentinelClient(host, port, onSwitchMaster, onNewSentinel, onSentinelDown, "SMSentinelClient")(system)
  }


  def onSwitchMaster(masterName: String, ip: String, port: Int) = {
    if (master == masterName)
      onMasterChange(ip, port)
  }

  def onNewSentinel(masterName: String, sentinelip: String, sentinelport: Int) = {
    val k = makeSentinelClientKey(sentinelip, sentinelport)
    if (master == masterName && !sentinelClients.contains(k)) {
      synchronized {
        if (!sentinelClients.contains(k))
          sentinelClients += k -> makeSentinelClient(sentinelip, sentinelport)
      }
    }
  }

  def onSentinelDown(masterName: String, sentinelip: String, sentinelport: Int) = {
    val k = makeSentinelClientKey(sentinelip, sentinelport)
    if (master == masterName && sentinelClients.contains(k)) {
      synchronized {
        if (sentinelClients.contains(k))
          sentinelClients -= k
      }
    }
  }

  def withMasterAddr[T](initFunction: (String, Int) => T): T = {
    import scala.concurrent.duration._

    val f = sentinelClients.values.map(_.getMasterAddr(master))
    val ff = Future.find(f) { case Some((_: String, _: Int)) => true case _ => false }
                   .map {
                      case Some(Some((ip: String, port: Int))) => initFunction(ip, port)
                      case _ => throw new Exception(s"No such master '$master'")
                   }

    Await.result(ff, 15 seconds)
  }
}

abstract class SentinelMonitoredRedisClientLike(system: ActorSystem) extends SentinelMonitored(system) {
  val redisClient: RedisClientActorLike
  val onMasterChange = (ip: String, port: Int) => {
    redisClient.reconnect(ip, port)
  }

  def redisConnection = redisClient.redisConnection

  /**
   * Disconnect from the server (stop the actors)
   */
  def stop() = {
    redisClient.stop()
    sentinelClients.values.foreach(_.stop())
  }

}

case class SentinelMonitoredRedisClient( sentinels: Seq[(String, Int)] = Seq(("localhost", 26379)),
                                         master: String)
                                       (implicit system: ActorSystem) extends SentinelMonitoredRedisClientLike(system) with RedisCommands with Transactions {

  val redisClient: RedisClient = withMasterAddr((ip, port) => {
    new RedisClient(ip, port, "SMRedisClient")
  })

}

case class SentinelMonitoredRedisBlockingClient( sentinels: Seq[(String, Int)] = Seq(("localhost", 26379)),
                                                 master: String)
                                               (implicit system: ActorSystem) extends SentinelMonitoredRedisClientLike(system) with RedisCommands with Transactions {
  val redisClient: RedisBlockingClient = withMasterAddr((ip, port) => {
    new RedisBlockingClient(ip, port, "SMRedisClient")
  })
}

private[redis] object Redis {

  val dispatcher = "rediscala.rediscala-client-worker-dispatcher"

  val tempNumber = new AtomicLong

  def tempName() = Helpers.base64(tempNumber.getAndIncrement())

}