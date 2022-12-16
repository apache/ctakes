@REM
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM   http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM

REM @ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION
set UIMA_HOME=c:\tools\apache-uima
SET CLASSPATH1=%UIMA_HOME%\lib\uima-core.jar;%UIMA_HOME%\lib\uima-cpe.jar;%UIMA_HOME%\lib\uima-tools.jar;%UIMA_HOME%\lib\uima-document-annotations.jar;%UIMA_HOME%\lib\uima-examples.jar;
SET CLASSPATH2=chunker/bin;ctakes-clinical-pipeline/bin;context dependent tokenizer/bin;core/bin;dictionary lookup/bin;document preprocessor/bin;LVG/bin;NE contexts/bin;POS tagger/bin;core/lib/log4j-1.2.8.jar;
SET CLASSPATH3=core/lib/jdom.jar;core/lib/lucene-core-3.0.2.jar;core/lib/opennlp-tools-1.4.0.jar;core/lib/maxent-2.5.0.jar;core/lib/OpenAI_FSM.jar;core/lib/trove.jar;Drug NER/bin;Drug NER/resources;
SET CLASSPATH4=LVG/lib/lvg2008dist.jar;document preprocessor/lib/xercesImpl.jar;document preprocessor/lib/xml-apis.jar;document preprocessor/lib/xmlParserAPIs.jar;smoking status/bin;smoking status/resources;
SET CLASSPATH5=chunker/resources;ctakes-clinical-pipeline/resources;context dependent tokenizer/resources;core/resources;dictionary lookup/resources;document preprocessor/resources;LVG/resources;POS tagger/resources;KnowtatorUtils/bin;KnowtatorUtils/resourses;utils/lib/knowtator.jar;
SET CLASSPATH6=utils/lib/looks-2.1.3.jar;utils/lib/protege.jar

FOR /F  %%G IN ('dir /b /on %1\Docset*') DO (

SET DOCSET=!DOCSET:=!%%G
		SET DOCSETNUM=!DOCSET:~7!
		IF "!DOCSET:~0,8!"=="DOCSET:=" SET DOCSET=!DOCSET:~8!
		IF "!DOCSET:~0,1!"=="=" SET DOCSET=!DOCSET:~1!
IF "!DOCSETNUM:~0,7!"=="=docset" SET DOCSETNUM=!DOCSETNUM:~7!
ECHO !DOCSETNUM!
ECHO !DOCSET!
java -cp "%CLASSPATH1%%CLASSPATH2%%CLASSPATH3%%CLASSPATH4%%CLASSPATH5%%CLASSPATH6%" -Xms256M -Xmx1424M edu.mayo.bmi.uima.knowtatorutils.apache.app.cTakesNeToKnowtatorSHARPAnnotation "%1\%%G\TemporalRelations_ProperCorefSchema_MergedSHARP.pprj" "Relations_May24Schema_Instance_40005"  "%1\%%G\doc!DOCSETNUM!"
)	
