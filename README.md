rediscala [![Build Status](https://travis-ci.org/etaty/rediscala.png)](https://travis-ci.org/etaty/rediscala) [![Coverage Status](https://coveralls.io/repos/etaty/rediscala/badge.png?branch=master)](https://coveralls.io/r/etaty/rediscala?branch=master)
=========

A [Redis](http://redis.io/) client for Scala (2.10+) and (AKKA 2.2+)

 * Replies are wrapped in Futures.

 * Rediscala uses redis pipelining. Blocking redis commands are moved into their own connection.

### Set up your project dependencies

If you use SBT, you just have to edit `build.sbt` and add the following:

```scala
resolvers += "rediscala" at "https://github.com/etaty/rediscala-mvn/raw/master/snapshots/"

libraryDependencies ++= Seq(
  "com.etaty.rediscala" %% "rediscala" % "0.1-SNAPSHOT"
)
```

### Connect to the database

```scala
import redis.RedisClient
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {
  implicit val akkaSystem = akka.actor.ActorSystem()

  val redis = RedisClient()

  val futurePong = redis.ping()
  println("Ping sent!")
  futurePong.map(pong => {
    println(s"Redis replied with a $pong")
  })
  Await.result(futurePong, 5 seconds)

  akkaSystem.shutdown()
}
```

### Examples

https://github.com/etaty/rediscala-demo

### Scaladoc

[Rediscala scaladoc API](http://etaty.github.io/rediscala/0.1-SNAPSHOT/api/index.html#package)

### Performance

Between 200 000 and 150 000 pings/seconds with one client on a 2.6GHz Intel Core i7 (8 threads)

For comparison, redis-benchmark with one client can get 1 000 000 pings/seconds

There are still rooms for improvements

###TODO
* commands
  * scriptings
  * server
  * keys : MIGRATE, MOVE, OBJECT, SORT
* --handling of connection failure-- (actor tests)
* Performance
  * Maybe request should be encoded in their own scala future (benchmark needed)
