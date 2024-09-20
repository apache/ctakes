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

#   Runs the default clinical pipeline with provided parameters.
#   Required parameters are:
#   -i , --inputDir {inputDirectory}
#   -o , --outputDir {outputDirectory}
#   --key {umlsKey}

#   Optional standard parameters are:
#   -s , --subDir {subDirectory}  (for i/o)
#   --xmiOut {xmiOutputDirectory} (if different from -o)
#   -l , --lookupXml {dictionaryConfigFile} (fast only)
#   -? , --help

# Requires Java 17


# Sets up environment for cTAKES
. ${HOME}/setenv.sh

cd $CTAKES_HOME

java -cp "$CLASS_PATH" -Xms512M -Xmx3g $PIPE_RUNNER -p $FAST_PIPER "$@"
