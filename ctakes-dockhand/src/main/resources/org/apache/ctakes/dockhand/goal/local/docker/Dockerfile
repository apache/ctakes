FROM openjdk:8-alpine

RUN apk update && apk add ca-certificates openssl wget unzip maven

RUN mkdir -p /ctakes_src
COPY pom.xml /ctakes_src/
COPY DockhandPipeline.piper /ctakes_src/
COPY log4j.xml /ctakes_src/
COPY LICENSE /ctakes_src/
COPY NOTICE /ctakes_src/
# copy RunPipeline scripts

WORKDIR /ctakes_src

RUN mvn package

WORKDIR /ctakes_src/apache-ctakes-6.0.0-SNAPSHOT

#  TODO -- create RunPipeline scripts

CMD RunPipeline.sh
