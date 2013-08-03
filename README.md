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
* [Keys](http://redis.io/commands#generic)
* [Strings](http://redis.io/commands#string)
* [Hashes](http://redis.io/commands#hash)
* [Lists](http://redis.io/commands#list)
* [Set](http://redis.io/commands#set)
* [Sorted Sets](http://redis.io/commands#sorted_set)
* [Pub/Sub](http://redis.io/commands#pubsub)
* [Transactions](http://redis.io/commands#transactions)
* [Connection](http://redis.io/commands#connection)
* [Server](http://redis.io/commands#server)

Soon :
* [Scripting](http://redis.io/commands#scripting)


### Scaladoc

[Rediscala scaladoc API](http://etaty.github.io/rediscala/latest/api/index.html#package)

### Performance

More than 200 000 requests/second

[scalemeter report](http://etaty.github.io/rediscala/report/index.html)
[Sources](https://github.com/etaty/rediscala/tree/master/src/benchmark/scala/redis/bench)
The hardware used is a macbook retina (Intel Core i7, 2.6 GHz, 4 cores, 8 threads, 8GB)

You can run the bench with :
1. clone the repo `git clone git@github.com:etaty/rediscala.git`
2. run `sbt bench:test`
3. open the test `rediscala/tmp/report/index.html`


###TODO
* commands
  * scriptings
  * server
  * keys : MIGRATE, MOVE, OBJECT, SORT
* Performance
  * Maybe request should be encoded in their own scala future (benchmark needed)
