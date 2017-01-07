#!/usr/bin/env bash
set -ex
wget http://download.redis.io/releases/redis-3.2.0.tar.gz
tar -xzvf redis-3.2.0.tar.gz
cd redis-3.2.0 && make