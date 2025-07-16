#!/bin/bash
set -m

if [ ! -f /data/db/.configured ]; then
  mongod --replSet rs0 --keyFile /etc/keyfile --bind_ip_all --fork --logpath /var/log/mongodb.log &

  sleep 5

  # Security, database and replica set initialization
  /configure.sh

  mongod --shutdown
fi

exec mongod --replSet rs0 --keyFile /etc/keyfile --auth --bind_ip_all
