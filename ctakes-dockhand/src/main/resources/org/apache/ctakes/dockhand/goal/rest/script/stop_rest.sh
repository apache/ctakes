#!/bin/bash

docker stop my_ctakes_rest
docker rmi ctakes_tiny_rest
docker system prune
