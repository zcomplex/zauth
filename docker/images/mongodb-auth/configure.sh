#!/bin/bash

# Admin User
ADM_USERNAME=${ADM_USERNAME:-"admin"}
ADM_PASSWORD=${ADM_PASSWORD:-"admin"}

# Application Database User
APP_DATABASE=${APP_DATABASE:-"app"}
APP_USERNAME=${APP_USERNAME:-"u_app"}
APP_PASSWORD=${APP_PASSWORD:-"p_app"}

# Wait for MongoDB to boot
until mongosh --eval "print('=> MongoDB ready!')" >/dev/null 2>&1; do
  sleep 1
done

# Replica set initialization
mongosh --eval "rs.initiate()"

# Waiting for primary
echo "=> Waiting for master..."
until mongosh --quiet --eval "rs.isMaster().ismaster" | grep true >/dev/null; do
  sleep 1
done

# Create the admin user
echo "=> Creating admin user with password"
mongosh <<EOF
use admin
db.createUser({
  user: "$ADM_USERNAME",
  pwd: "$ADM_PASSWORD",
  roles: [{ role: "root", db: "admin" }]
})
EOF

echo "=> Creating '$ADM_DATABASE' database and '$ADM_USERNAME' user"
mongosh -u "$ADM_USERNAME" -p "$ADM_PASSWORD" --authenticationDatabase admin <<EOF
use $APP_DATABASE
db.createUser({
  user: "$APP_USERNAME",
  pwd: "$APP_PASSWORD",
  roles: [{ role: "readWrite", db: "$APP_DATABASE" }]
})
EOF

sleep 1

touch /data/db/.configured

echo "=> Successfully configured"
