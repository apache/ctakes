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

@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION
REM - The two required parameters are 1) fully qualified file containing file source and target files (e.g. R:\Dept\projects\Text\temporalRelations\docs\TemporalRelations_remapped_documents.txt)
REM - 2) relative path containing files to be exported to.
REM - For a given number of directories find clinics specific groups of documents; create correponding deidentified \
REM - document names based on mappings provided in 'R:\Dept\projects\Text\temporalRelations\docs\TemporalRelations_remapped_documents.txt
REM - NOTE: run from 'R:\Dept\projects\Text\temporalRelations\docs\tools' only
REM - **To process copying knowtator files automagic then put code here**
REM - For each dir returned find files that match group corresponding to 'Docset???' where '???' designates the group
REM	%MAT_PKG_HOME%\bin\MATEngine.cmd --task "SHARP Deidentification" --workflow Demo --steps nominate --input_file "r:\Dept\projects\Text\temporalRelations\tools\workspace\folders\completed\!NEXTFILE!" --input_file_type mat-json --output_file "R:\Dept\projects\Text\temporalRelations\nominate\!DOCSETPATH!\!DOCSETPATH:set=!\!TOFILE!" --output_file_type mat-json --replacer "clear -> clear"
cd ..\%2
FOR /F  %%G IN ('dir /b /on "Docset*"') DO (
	SET HOLDER="NEXTFILE:="
	SET HOLDTOFILE="ID001"
	SET HOLDDOC="DOCSET"
	SET RUNTRANSFORM=1
	SET RUNALTTRANSFORM=1
	FOR /f "tokens=3,4delims=," %%H IN (%1) DO (
		SET NEXTFILE=!NEXTFILE:%HOLDER%=!%%H
		SET TOFILE=!TOFILE:%HOLDER%=!%%I
		SET DOCSET=!DOCSET:%HOLDDOC%=!%%G
		SET DOCSETNUM=!DOCSET:~7!
		IF "!DOCSET:~0,8!"=="DOCSET:=" SET DOCSET=!DOCSET:~8!
		IF "!DOCSET:~0,1!"=="=" SET DOCSET=!DOCSET:~1!
		IF "!NEXTFILE:~0,1!"=="=" SET NEXTFILE=!NEXTFILE:~1!
		IF "!NEXTFILE:~0,10!"=="NEXTFILE:=" SET NEXTFILE=!NEXTFILE:~10!
		IF "!TOFILE:~0,1!"=="=" SET TOFILE=!TOFILE:~1!
		IF "!TOFILE:~0,8!"=="TOFILE:=" SET TOFILE=!TOFILE:~8!
		IF "!DOCSETNUM:~0,7!"=="=docset" SET DOCSETNUM=!DOCSETNUM:~7!
		SET COUNTER="!TOFILE:~2,3!"
		SET CNT1="!COUNTER:~1,1!"
		IF !CNT1!=="0" set COUNTER=!COUNTER:~2,-1!
		SET CNT2="!COUNTER:~0,1!"
		IF !CNT2!=="0" set COUNTER=!COUNTER:~1!
		ECHO !DOCSETNUM!
		ECHO !COUNTER!
		ECHO !DOCSET!
		IF ""!DOCSETNUM!""=="!COUNTER!" (
			ECHO !TOFILE!
			ECHO !DOCSET!\!DOCSET:set=!\!TOFILE!
			SET RUNALTTRANSFORM=0
			COPY r:\Dept\projects\Text\temporalRelations\tools\workspace\folders\completed\!NEXTFILE! R:\Dept\projects\Text\temporalRelations\%2\!DOCSET!
			REN R:\Dept\projects\Text\temporalRelations\%2\!DOCSET!\!NEXTFILE! !TOFILE!
		)
		IF "!DOCSETNUM!"=="!COUNTER!" (
			ECHO !TOFILE!
			ECHO !DOCSET!\!DOCSET:set=!\!TOFILE!
			SET RUNTRANSFORM=0
			COPY r:\Dept\projects\Text\temporalRelations\tools\workspace\folders\completed\!NEXTFILE! R:\Dept\projects\Text\temporalRelations\%2\!DOCSET!
			REN R:\Dept\projects\Text\temporalRelations\%2\!DOCSET!\!NEXTFILE! !TOFILE!
		)
		IF "!DOCSETNUM!" LSS "!COUNTER!" (
			IF "!RUNTRANSFORM!"=="0" (
				CALL ..\tools\createGroupDocs.bat !DOCSET! !DOCSET!\!DOCSET:set=! %2
				CALL ..\tools\createGroupTransform.bat !DOCSET! !DOCSET!\!DOCSET:set=! !NEXTFILE! %2
				SET RUNTRANSFORM=1)

		)
		IF "!DOCSETNUM!" GTR "!COUNTER!" (
			IF "!RUNALTTRANSFORM!"=="0" (
				CALL ..\tools\createGroupDocs.bat !DOCSET! !DOCSET!\!DOCSET:set=! %2
				CALL ..\tools\createGroupTransform.bat !DOCSET! !DOCSET!\!DOCSET:set=! !NEXTFILE! %2
				SET RUNALTTRANSFORM=1)

		)

		
		))	
cd ..\tools