--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRCOC') AND type in (N'U'))
drop table $(db_schema).MRCOC
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRCOLS') AND type in (N'U'))
drop table $(db_schema).MRCOLS
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRCONSO') AND type in (N'U'))
drop table $(db_schema).MRCONSO
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRCUI') AND type in (N'U'))
drop table $(db_schema).MRCUI
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRCXT') AND type in (N'U'))
drop table $(db_schema).MRCXT
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRDEF') AND type in (N'U'))
drop table $(db_schema).MRDEF
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRDOC') AND type in (N'U'))
drop table $(db_schema).MRDOC
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRFILES') AND type in (N'U'))
drop table $(db_schema).MRFILES
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRHIER') AND type in (N'U'))
drop table $(db_schema).MRHIER
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRHIST') AND type in (N'U'))
drop table $(db_schema).MRHIST
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRMAP') AND type in (N'U'))
drop table $(db_schema).MRMAP
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRRANK') AND type in (N'U'))
drop table $(db_schema).MRRANK
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRREL') AND type in (N'U'))
drop table $(db_schema).MRREL
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRSAB') AND type in (N'U'))
drop table $(db_schema).MRSAB
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRSAT') AND type in (N'U'))
drop table $(db_schema).MRSAT
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRSMAP') AND type in (N'U'))
drop table $(db_schema).MRSMAP
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRSTY') AND type in (N'U'))
drop table $(db_schema).MRSTY
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXNS_ENG') AND type in (N'U'))
drop table $(db_schema).MRXNS_ENG
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXNW_ENG') AND type in (N'U'))
drop table $(db_schema).MRXNW_ENG
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRAUI') AND type in (N'U'))
drop table $(db_schema).MRAUI
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_BAQ') AND type in (N'U'))
drop table $(db_schema).MRXW_BAQ
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_CZE') AND type in (N'U'))
drop table $(db_schema).MRXW_CZE
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_DAN') AND type in (N'U'))
drop table $(db_schema).MRXW_DAN
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_DUT') AND type in (N'U'))
drop table $(db_schema).MRXW_DUT
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_ENG') AND type in (N'U'))
drop table $(db_schema).MRXW_ENG
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_FIN') AND type in (N'U'))
drop table $(db_schema).MRXW_FIN
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_FRE') AND type in (N'U'))
drop table $(db_schema).MRXW_FRE
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_GER') AND type in (N'U'))
drop table $(db_schema).MRXW_GER
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_HEB') AND type in (N'U'))
drop table $(db_schema).MRXW_HEB
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_HUN') AND type in (N'U'))
drop table $(db_schema).MRXW_HUN
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_ITA') AND type in (N'U'))
drop table $(db_schema).MRXW_ITA
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_JPN') AND type in (N'U'))
drop table $(db_schema).MRXW_JPN
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_KOR') AND type in (N'U'))
drop table $(db_schema).MRXW_KOR
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_LAV') AND type in (N'U'))
drop table $(db_schema).MRXW_LAV
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_NOR') AND type in (N'U'))
drop table $(db_schema).MRXW_NOR
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_POR') AND type in (N'U'))
drop table $(db_schema).MRXW_POR
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_RUS') AND type in (N'U'))
drop table $(db_schema).MRXW_RUS
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_SCR') AND type in (N'U'))
drop table $(db_schema).MRXW_SCR
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_SPA') AND type in (N'U'))
drop table $(db_schema).MRXW_SPA
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MRXW_SWE') AND type in (N'U'))
drop table $(db_schema).MRXW_SWE
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).AMBIGSUI') AND type in (N'U'))
drop table $(db_schema).AMBIGSUI
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).AMBIGLUI') AND type in (N'U'))
drop table $(db_schema).AMBIGLUI
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).DELETEDCUI') AND type in (N'U'))
drop table $(db_schema).DELETEDCUI
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).DELETEDLUI') AND type in (N'U'))
drop table $(db_schema).DELETEDLUI
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).DELETEDSUI') AND type in (N'U'))
drop table $(db_schema).DELETEDSUI
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MERGEDCUI') AND type in (N'U'))
drop table $(db_schema).MERGEDCUI
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).MERGEDLUI') AND type in (N'U'))
drop table $(db_schema).MERGEDLUI
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).SRDEF') AND type in (N'U'))
drop table $(db_schema).SRDEF
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).SRFIL') AND type in (N'U'))
drop table $(db_schema).SRFIL
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).SRFLD') AND type in (N'U'))
drop table $(db_schema).SRFLD
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).SRSTR') AND type in (N'U'))
drop table $(db_schema).SRSTR
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).SRSTRE1') AND type in (N'U'))
drop table $(db_schema).SRSTRE1
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).SRSTRE2') AND type in (N'U'))
drop table $(db_schema).SRSTRE2
;
