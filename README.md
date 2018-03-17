rediscala [![Build Status](https://travis-ci.org/Ma27/rediscala.svg)](https://travis-ci.org/Ma27/rediscala) [![Coverage Status](https://img.shields.io/coveralls/Ma27/rediscala.svg)](https://coveralls.io/r/Ma27/rediscala?branch=master) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.Ma27/rediscala_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.Ma27/rediscala_2.11)
=========

A [Redis](http://redis.io/) client for Scala (2.11+) and (AKKA 2.5+) with non-blocking and asynchronous I/O operations.

 * Reactive : Redis requests/replies are wrapped in Futures.

 * Typesafe : Redis types are mapped to Scala types.

 * Fast : Rediscala uses redis pipelining. Blocking redis commands are moved into their own connection. 
A worker actor handles I/O operations (I/O bounds), another handles decoding of Redis replies (CPU bounds).

### Set up your project dependencies

If you use SBT, you just have to edit `build.sbt` and add the following:

From version 1.8.0: 
 * use Akka 2.5 (Java 1.8)
 * released for Scala 2.11 & 2.12

```scala
libraryDependencies += "com.github.Ma27" %% "rediscala" % "1.8.2"
```

From version 1.3.1: 
 * use akka 2.3
 * released for scala 2.10 & 2.11
```scala
// new repo on maven.org
libraryDependencies += "com.github.etaty" %% "rediscala" % "1.7.0"


// old repo on bintray (1.5.0 and inferior version)
resolvers += "rediscala" at "http://dl.bintray.com/etaty/maven"
libraryDependencies += "com.etaty.rediscala" %% "rediscala" % "1.5.0"
```

For older rediscala versions (<= 1.3):
 * use akka 2.2
 * released for scala 2.10 only
 * use github "repo"
```scala
resolvers += "rediscala" at "https://raw.github.com/etaty/rediscala-mvn/master/releases/"

libraryDependencies += "com.etaty.rediscala" %% "rediscala" % "1.3"
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

  akkaSystem.terminate()
}
```

### Basic Example

https://github.com/etaty/rediscala-demo

You can fork with : `git clone git@github.com:etaty/rediscala-demo.git` then run it, with `sbt run`


### Redis Commands

All commands are supported :
* [Keys](http://redis.io/commands#generic) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Keys))
* [Strings](http://redis.io/commands#string) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Strings))
* [Hashes](http://redis.io/commands#hash) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Hashes))
* [Lists](http://redis.io/commands#list) 
  * non-blocking ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Lists))
  * blocking ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.BLists))
* [Sets](http://redis.io/commands#set) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Sets))
* [Sorted Sets](http://redis.io/commands#sorted_set) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.SortedSets))
* [Pub/Sub](http://redis.io/commands#pubsub) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Publish))
* [Transactions](http://redis.io/commands#transactions) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Transactions))
* [Connection](http://redis.io/commands#connection) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Connection))
* [Scripting](http://redis.io/commands#scripting) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Scripting))
* [Server](http://redis.io/commands#server) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.Server))
* [HyperLogLog](http://redis.io/commands#hyperloglog) ([scaladoc](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.HyperLogLog))

### Blocking commands

[RedisBlockingClient](http://etaty.github.io/rediscala/latest/api/index.html#redis.RedisBlockingClient) is the instance allowing access to blocking commands :
* blpop
* brpop
* brpopplush

```scala
  redisBlocking.blpop(Seq("workList", "otherKeyWithWork"), 5 seconds).map(result => {
    result.map({
      case (key, work) => println(s"list $key has work : ${work.utf8String}")
    })
  })
```
Full example: [ExampleRediscalaBlocking](https://github.com/etaty/rediscala-demo/blob/master/src/main/scala/ExampleRediscalaBlocking.scala)

You can fork with: `git clone git@github.com:etaty/rediscala-demo.git` then run it, with `sbt run`


### Transactions

The idea behind transactions in Rediscala is to start a transaction outside of a redis connection.
We use the [TransactionBuilder](http://etaty.github.io/rediscala/latest/api/index.html#redis.commands.TransactionBuilder) to store call to redis commands (and for each command we give back a future).
When `exec` is called, `TransactionBuilder` will build and send all the commands together to the server. Then the futures will be completed.
By doing that we can use a normal connection with pipelining, and avoiding to trap a command from outside, in the transaction...

```scala
  val redisTransaction = redis.transaction() // new TransactionBuilder
  redisTransaction.watch("key")
  val set = redisTransaction.set("key", "abcValue")
  val decr = redisTransaction.decr("key")
  val get = redisTransaction.get("key")
  redisTransaction.exec()
```

Full example: [ExampleTransaction](https://github.com/etaty/rediscala-demo/blob/master/src/main/scala/ExampleTransaction.scala)

You can fork with : `git clone git@github.com:etaty/rediscala-demo.git` then run it, with `sbt run`

[TransactionsSpec](https://github.com/etaty/rediscala/blob/master/src/test/scala/redis/commands/TransactionsSpec.scala) will reveal even more gems of the API.

### Pub/Sub

You can use a case class with callbacks [RedisPubSub](http://etaty.github.io/rediscala/latest/api/index.html#redis.RedisPubSub)
or extend the actor [RedisSubscriberActor](http://etaty.github.io/rediscala/latest/api/index.html#redis.actors.RedisSubscriberActor) as shown in the example below

```scala
object ExamplePubSub extends App {
  implicit val akkaSystem = akka.actor.ActorSystem()

  val redis = RedisClient()

  // publish after 2 seconds every 2 or 5 seconds
  akkaSystem.scheduler.schedule(2 seconds, 2 seconds)(redis.publish("time", System.currentTimeMillis()))
  akkaSystem.scheduler.schedule(2 seconds, 5 seconds)(redis.publish("pattern.match", "pattern value"))
  // shutdown Akka in 20 seconds
  akkaSystem.scheduler.scheduleOnce(20 seconds)(akkaSystem.terminate())

  val channels = Seq("time")
  val patterns = Seq("pattern.*")
  // create SubscribeActor instance
  akkaSystem.actorOf(Props(classOf[SubscribeActor], channels, patterns).withDispatcher("rediscala.rediscala-client-worker-dispatcher"))

}

class SubscribeActor(channels: Seq[String] = Nil, patterns: Seq[String] = Nil) extends RedisSubscriberActor(channels, patterns) {
  override val address: InetSocketAddress = new InetSocketAddress("localhost", 6379)

  def onMessage(message: Message) {
    println(s"message received: $message")
  }

  def onPMessage(pmessage: PMessage) {
    println(s"pattern message received: $pmessage")
  }
}
```

Full example: [ExamplePubSub](https://github.com/etaty/rediscala-demo/blob/master/src/main/scala/ExamplePubSub.scala)

You can fork with : `git clone git@github.com:etaty/rediscala-demo.git` then run it, with `sbt run`

[RedisPubSubSpec](https://github.com/etaty/rediscala/blob/master/src/test/scala/redis/RedisPubSubSpec.scala) will reveal even more gems of the API.

### Scripting

`RedisScript` is a helper, you can put your LUA script inside and it will compute the hash. 
You can use it with `evalshaOrEval` which run your script even if it wasn't already loaded.

```scala
  val redis = RedisClient()

  val redisScript = RedisScript("return 'rediscala'")

  val r = redis.evalshaOrEval(redisScript).map({
    case b: Bulk => println(b.toString())
  })
  Await.result(r, 5 seconds)
```

Full example: [ExampleScripting](https://github.com/etaty/rediscala-demo/blob/master/src/main/scala/ExampleScripting.scala)

### Redis Sentinel

[SentinelClient](http://etaty.github.io/rediscala/latest/api/index.html#redis.SentinelClient) connect to a redis sentinel server.

[SentinelMonitoredRedisClient](http://etaty.github.io/rediscala/latest/api/index.html#redis.SentinelMonitoredRedisClient) connect to a sentinel server to find the master addresse then start a connection. In case the master change your RedisClient connection will automatically connect to the new master server.
If you are using a blocking client, you can use [SentinelMonitoredRedisBlockingClient](http://etaty.github.io/rediscala/latest/api/index.html#redis.SentinelMonitoredRedisBlockingClient)

### Pool

[RedisClientPool](http://etaty.github.io/rediscala/latest/api/index.html#redis.RedisClientPool) connect to a pool of redis servers.
Redis commands are dispatched to redis connection in a round robin way.

### Master Slave

[RedisClientMasterSlaves](http://etaty.github.io/rediscala/latest/api/index.html#redis.RedisClientMasterSlaves) connect to a master and a pool of slaves.
The `write` commands are sent to the master, while the read commands are sent to the slaves in the [RedisClientPool](http://etaty.github.io/rediscala/latest/api/index.html#redis.RedisClientPool)

### Config Which Dispatcher to Use

By default, the actors in this project will use the dispatcher `rediscala.rediscala-client-worker-dispatcher`. If you want to use another dispatcher, just config the implicit value of `redisDispatcher`:

```scala
implicit val redisDispatcher = RedisDispatcher("akka.actor.default-dispatcher")
```

### ByteStringSerializer ByteStringDeserializer ByteStringFormatter

[ByteStringSerializer](http://etaty.github.io/rediscala/latest/api/index.html#redis.ByteStringSerializer)

[ByteStringDeserializer](http://etaty.github.io/rediscala/latest/api/index.html#redis.ByteStringDeserializer)

[ByteStringFormatter](http://etaty.github.io/rediscala/latest/api/index.html#redis.ByteStringFormatter)

```scala
case class DumbClass(s1: String, s2: String)

object DumbClass {
  implicit val byteStringFormatter = new ByteStringFormatter[DumbClass] {
    def serialize(data: DumbClass): ByteString = {
      //...
    }

    def deserialize(bs: ByteString): DumbClass = {
      //...
    }
  }
}
//...

  val dumb = DumbClass("s1", "s2")

  val r = for {
    set <- redis.set("dumbKey", dumb)
    getDumbOpt <- redis.get[DumbClass]("dumbKey")
  } yield {
    getDumbOpt.map(getDumb => {
      assert(getDumb == dumb)
      println(getDumb)
    })
  }
```

Full example: [ExampleByteStringFormatter](https://github.com/etaty/rediscala-demo/blob/master/src/main/scala/ExampleByteStringFormatter.scala)

### Scaladoc
[Rediscala scaladoc API (version 1.8)](https://oss.sonatype.org/service/local/repositories/releases/archive/com/github/etaty/rediscala_2.11/1.8.0/rediscala_2.11-1.8.0-javadoc.jar/!/index.html#package)

[Rediscala scaladoc API (version 1.7)](https://oss.sonatype.org/service/local/repositories/releases/archive/com/github/etaty/rediscala_2.11/1.7.0/rediscala_2.11-1.7.0-javadoc.jar/!/index.html#package)

[Rediscala scaladoc API (version 1.6)](http://etaty.github.io/rediscala/1.6.0/api/index.html#package)

[Rediscala scaladoc API (version 1.5)](http://etaty.github.io/rediscala/1.5.0/api/index.html#package)

[Rediscala scaladoc API (version 1.4)](http://etaty.github.io/rediscala/1.4.0/api/index.html#package)

[Rediscala scaladoc API (version 1.3)](http://etaty.github.io/rediscala/1.3/api/index.html#package)

[Rediscala scaladoc API (version 1.2)](http://etaty.github.io/rediscala/1.2/api/index.html#package)

[Rediscala scaladoc API (version 1.1)](http://etaty.github.io/rediscala/1.1/api/index.html#package)

[Rediscala scaladoc API (version 1.0)](http://etaty.github.io/rediscala/1.0/api/index.html#package)

### Performance

More than 250 000 requests/second

* [benchmark result from scalameter](http://bit.ly/rediscalabench-1-1)
* [sources directory](https://github.com/etaty/rediscala/tree/master/src/benchmark/scala/redis/bench)

The hardware used is a macbook retina (Intel Core i7, 2.6 GHz, 4 cores, 8 threads, 8GB) running the sun/oracle jvm 1.6

You can run the bench with :

1. clone the repo `git clone git@github.com:etaty/rediscala.git`
2. run `sbt bench:test`
3. open the bench report `rediscala/tmp/report/index.html`

