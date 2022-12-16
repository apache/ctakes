FROM openjdk:8-alpine

RUN apk update && apk add ca-certificates openssl wget unzip maven

## Download apache-tomcat and extract:
RUN wget https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.14/bin/apache-tomcat-9.0.14.zip \
&& unzip apache-tomcat-9.0.14.zip && rm apache-tomcat-9.0.14.zip


RUN mkdir -p /ctakes_src
COPY pom.xml /ctakes_src/
COPY log4j.xml /ctakes_src/
COPY LICENSE /ctakes_src/
COPY NOTICE /ctakes_src/

COPY webapp /ctakes_src/src/main/webapp

WORKDIR /ctakes_src

RUN mvn compile -DskipTests
#  Don't copy the piper file to resources until mvn compile has created the directories.
#  Then package can put it in the WAR.  The rest Server calls TinyRestPipeline.
COPY TinyRestPipeline.piper /ctakes_src/src/main/resources/
COPY DockhandPipeline.piper /ctakes_src/src/main/resources/

RUN mvn package

# Move the war file and copy the dictionary.
RUN mv /ctakes_src/ctakes_tiny_rest.war /apache-tomcat-9.0.14/webapps/
RUN cp -fr /ctakes_src/resources /apache-tomcat-9.0.14/

# A little cleanup.
RUN rm -fr /ctakes_src

# Expose port 8080 for html access.
EXPOSE 8080

ENV TOMCAT_HOME=/apache-tomcat-9.0.14
WORKDIR $TOMCAT_HOME
RUN chmod u+x bin/*.sh

ENV JAVA_OPTS="-Xmx6G"
CMD bin/catalina.sh run
