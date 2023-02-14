FROM openjdk:8-alpine

## This Dockerfile checks out and builds all of ctakes.  Is there a reason for this?
## If you just checked in some fresh code and can't wait for a jenkins build then ok.
## Otherwise you can just check out the ctakes-web-rest directory and build it for the war.
## You can also add ctakes-web-rest as a dependency in a -dummy- pom with an output directory for the war.
## If you do have brand new code then another tactic is a copy of local source into the container, then building.
## That is more useful for developers who are not ctakes committers.
## Consider offering multiple dockerfiles.

RUN apk update && \
    apk add --no-cache git openssh ca-certificates openssl wget unzip subversion maven


## Download apache-tomcat and extract:
RUN wget http://mirror.cc.columbia.edu/pub/software/apache/tomcat/tomcat-9/v9.0.14/bin/apache-tomcat-9.0.14.zip && \
    unzip apache-tomcat-9.0.14.zip

## Check out version of ctakes with best working web-rest module
RUN git clone https://github.com/apache/ctakes.git

## Copy hsql dictionary descriptor into right location
RUN mkdir -p /ctakes/resources/org/apache/ctakes/dictionary/lookup/fast/ && \
    wget -q -O dict.zip https://downloads.sourceforge.net/project/ctakesresources/sno_rx_16ab.zip && \
    unzip -o dict.zip -d /ctakes/resources/org/apache/ctakes/dictionary/lookup/fast/

COPY customDictionary.xml /ctakes/ctakes-web-rest/src/main/resources/org/apache/ctakes/dictionary/lookup/fast/

# This version of the default piper comments out a memory-intensive negation module. If you need to run
# negation detection, then comment out this line.
COPY Default.piper /ctakes/ctakes-web-rest/src/main/resources/pipers/
COPY TinyRestPipeline.piper /ctakes/ctakes-web-rest/src/main/resources/pipers/

WORKDIR /ctakes
#     There is a profile to skip the building of zip distributables and another to build the web-rest war.
RUN mvn package -P no-zips-build,web-rest-build -DskipTests

WORKDIR /
RUN mv /ctakes/ctakes-web-rest/target/ctakes-web-rest.war /apache-tomcat-9.0.14/webapps/

ENV TOMCAT_HOME=/apache-tomcat-9.0.14
ENV CTAKES_HOME=/ctakes

EXPOSE 8080


WORKDIR $TOMCAT_HOME
RUN chmod u+x bin/*.sh

CMD bin/catalina.sh run
