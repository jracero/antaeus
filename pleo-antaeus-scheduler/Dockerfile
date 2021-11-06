FROM ubuntu:latest

# Install cron
RUN apt-get update && apt-get -y install cron && apt-get -y install curl

# Create the log file to be able to run tail
RUN touch /var/log/cron.log

# Setup cron job
RUN (crontab -l ; echo "* * * * * /usr/bin/curl --request POST http://localhost:7000/rest/v1/charge > /var/log/cron.log 2>&1") | crontab

# Run the command on container startup
CMD cron && tail -f /var/log/cron.log