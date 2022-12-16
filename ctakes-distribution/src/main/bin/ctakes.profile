#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"): you may not use this file except in compliance
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

# Requires JAVA JDK 1.6+
# If you plan to use the UMLS Resources, set/export env variables
# export ctakes.umlsuser=[username], ctakes.umlspw=[password]
# or add the properties
# -Dctakes.umlsuser=[username] -Dctakes.umlspw=[password]

# change CTAKES_HOME to match your environment
CTAKES_HOME=${HOME}/apache-ctakes-3.2.1-SNAPSHOT
export CTAKES_HOME

ANT_CP=${CTAKES_HOME}/lib/ant-1.9.2.jar:${CTAKES_HOME}/lib/ant-launcher-1.9.2.jar:${CTAKES_HOME}/lib/ant-contrib-1.0b3.jar
export ANT_CP

CLASSPATH="${CTAKES_HOME}/desc/:${CTAKES_HOME}/resources/:${CTAKES_HOME}/lib/*"
export CLASSPATH
