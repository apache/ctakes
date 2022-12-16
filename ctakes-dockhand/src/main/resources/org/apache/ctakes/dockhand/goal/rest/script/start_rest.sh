#!/bin/bash

if [ "$1" != "" ]; then
    umlsUser=$1
    set umlsUser=$1
    export umlsUser
fi

if [ "$2" != "" ]; then
    umlsPass=$2
    set umlsPass=$2
    export umlsPass
fi

## Make sure there are environment variables for umls username and password
if [ -z $umlsUser ] ; then
    echo "Environment variable umlsUser must be defined"
    exit 1
fi

if [ -z $umlsPass ] ; then
    echo "Environment variable umlsPass must be defined"
    exit 1
fi

## Pass in environment variables
docker run --name my_ctakes_rest --rm -d -p 8080:8080 -e umlsUser -e umlsPass ctakes_tiny_rest

