package redis

import java.net.InetSocketAddress
import akka.routing.{Broadcast, RouterConfig}
import scala.concurrent.duration.FiniteDuration
import akka.actor.{Terminated, Props, ActorRef, Actor}
import akka.event.Logging
/*
class RedisClientSupervisor(address: InetSocketAddress, routerConfig: RouterConfig, description: String,
                            reconnectDuration: FiniteDuration, worker: => Actor) extends Actor {
  val log = Logging(context.system, this)
  var router: Option[ActorRef] = None

  def routerProto = {
    context.system.actorOf(Props(worker).withRouter(routerConfig))
  }

  def initialize {
    router = Some(routerProto)
    router.get ! Broadcast(RedisClientActor.ConnectToHost(address))
    context.watch(router.get)
  }

  override def preStart = {
    self ! InitializeRouter
  }

  def receive = {
    case InitializeRouter ⇒
      initialize

    case ReconnectRouter ⇒
      if (router.isEmpty) initialize

    case Terminated(actor) ⇒
      /* If router died, restart after a period of time */
      router = None
      log.debug("Router died, restarting in: " + reconnectDuration.toString())
      context.system.scheduler.scheduleOnce(reconnectDuration, self, ReconnectRouter)

    case x: Operation[_, _] ⇒
      router match {
        case Some(r) ⇒ r forward x
        case None ⇒ x.promise.failure(NoConnectionException())
      }

    case _ ⇒
  }
}
*/