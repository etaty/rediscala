package redis.commands

import redis.Request
import redis.actors.ReplyErrorException
import redis.api._

import scala.concurrent.Future

trait Sentinel extends Request {

  def masters(): Future[Seq[Map[String, String]]] =
    send(SenMasters())

  def slaves(master: String): Future[Seq[Map[String, String]]] =
    send(SenSlaves(master))

  def isMasterDown(master: String): Future[Option[Boolean]] = {
    send(SenMasterInfo(master)) map { response =>
      Some(!(response("name") == master && response("flags") == "master"))
    } recoverWith {
      case ReplyErrorException(message) if message.startsWith("ERR No such master with that name") => Future.successful(None)
    }
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
