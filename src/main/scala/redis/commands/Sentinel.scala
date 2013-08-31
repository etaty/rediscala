package redis.commands

import redis.Request
import scala.concurrent.Future
import redis.api._
import redis.api.SenMasters
import redis.api.SenSlaves
import redis.api.SenGetMasterAddr
import scala.Some
import redis.api.SenIsMasterDown

trait Sentinel extends Request {

  def masters(): Future[Seq[Map[String,String]]] =
    send(SenMasters())

  def slaves(master: String): Future[Seq[Map[String,String]]] =
    send(SenSlaves(master))

  def isMasterDown(masterIp: String, port: Int): Future[Option[Boolean]] =
    send(SenIsMasterDown(masterIp, port)) map {
      case Seq(sdown, runid) if runid != "?" => Some(sdown == "1")
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
