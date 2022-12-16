@ECHO OFF
::
:: Licensed to the Apache Software Foundation (ASF) under one
:: or more contributor license agreements.  See the NOTICE file
:: distributed with this work for additional information
:: regarding copyright ownership.  The ASF licenses this file
:: to you under the Apache License, Version 2.0 (the
:: "License"); you may not use this file except in compliance
:: with the License.  You may obtain a copy of the License at
::
::   http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing,
:: software distributed under the License is distributed on an
:: "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
:: KIND, either express or implied.  See the License for the
:: specific language governing permissions and limitations
:: under the License.
::
::
::   Runs the default clinical pipeline on files in the input directory specified by -i {directory}
::   Writes .xmi files to the output directory specified by --xmiOut {directory}
::   Uses UMLS credentials specified by --user {username} --pass {password}
::   Can also use a custom dictionary with -l {dictionaryConfigFile}
::
::
:: Requires JAVA JDK 1.8+
::

@REM Guess CTAKES_HOME if not defined
set CURRENT_DIR=%cd%
if not "%CTAKES_HOME%" == "" goto gotHome
set CTAKES_HOME=%CURRENT_DIR%
if exist "%CTAKES_HOME%\bin\runctakesCVD.bat" goto okHome
cd ..
set CTAKES_HOME=%cd%

:gotHome
if exist "%CTAKES_HOME%\bin\runctakesCVD.bat" goto okHome
echo The CTAKES_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end

:okHome
@set PATH=%PATH%;%CTAKES_HOME%\lib\auth\x64
@REM use JAVA_HOME if set
if exist "%JAVA_HOME%\bin\java.exe" set PATH=%JAVA_HOME%\bin;%PATH%

cd %CTAKES_HOME%
set CLASS_PATH=%CTAKES_HOME%\desc\;%CTAKES_HOME%\resources\;%CTAKES_HOME%\lib\*
set LOG4J_PARM=-Dlog4j.configuration="file:\%CTAKES_HOME%\config\log4j.xml"
set PIPE_RUNNER=org.apache.ctakes.core.pipeline.PiperFileRunner
set FAST_PIPER=resources\org\apache\ctakes\clinical\pipeline\DefaultFastPipeline.piper
java -cp "%CLASS_PATH%" %LOG4J_PARM% -Xms512M -Xmx3g %PIPE_RUNNER% -p %FAST_PIPER% %*
cd %CURRENT_DIR%

:end
