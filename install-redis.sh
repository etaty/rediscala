#!/usr/bin/env bash
set -ex
wget http://download.redis.io/releases/redis-5.0.5.tar.gz
tar -xzvf redis-5.0.5.tar.gz
cd redis-5.0.5 && make
