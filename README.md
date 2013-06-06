rediscala [![Build Status](https://travis-ci.org/etaty/rediscala.png)](https://travis-ci.org/etaty/rediscala)
=========

A Redis driver for Scala (2.10+) and (AKKA 2.2+)

Always wrap replies in Futures.

Use Pipelining. Blocking command are moved into their own connection.

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
  * Maybe replies should be processed in their own thread-actor (benchmark needed)
* docs (as always :p )
