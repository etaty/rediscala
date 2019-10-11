package redis.commands

import redis.Request
import redis.api.servers._
import scala.concurrent.Future
import redis.api.ShutdownModifier

trait Server extends Request {
  def bgrewriteaof(): Future[String] = send(Bgrewriteaof)

  def bgsave(): Future[String] = send(Bgsave)

  def clientKill(ip: String, port: Int): Future[Boolean] =
    send(ClientKill(ip, port))

  def clientList(): Future[Seq[Map[String, String]]] =
    send(ClientList)

  def clientGetname(): Future[Option[String]] =
    send(ClientGetname)

  def clientSetname(connectionName: String): Future[Boolean] =
    send(ClientSetname(connectionName))

  def configGet(parameter: String): Future[Map[String, String]] =
    send(ConfigGet(parameter))

  def configSet(parameter: String, value: String): Future[Boolean] =
    send(ConfigSet(parameter, value))

  def configResetstat(): Future[Boolean] =
    send(ConfigResetstat)

  def dbsize(): Future[Long] =
    send(Dbsize)

  def debugObject(key: String): Future[String] =
    send(DebugObject(key))

  def debugSegfault(): Future[String] =
    send(DebugSegfault)

  def flushall(async: Boolean = false): Future[Boolean] =
    send(Flushall(async))

  def flushdb(async: Boolean = false): Future[Boolean] =
    send(Flushdb(async))

  def info(): Future[String] =
    send(Info())

  def info(section: String): Future[String] =
    send(Info(Some(section)))

  def lastsave(): Future[Long] =
    send(Lastsave)

  def save(): Future[Boolean] =
    send(Save)

  def shutdown(): Future[Boolean] =
    send(Shutdown())

  def shutdown(modifier: ShutdownModifier): Future[Boolean] =
    send(Shutdown(Some(modifier)))

  def slaveof(host: String, port: Int): Future[Boolean] =
    send(Slaveof(host, port))

  def slaveofNoOne(): Future[Boolean] = send(SlaveofNoOne)

  def time(): Future[(Long, Long)] =
    send(Time)
}