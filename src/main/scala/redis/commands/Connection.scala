package redis.commands

import akka.util.ByteString
import redis.{ByteStringSerializer, Request}
import scala.concurrent.Future
import redis.protocol.Status
import redis.api.connection._

trait Connection extends Request {
  def auth[V: ByteStringSerializer](value: V): Future[Status] =
    send(Auth(value))

  def echo[V: ByteStringSerializer](value: V): Future[Option[ByteString]] =
    send(Echo(value))

  def ping(): Future[String] =
    send(Ping)

  // commands sent after will fail with [[redis.protocol.NoConnectionException]]
  def quit(): Future[Boolean] =
    send(Quit)

  def select(index: Int): Future[Boolean] =
    send(Select(index))
}

