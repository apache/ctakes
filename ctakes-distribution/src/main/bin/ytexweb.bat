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

@rem simple script to start jetty with the desc\ctakes-ytex-web web app
@rem YOU MUST use a java JDK, not JRE.
@rem typical windows Paths include the JRE, not the JDK
@rem do one of the following:
@rem * change java in this script to the full path of java from the JDK
@rem * or modify setenv.bat to put the JDK bin directory at the beginning of the path
@rem * or modify the PATH system environment variable and put the JDK bin directory at the beginning
@setlocal
@call %~dp0setenv.bat
java -cp "%CLASSPATH%" -Dlog4j.configuration="file:\%CTAKES_HOME%\config\log4j.xml" -XX:MaxPermSize=128m -Xmx512m org.eclipse.jetty.runner.Runner "%CTAKES_HOME%\desc\ctakes-ytex-web"
@endlocal