package redis.commands

import redis.Request
import redis.actors.ReplyErrorException
import redis.api._

import scala.concurrent.Future
import scala.util.control.NonFatal

trait Sentinel extends Request {

  def masters(): Future[Seq[Map[String, String]]] =
    send(SenMasters()) recover recoverWith(Seq.empty)

  def slaves(master: String): Future[Seq[Map[String, String]]] =
    send(SenSlaves(master)) recover recoverWith(Seq.empty)

  def isMasterDown(master: String): Future[Option[Boolean]] = {
    send(SenMasterInfo(master)) map { response =>
      Some(!(response("name") == master && response("flags") == "master"))
    } recoverWith {
      case ReplyErrorException(message) if message.startsWith("ERR No such master with that name") => Future.successful(None)
    } recover recoverWith(None)
  }

  def getMasterAddr(master: String): Future[Option[(String, Int)]] =
    send(SenGetMasterAddr(master)) map {
      case Some(Seq(ip, port)) => Some((ip, port.toInt))
      case _ => None
    } recover recoverWith(None)

  def resetMaster(pattern: String): Future[Boolean] =
    send(SenResetMaster(pattern))

  def failover(master: String): Future[Boolean] =
    send(SenMasterFailover(master))

  private def recoverWith[R](r: R): PartialFunction[Throwable, R] = {
    case NonFatal(_) => r
  }
}
