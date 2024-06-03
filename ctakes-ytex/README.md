Contains the following components:
* Semantic Similarity - compute the similarity between concepts
* Word Sense Disambiguation - use semantic similarity to disambiguate terms that have multiple meanings
* Bag-of-Words exporter - export sparse matrices from a database
* YTEX Database setup scripts
* Java Data Loader - simple utility for loading delimited files into database tables

<details>
<summary>Getting started</summary>

* Create ytex.properties: `create src\main\resources\org\apache\ctakes\ytex\ytex.properties` (see examples in ctakes-ytex/src/main/resources/org/apache/ctakes/ytex)
* Add jdbc drivers to maven repo: if you are using ms sql server / or oracle, add the jdbc driver(s) to your local maven repo (see comments in dependencies section of pom.xml)
* Add ms sql auth dlls to path: if you are using ms sql server with integrated auth, make sure the ms sql jdbc auth directory is in your system PATH (sqljdbc_4.0\enu\auth\x64)
* Unzip ctakes-ytex-resources.zip: extract to  ctakes-ytex-res/src/main
* run the maven build from the command line for ctakes-ytex, ctakes-ytex-uima projects.  From the ctakes root directory:
`mvn -pl ctakes-ytex,ctakes-ytex-uima -DskipTests`
* set up your database: Open a shell/command prompt, set the CTAKES_HOME variable to be the checkout directory of ctakes (i.e. the parent of this file's directory), and run the ant script:

Windows

```
set CTAKES_HOME=xxx
cd %CTAKES_HOME%\ctakes-ytex\scripts\data
ant -Dconfig.local=..\..\target\classes all > ..\..\target\build.out 2>&1
```

Linux
```
CTAKES_HOME=xxx
export CTAKES_HOME
cd ${CTAKES_HOME}/ctakes-ytex/scripts/data
ant -Dconfig.local=../../target/classes all > ../../target/build.out 2>&1
```

You should be all set now.
</details>

<details>
<summary>Developing</summary>
We have currently tested mysql and ms sql server, oracle is pending.  HSQL is used only for unit testing.  

YTEX is driven by a property file, ytex.properties.  The property file used when running ytex java programs 
(aside from junit tests) is src\main\resources\org\apache\ctakes\ytex\ytex.properties.  Refer to the example
properties files under /ctakes-ytex/src/main/resources/org/apache/ctakes/ytex

YTEX relies on some config files generated from templates via an ant build script.  These config files are 
generated automatically by the maven build (which runs the ant build script).  
The maven eclipse integration is a bit buggy; you should run the maven build from the command line so that 
things work correctly.

For development, you must set up a database.  There are 2 types of database setups, depending on if you have a UMLS
installation (and you've configured ytex to use it):
* With UMLS - YTEX will generate a dictionary lookup table from your UMLS database.  This requires tokenizing every 
string in MRCONSO which will take a while.  If you have UMLS installed and want to use it, configure the 
umls.schema/umls.catalog properties in ytex.properties.
* Without UMLS - YTEX will load a pre-fabricated dictionary lookup table generated from the UMLS 2013AA.  This is included
in the ctakes-resources zip from sourceforge.  You have to copy v_snomed_fword_lookup.txt to ctakes-ytex\scripts\data\umls\.
</details>

<details>
<summary>Testing</summary>

YTEX is dependent upon a database which is set up by the ant script scripts\data\build.xml

Note that all ytex-related tables in these databases will be dropped and recreated every time 
the maven build is run (don't put data you want to keep there).
  
Maven runs the ant script prior to running the tests. 
By default, we set up an hsqldb database for testing in the TEMP dir.
Override this by:
 * passing -Ddb.type=[mysql|mssql|orcl] to the maven command line.  If you do that,
 we will use the \src\test\resources\org\apache\ctakes\ytex\ytex.${db.type}.properties
 which point to the ytex_test schema (mysql/orcl) or catalog (mssql).
 * or dropping your own ytex.properties in 
 \src\test\resources\org\apache\ctakes\ytex.
 </details>

<details>
<summary>Creating the ctakes-ytex-resources.zip </summary>
See scripts/build-nonasf.xml
</details>



