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


@rem determine CTAKES_HOME - set it to the parent of the directory in which this file is located
@set CURDIR=%cd%
@set BINDIR=%~dp0
@rem change to CTAKES_HOME dir and save the directory
@cd %BINDIR%..
@set CTAKES_HOME=%cd%
@rem change back to the original directory
@cd %CURDIR%

@rem -------------------------------------------
@rem customize these variables to match your environment
@rem -------------------------------------------

@rem add 64-bit MS SQL Server authentication library to path
@rem modify this accordingly if you are using the 32-bit jdk
@set PATH=%PATH%;%CTAKES_HOME%\lib\auth\x64

@rem -------------------------------------------
@rem end customizations.  The following is environment-independent
@rem -------------------------------------------

@set ANT_CP=%CTAKES_HOME%\lib\ant-1.9.2.jar;%CTAKES_HOME%\lib\ant-launcher-1.9.2.jar;%CTAKES_HOME%\lib\ant-contrib-1.0b3.jar
@set CLASSPATH="%CTAKES_HOME%\desc\;%CTAKES_HOME%\resources\;%CTAKES_HOME%\lib\*"
