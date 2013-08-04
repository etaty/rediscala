rediscala [![Build Status](https://travis-ci.org/etaty/rediscala.png)](https://travis-ci.org/etaty/rediscala) [![Coverage Status](https://coveralls.io/repos/etaty/rediscala/badge.png?branch=master)](https://coveralls.io/r/etaty/rediscala?branch=master)
=========

A [Redis](http://redis.io/) client for Scala (2.10+) and (AKKA 2.2+)

 * Reactive : Redis requests/replies are wrapped in Futures.

 * Typesafe : Redis types are mapped to Scala types.

 * Fast : Rediscala uses redis pipelining. Blocking redis commands are moved into their own connection.

### Set up your project dependencies

If you use SBT, you just have to edit `build.sbt` and add the following:

```scala
resolvers += "rediscala" at "https://github.com/etaty/rediscala-mvn/raw/master/snapshots/"

libraryDependencies ++= Seq(
  "com.etaty.rediscala" %% "rediscala" % "0.6-SNAPSHOT"
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

###### Basic
https://github.com/etaty/rediscala-demo

###### Blocking commands

###### Pub/Sub

### Commands

Supported :
* [Keys](http://redis.io/commands#generic) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Keys))
* [Strings](http://redis.io/commands#string) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Strings))
* [Hashes](http://redis.io/commands#hash) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Hashes))
* [Lists](http://redis.io/commands#list) 
  * non-blocking ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Lists))
  * blocking ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.BLists))
* [Sets](http://redis.io/commands#set) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Sets))
* [Sorted Sets](http://redis.io/commands#sorted_set) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.SortedSets))
* [Pub/Sub](http://redis.io/commands#pubsub) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.PubSub))
* [Transactions](http://redis.io/commands#transactions) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Transactions))
* [Connection](http://redis.io/commands#connection) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Connection))
* [Server](http://redis.io/commands#server) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Server))

Soon :
* [Scripting](http://redis.io/commands#scripting)


### Scaladoc

[Rediscala scaladoc API](http://etaty.github.io/rediscala/latest/api/index.html#package)

### Performance

More than 200 000 requests/second

* [benchmark result from scalameter](http://bit.ly/12QZsRs)
* [sources directory](https://github.com/etaty/rediscala/tree/master/src/benchmark/scala/redis/bench)

The hardware used is a macbook retina (Intel Core i7, 2.6 GHz, 4 cores, 8 threads, 8GB)

You can run the bench with :

1. clone the repo `git clone git@github.com:etaty/rediscala.git`
2. run `sbt bench:test`
3. open the bench report `rediscala/tmp/report/index.html`


###TODO
* commands
  * scriptings
  * server
  * keys : MIGRATE, MOVE, OBJECT, SORT
* Performance
  * Maybe request should be encoded in their own scala future (benchmark needed)
