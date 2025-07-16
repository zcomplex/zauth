#!/bin/bash

openssl rand -base64 756 > keyfile
chmod 400 keyfile

docker build -t mongodb-auth .
