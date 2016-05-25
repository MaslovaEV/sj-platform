#!/bin/bash

echo MONGO_HOST: ${MONGO_HOST?Please provide MONGO_HOST}

if [ -n "$REST_PORT" ]; then echo "REST_PORT env var is voided" && exit; fi

if [ -n "$REST_PORT" ]; then echo "REST_PORT env var is voided" && exit; fi

if [ -z "$MONGO_PORT" ]
then
   export MONGO_PORT=27017
   echo "Using default MONGO_PORT 27017"
else
   echo "Using MONGO_PORT $MONGO_PORT"
fi

if [ -z "$REST_PORT" ]
then
   export REST_PORT=8080
   echo "REST is starting on default $REST_PORT port..."
else
   echo "REST is starting on $REST_PORT port..."
fi

# Cfg nginx proxy
sed -i 's/listen 8080/listen '"$REST_PORT"'/g' /etc/nginx/conf.d/rest-nginx.conf

# Cfg for scala REST
export CRUD_REST_PORT=3001
export CRUD_REST_HOST=0.0.0.0

echo "Running. See logs in /var/log/rest"
supervisord -n > /var/log/rest/supervisor.log 2>&1
