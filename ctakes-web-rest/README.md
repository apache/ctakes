The ctakes-web-rest module builds a standalone product, Apache cTaKES Web REST.  

ctakes-web-rest is not compiled or built when the main Apache cTAKES code is built with maven.   
ctakes-web-rest is not contained within the main Apache cTAKES binary distributable zipped files.   

You must build the Apache cTAKES Web REST product alone.
Doing so will create its own self-contained Web Application Resource (aka Web ARchive) (WAR) file.  
The WAR file contains a large segment of the main Apache cTAKES binary distributable. 

If you are running maven by the command line then you must run in the ctakes-web-rest directory or direct maven to the ctakes-web-rest pom.   
If you are running maven in an IDE, then you must add ctakes-web-rest as its own maven project and execute maven commands on that project.   
Building the main ctakes project will not build ctakes-web-rest.

This configuration exists because:
- building ctakes-web-rest with the main ctakes maven project adds to the build time.
- building ctakes-web-rest with the main ctakes maven project adds to the disk footprint of the build.  i.e. it has its own huge target/ directory.
- including ctakes-web-rest in the main Apache cTAKES binary distributable essentially puts two copies of cTAKES in the distributable.
- building ctakes-web-rest with the main ctakes maven project will NOT put any changed code or resources in the war. 
Unless you mvn install.  Making ctakes-web-rest a separate build reinforces the separation.

This module performs natural language processing of input payload using REST API endpoint and extracts out clicnical information in JSON format.

<details>
<summary>Installation</summary>

1) Build this ctakes-web-rest module.
2) Modify ```src\main\resources\org\apache\ctakes\dictionary\lookup\fast\customDictionary.xml```
to refer to the respective database where dictionary is loaded or use UMLS database.
3) Build ctakes-web-rest module and deploy ```ctakes-web-rest.war``` available under ```target/``` folder in Apache Tomcat.
4) Access the following URL to perform text analysis using cTAKES web application:
    ```http://<host-name>:<port>/ctakes-web-rest/index.jsp```
5) Access the following REST API endpoint to perform text analysis using default pipeline:
    ```http://<host-name>:<port>/ctakes-web-rest/service/analyze?pipeline=Default```
6) Access the following REST API endpoint to perform text analysis using full pipeline:
    ```http://<host-name>:<port>/ctakes-web-rest/service/analyze?pipeline=Full```
</details>