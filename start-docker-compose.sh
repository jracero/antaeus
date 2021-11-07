#!/bin/sh

# stop containers if they are already running
docker stop antaeus_app
docker rm antaeus_app
docker stop antaeus_scheduler
docker rm antaeus_scheduler
docker-compose up
