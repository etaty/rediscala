package redis.commands

import redis.api.blists._
import redis.{ByteStringDeserializer, Request}

import scala.concurrent.Future
import scala.concurrent.duration._
import redis.api.clusters._
/**
 * Blocking commands on the Lists
 */
trait Clusters extends Request {

  def clusterSlots(): Future[Seq[ClusterSlot]] = send(ClusterSlots())

  def clusterInfo(): Future[Map[String, String]] = send(ClusterInfo())
  
  def clusterNodes(): Future[Array[ClusterNodeInfo]] = send(ClusterNodes())
}