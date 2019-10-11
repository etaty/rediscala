#!/usr/bin/env bash
set -ex
wget http://download.redis.io/releases/redis-4.0.14.tar.gz
tar -xzvf redis-4.0.14.tar.gz
cd redis-4.0.14 && make
