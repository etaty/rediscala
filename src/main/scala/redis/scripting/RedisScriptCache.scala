package redis.scripting

import akka.actor.ActorRefFactory
import java.io.File
import redis.RedisClient
import redis.api.scripting.RedisScript
import redis.protocol.RedisReply
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

object RedisScriptCache {
  def fileToString(path: String): String = {
    scala.io.Source.fromURL(getClass.getResource(path)).mkString
  }

  def apply(redis: RedisClient, root: String = "/lua", extension: String = ".lua")
           (implicit _system: ActorRefFactory): RedisScriptCache = {
    new RedisScriptCache(redis, root, extension)
  }
}

class RedisScriptCache(redis: RedisClient, root: String = "/lua", extension: String = ".lua")
                      (implicit _system: ActorRefFactory) {
  import RedisScriptCache._

  protected val scriptCache = mutable.Map.empty[String, RedisScript]


  def nameToPath(name: String): String = {
    if(root.takeRight(1) == "/") s"$root$name$extension"
    else s"$root/$name$extension"
  }

  def pathToName(path: String): String = {
    path.split("/").last.dropRight(extension.length)
  }

  def loadByPath(path: String): Future[String] = {
    scriptCache.update(pathToName(path), RedisScript(fileToString(path)))
    redis.scriptLoad(path)
  }

  def loadByName(name: String): Future[String] = {
    scriptCache.update(name, RedisScript(fileToString(nameToPath(name))))
    redis.scriptLoad(nameToPath(name))
  }

  def registerScripts()(implicit dispatcher: ExecutionContextExecutor): Future[List[String]] = {
    val dir = new File(getClass.getResource(root).toURI.getPath)

    Future.sequence(
      dir.list
        .filter(_.endsWith(extension))
        .map(_.dropRight(extension.length))
        .map(loadByName).toList
    )
  }


  def run(scriptName: String, keys: Seq[String] = Seq(), args: Seq[String] = Seq())
         (implicit dispatcher: ExecutionContextExecutor): Future[RedisReply] = {
    val redisScript = scriptCache.getOrElseUpdate(scriptName, RedisScript(fileToString(nameToPath(scriptName))))
    redis.evalshaOrEval(redisScript, keys, args)
  }
}

