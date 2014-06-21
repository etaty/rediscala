package redis.commands

import redis.Request
import scala.concurrent.Future
import redis.api._
import scala.Some
import redis.actors.ReplyErrorException

trait Sentinel extends Request {

  def masters(): Future[Seq[Map[String,String]]] =
    send(SenMasters())

  def slaves(master: String): Future[Seq[Map[String,String]]] =
    send(SenSlaves(master))

  def isMasterDown(master: String): Future[Option[Boolean]] =
    send(SenMasterInfo(master)).recoverWith({
      case ReplyErrorException(message) if message.startsWith("ERR No such master with that name") => Future(Some(true))
    }) map {
      case response: Map[String, String] => Some(!(response("name") == master && response("flags") == "master"))
      case _ => None
    }

  def getMasterAddr(master: String): Future[Option[(String, Int)]] =
    send(SenGetMasterAddr(master)) map {
      case Some(Seq(ip, port)) => Some((ip, port.toInt))
      case _ => None
    }

  def resetMaster(pattern: String): Future[Boolean] =
    send(SenResetMaster(pattern))

  def failover(master: String): Future[Boolean] =
    send(SenMasterFailover(master))

}
