FROM openjdk:8-alpine
  
RUN apk update && apk add ca-certificates openssl wget unzip subversion maven

## Download apache-tomcat and extract:
RUN wget http://mirror.cc.columbia.edu/pub/software/apache/tomcat/tomcat-9/v9.0.14/bin/apache-tomcat-9.0.14.zip
RUN unzip apache-tomcat-9.0.14.zip

## Check out version of ctakes with best working web-rest module
## Then compile with maven
RUN svn co https://svn.apache.org/repos/asf/ctakes/trunk@1850060 ctakes

## Copy hsql dictionary descriptor into right location
RUN wget -q -O dict.zip https://downloads.sourceforge.net/project/ctakesresources/sno_rx_16ab.zip
RUN mkdir -p /ctakes/resources/org/apache/ctakes/dictionary/lookup/fast/
RUN unzip -o dict.zip -d /ctakes/resources/org/apache/ctakes/dictionary/lookup/fast/

COPY customDictionary.xml /ctakes/ctakes-web-rest/src/main/resources/org/apache/ctakes/dictionary/lookup/fast/
COPY pom.xml /ctakes

# This version of the default piper comments out a memory-intensive negation module. If you need to run
# negation detection, then comment out this line.
COPY Default.piper /ctakes/ctakes-web-rest/src/main/resources/pipers/

WORKDIR /ctakes
RUN mvn compile -DskipTests
RUN mvn install -pl '!ctakes-distribution'  -DskipTests

WORKDIR /
RUN cp /ctakes/ctakes-web-rest/target/ctakes-web-rest.war /apache-tomcat-9.0.14/webapps/

ENV TOMCAT_HOME=/apache-tomcat-9.0.14
ENV CTAKES_HOME=/ctakes

EXPOSE 8080


WORKDIR $TOMCAT_HOME
RUN chmod u+x bin/*.sh

CMD bin/catalina.sh run
