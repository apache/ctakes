#!/bin/bash

if [ "$1" != "" ]; then
    umlsKey=$1
    set umlsKey=$1
    export umlsKey
fi

## Make sure there are environment variables for umls username and password
if [ -z $umlsKey ] ; then
    echo "Environment variable umlsKey must be defined"
    exit 1
fi

## Pass in environment variables
docker run --name my_ctakes_rest --rm -d -p 8080:8080 -e umlsKey ctakes_tiny_rest

