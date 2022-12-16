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

# Requires JAVA JDK 1.6+
# If you plan to use the UMLS Resources, set/export env variables
# export ctakes.umlsuser=[username], ctakes.umlspw=[password]
# or add the properties
# -Dctakes.umlsuser=[username] -Dctakes.umlspw=[password]

# You can also pass in the name of the XML descriptor to auto load as an arugement
# -desc $CTAKES_HOME/ctakes-clinical-pipeline/desc/AggregatePlaintextUMLSProcessor.xml

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
java -cp $CTAKES_HOME/desc/:$CTAKES_HOME/resources/:$CTAKES_HOME/lib/* -Dlog4j.configuration=file:$CTAKES_HOME/config/log4j.xml -Xms512M -Xmx3g org.apache.uima.tools.cvd.CVD "$@"
