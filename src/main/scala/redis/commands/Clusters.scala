package redis.commands

import redis.Request
import redis.api.clusters._

import scala.concurrent.Future
/**
 * Blocking commands on the Lists
 */
trait Clusters extends Request {

  def clusterSlots(): Future[Seq[ClusterSlot]] = send(ClusterSlots())

  def clusterInfo(): Future[Map[String, String]] = send(ClusterInfo())
  
  def clusterNodes(): Future[Array[ClusterNodeInfo]] = send(ClusterNodes())
}