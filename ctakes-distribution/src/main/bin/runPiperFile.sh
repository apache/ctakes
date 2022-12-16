#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
#   Runs the pipeline in the piper file specified by -p (piperfile)
#   with any other provided parameters.  Standard parameters are:
#     -i , --inputDir {inputDirectory}
#     -o , --outputDir {outputDirectory}
#     -s , --subDir {subDirectory}  (for i/o)
#     --xmiOut {xmiOutputDirectory} (if different from -o)
#     -l , --lookupXml {dictionaryConfigFile} (fast only)
#     --user {umlsUsername}
#     --pass {umlsPassword}
#     -? , --help
#
#   Other parameters may be declared in the piper file using the cli command:
#     cli {parameterName}={singleCharacter}
#   For instance, for declaration of ParagraphAnnotator path to regex file optional parameter PARAGRAPH_TYPES_PATH,
#   in the custom piper file add the line:
#     cli PARAGRAPH_TYPES_PATH=t
#   and when executing this script use:
#      runPiperFile -p path/to/my/custom.piper -t path/to/my/custom.bsv  ...
#
# Requires JAVA JDK 1.8+
#

PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
PRGDIR=`dirname "$PRG"`

# Only set CTAKES_HOME if not already set
[ -z "$CTAKES_HOME" ] && CTAKES_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`

cd $CTAKES_HOME
java -cp $CTAKES_HOME/desc/:$CTAKES_HOME/resources/:$CTAKES_HOME/lib/* -Dlog4j.configuration=file:$CTAKES_HOME/config/log4j.xml -Xms512M -Xmx3g org.apache.ctakes.core.pipeline.PiperFileRunner "$@"
