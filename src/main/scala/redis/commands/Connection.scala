package redis.commands

import akka.util.ByteString
import redis.{RedisValueConverter, Request}
import scala.concurrent.Future
import redis.protocol.Status
import redis.api.connection._

trait Connection extends Request {
  def auth[A](value: A)(implicit convert: RedisValueConverter[A]): Future[Status] =
    send(Auth(value))

  def echo[A](value: A)(implicit convert: RedisValueConverter[A]): Future[Option[ByteString]] =
    send(Echo(value))

  def ping(): Future[String] =
    send(Ping)

  // commands sent after will fail with [[redis.protocol.NoConnectionException]]
  def quit(): Future[Boolean] =
    send(Quit)

  def select(index: Int): Future[Boolean] =
    send(Select(index))
}

