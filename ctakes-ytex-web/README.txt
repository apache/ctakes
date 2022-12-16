ctakes-ytex-web is a web application that provides the following:
* searching the annotated corpus by concepts (implemented by queries to ytex tables)
* computing the semantic similarity of concept pairs
* provides Web & Restful services for computing semantic similarity

ctakes-ytex-web just relies on ctakes-ytex (and is independent of the 
rest of ctakes).  Although this produces a war file (as it should), we 
don't actually deliver a WAR file with the ctakes distro. We package and 
install a ctakes-ytex-web.jar (notice the extension) file with the classes from this project. 
We deliver the exploded web-app in the ctakes distro under desc/ctakes-ytex-web.  All 
non-web app specific resources (properties files, spring config files) are 
in ctakes-ytex project/resources directory of ctakes.
	
To build and run the web app run the following (command line/eclipse):
mvn clean install jetty:run 

This relies on you having correctly built and configured the ctakes-ytex.
