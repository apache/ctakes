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


ECHO This needs to be customized for the user's installation.  Placing it in the ctakes-chunker/ root directory is best.

cd /d c:

ECHO Replace "your_pipeline_installation_home" in the following lines
cd  \your_pipeline_installation_home\chunker
cd  /your_pipeline_installation_home/chunker


pause
java  -Xms1024M -Xmx1300M    -cp "../ctakes-core/lib/opennlp-tools-1.4.0.jar;../ctakes-core/lib/trove.jar;../ctakes-core/lib/maxent-2.5.0.jar;%CLASSPATH%"  opennlp.tools.chunker.ChunkerME  "/EraseME/chunk/corpus.opennlp.chunks"  "/EraseME/chunk/corpus.chunk.model.bin.gz"
pause
 



