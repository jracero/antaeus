#!/bin/sh

# stop containers if they are already running
docker stop antaeus_app
docker rm antaeus_app
docker stop antaeus_scheduler
docker rm antaeus_scheduler

./gradlew clean build
docker build . -t antaeus

cd pleo-antaeus-scheduler
docker build . -t antaeus-scheduler

#Starting containers
echo "starting antaeus app..."
docker run -d --name antaeus_app -p 7000:7000 antaeus
sleep 120
echo "starting antaeus scheduler..."
docker run --name antaeus_scheduler --network=host antaeus-scheduler