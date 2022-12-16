#! /bin/sh
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

# simple script to start jetty with the desc/ctakes-ytex-web web app
. ${HOME}/ctakes.profile
java -cp ${CLASSPATH} -Dlog4j.configuration=file:${CTAKES_HOME}/config/log4j.xml -XX:MaxPermSize=128m -Xmx512m org.eclipse.jetty.runner.Runner ${CTAKES_HOME}/desc/ctakes-ytex-web

