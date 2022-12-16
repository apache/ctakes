#!/bin/bash

## Make sure there are environment variables for umls username and password
if [ -z $ctakes_umlsuser ] ; then
    echo "Environment variable ctakes_umlsuser must be defined"
    exit 1
fi

if [ -z $ctakes_umlspw ] ; then
    echo "Environment variable ctakes_umlspw must be defined"
    exit 1
fi

## Pass in environment variables
docker run -p 8080:8080 -e ctakes_umlsuser -e ctakes_umlspw -d ctakes-web-rest

