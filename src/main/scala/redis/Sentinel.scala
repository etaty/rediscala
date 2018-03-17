package redis

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import redis.actors.RedisSubscriberActorWithCallback
import redis.api.pubsub.{Message, PMessage}
import redis.commands.Sentinel

import scala.concurrent.{Await, Future}

trait SentinelCommands
  extends Sentinel

case class SentinelClient(var host: String = "localhost",
                          var port: Int = 26379,
                          onMasterChange: (String, String, Int) => Unit = (masterName: String, ip: String, port: Int) => {},
                          onNewSentinel: (String, String, Int) => Unit = (masterName: String, sentinelip: String, sentinelport: Int) => {},
                          onSentinelDown: (String, String, Int) => Unit = (masterName: String, sentinelip: String, sentinelport: Int) => {},
                          onNewSlave: (String, String, Int) => Unit = (masterName: String, sentinelip: String, sentinelport: Int) => {},
                          onSlaveDown: (String, String, Int) => Unit = (masterName: String, sentinelip: String, sentinelport: Int) => {},
                          name: String = "SentinelClient")
                         (implicit _system: ActorSystem,
                          redisDispatcher: RedisDispatcher = Redis.dispatcher
                         ) extends RedisClientActorLike(_system, redisDispatcher) with SentinelCommands {
  val system: ActorSystem = _system

  val log = Logging.getLogger(system, this)

  val channels = Seq("+switch-master", "+sentinel", "+sdown", "-sdown", "+failover-state-send-slaveof-noone", "+slave")

  val onMessage = (message: Message) => {

    if (log.isDebugEnabled)
      log.debug(s"SentinelClient.onMessage: message received:${message.channel} ${message.data.utf8String}")

    message match {
      case Message("+switch-master", data) => {
        data.utf8String.split(" ") match {
          case Array(master, oldip, oldport, newip, newport) =>
            onMasterChange(master, newip, newport.toInt)
          case _ => {}
        }
      }
      case Message("+failover-state-send-slaveof-noone", data) => {
        data.utf8String.split(" ") match {
          case Array("slave", slaveName, slaveip, slaveport, "@", master, masterip, masterport) =>
            onMasterChange(master, slaveip, slaveport.toInt)
          case _ => {}
        }
      }
      case Message("+sentinel", data) => {
        data.utf8String.split(" ") match {
          case Array("sentinel", sentName, sentinelip, sentinelport, "@", master, masterip, masterport) =>
            onNewSentinel(master, sentinelip, sentinelport.toInt)
          case _ => {}
        }
      }
      case Message("+sdown", data) => {
        data.utf8String.split(" ") match {
          case Array("sentinel", sentName, sentinelip, sentinelport, "@", master, masterip, masterport) =>
            onSentinelDown(master, sentinelip, sentinelport.toInt)

          case Array("slave", slaveName, slaveip, slaveport, "@", master, masterip, masterport) =>
            onSlaveDown(master, slaveip, slaveport.toInt)

          case _ => {}
        }
      }
      case Message("-sdown", data) => {
        data.utf8String.split(" ") match {
          case Array("slave", slaveName, slaveip, slaveport, "@", master, masterip, masterport) =>
            onNewSlave(master, slaveip, slaveport.toInt)

          case _ => {}
        }
      }
      case Message("+slave", data) => {
        data.utf8String.split(" ") match {
          case Array("slave", slaveName, slaveip, slaveport, "@", master, masterip, masterport) =>
            onNewSlave(master, slaveip, slaveport.toInt)

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
      new InetSocketAddress(host, port), channels, Seq(), onMessage, (pmessage: PMessage) => {}, None, (status: Boolean) => {})
      .withDispatcher(redisDispatcher.name),
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

abstract class SentinelMonitored(system: ActorSystem, redisDispatcher: RedisDispatcher) {
  val sentinels: Seq[(String, Int)]
  val master: String
  val onMasterChange: (String, Int) => Unit
  val onNewSlave: (String, Int) => Unit
  val onSlaveDown: (String, Int) => Unit

  implicit val executionContext = system.dispatchers.lookup(redisDispatcher.name)

  val log = Logging.getLogger(system, this)

  val sentinelClients =
    collection.mutable.Map(
      sentinels.map(
        hp =>
          (makeSentinelClientKey(hp._1, hp._2), makeSentinelClient(hp._1, hp._2))
      ): _*
    )

  def makeSentinelClientKey(host: String, port: Int) = s"$host:$port"


  def internalOnNewSlave(masterName: String, ip: String, port: Int) {
    if (master == masterName)
      onNewSlave(ip, port)
  }

  def internalOnSlaveDown(masterName: String, ip: String, port: Int) {
    if (master == masterName)
      onSlaveDown(ip, port)
  }

  def onSwitchMaster(masterName: String, ip: String, port: Int) {
    if (master == masterName) {
      onMasterChange(ip, port)
      onSlaveDown(ip, port)
    }
  }

  def makeSentinelClient(host: String, port: Int): SentinelClient = {
    new SentinelClient(host, port, onSwitchMaster, onNewSentinel, onSentinelDown, internalOnNewSlave, internalOnSlaveDown, "SMSentinelClient")(system)
  }

  def onNewSentinel(masterName: String, sentinelip: String, sentinelport: Int) {
    val k = makeSentinelClientKey(sentinelip, sentinelport)
    if (master == masterName && !sentinelClients.contains(k)) {
      sentinelClients.synchronized {
        if (!sentinelClients.contains(k))
          sentinelClients += k -> makeSentinelClient(sentinelip, sentinelport)
      }
    }
  }

  def onSentinelDown(masterName: String, sentinelip: String, sentinelport: Int) {
    val k = makeSentinelClientKey(sentinelip, sentinelport)
    if (master == masterName && sentinelClients.contains(k)) {
      sentinelClients.synchronized {
        if (sentinelClients.contains(k)) {
          sentinelClients(k).stop
          sentinelClients -= k
        }
      }
    }
  }

  def withMasterAddr[T](initFunction: (String, Int) => T): T = {
    import scala.concurrent.duration._

    val f = sentinelClients.values.map(_.getMasterAddr(master))
    val ff = Future.sequence(f).map { listAddr =>
      listAddr.flatten
        .headOption
        .map {
          case (ip: String, port: Int) => initFunction(ip, port)
        }.getOrElse(throw new Exception(s"No such master '$master'"))
    }

    Await.result(ff, 15 seconds)
  }

  def withSlavesAddr[T](initFunction: Seq[(String, Int)] => T): T = {
    import scala.concurrent.duration._

    val fslaves = Future.sequence(sentinelClients.values.map(_.slaves(master)))
      .map { lm =>
        val ipPortBuilder = Set.newBuilder[(String, Int)]
        for {
          slaves <- lm
          slave <- slaves
          ip <- slave.get("ip")
          port <- slave.get("port")
        } yield {
          ipPortBuilder += ip -> port.toInt
        }
        initFunction(ipPortBuilder.result().toSeq)
      }

    Await.result(fslaves, 15 seconds)
  }
}

abstract class SentinelMonitoredRedisClientLike(system: ActorSystem, redisDispatcher: RedisDispatcher
                                               ) extends SentinelMonitored(system, redisDispatcher) with ActorRequest {
  val redisClient: RedisClientActorLike
  val onMasterChange = (ip: String, port: Int) => {
    log.info(s"onMasterChange: $ip:$port")
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

abstract class SentinelMonitoredRedisBlockingClientLike(system: ActorSystem, redisDispatcher: RedisDispatcher
                                                       ) extends SentinelMonitored(system, redisDispatcher) with ActorRequest {
  val redisClient: RedisClientActorLike

  val onMasterChange = (ip: String, port: Int) => {
    log.info(s"onMasterChange: $ip:$port")
    redisClient.reconnect(ip, port)
  }

  /**
    * Disconnect from the server (stop the actors)
    */
  def stop() = {
    redisClient.stop()
    sentinelClients.values.foreach(_.stop())
  }

}

