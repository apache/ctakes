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

CREATE TABLE  $(db_schema).MRCOC (
    CUI1	char(8) NOT NULL,
    AUI1	varchar(9) NOT NULL,
    CUI2	char(8),
    AUI2	varchar(9),
    SAB	varchar(20) NOT NULL,
    COT	varchar(3) NOT NULL,
    COF	int ,
    COA	varchar(300),
    CVF	int 
);


CREATE TABLE  $(db_schema).MRCOLS (
    COL	varchar(20),
    DES	varchar(200),
    REF	varchar(20),
    MIN	int ,
    AV	numeric(5,2),
    MAX	int ,
    FIL	varchar(50),
    DTY	varchar(20)
);


CREATE TABLE  $(db_schema).MRCONSO (
    CUI	char(8) NOT NULL,
    LAT	char(3) NOT NULL,
    TS	char(1) NOT NULL,
    LUI	varchar(10) NOT NULL,
    STT	varchar(3) NOT NULL,
    SUI	varchar(10) NOT NULL,
    ISPREF	char(1) NOT NULL,
    AUI	varchar(9) NOT NULL,
    SAUI	varchar(50),
    SCUI	varchar(50),
    SDUI	varchar(50),
    SAB	varchar(20) NOT NULL,
    TTY	varchar(20) NOT NULL,
    CODE	varchar(50) NOT NULL,
    STR	nvarchar(max) NOT NULL,
    SRL	int  NOT NULL,
    SUPPRESS	char(1) NOT NULL,
    CVF	int 
);


CREATE TABLE  $(db_schema).MRCUI (
    CUI1	char(8) NOT NULL,
    VER	varchar(10) NOT NULL,
    REL	varchar(4) NOT NULL,
    RELA	varchar(100),
    MAPREASON	nvarchar(4000),
    CUI2	char(8),
    MAPIN	char(1)
);


CREATE TABLE  $(db_schema).MRCXT (
    CUI	char(8),
    SUI	varchar(10),
    AUI	varchar(9),
    SAB	varchar(20),
    CODE	varchar(50),
    CXN	int ,
    CXL	char(3),
    RANK	int ,
    CXS	varchar(3000),
    CUI2	char(8),
    AUI2	varchar(9),
    HCD	varchar(50),
    RELA	varchar(100),
    XC	varchar(1),
    CVF	int 
);


CREATE TABLE  $(db_schema).MRDEF (
    CUI	char(8) NOT NULL,
    AUI	varchar(9) NOT NULL,
    ATUI	varchar(11) NOT NULL,
    SATUI	varchar(50),
    SAB	varchar(20) NOT NULL,
    DEF	nvarchar(4000) NOT NULL,
    SUPPRESS	char(1) NOT NULL,
    CVF	int 
);


CREATE TABLE  $(db_schema).MRDOC (
    DOCKEY	varchar(50) NOT NULL,
    VALUE	varchar(200),
    TYPE	varchar(50) NOT NULL,
    EXPL	varchar(max)
);


CREATE TABLE  $(db_schema).MRFILES (
    FIL	varchar(50),
    DES	varchar(200),
    FMT	varchar(300),
    CLS	int ,
    RWS	int ,
    BTS	bigint
);


CREATE TABLE  $(db_schema).MRHIER (
    CUI	char(8) NOT NULL,
    AUI	varchar(9) NOT NULL,
    CXN	int  NOT NULL,
    PAUI	varchar(10),
    SAB	varchar(20) NOT NULL,
    RELA	varchar(100),
    PTR	varchar(1000),
    HCD	varchar(50),
    CVF	int 
);


CREATE TABLE  $(db_schema).MRHIST (
    CUI	char(8),
    SOURCEUI	varchar(50),
    SAB	varchar(20),
    SVER	varchar(20),
    CHANGETYPE	nvarchar(1000),
    CHANGEKEY	nvarchar(1000),
    CHANGEVAL	nvarchar(1000),
    REASON	nvarchar(1000),
    CVF	int 
);


CREATE TABLE  $(db_schema).MRMAP (
    MAPSETCUI	char(8) NOT NULL,
    MAPSETSAB	varchar(20) NOT NULL,
    MAPSUBSETID	varchar(10),
    MAPRANK	int ,
    MAPID	varchar(50) NOT NULL,
    MAPSID	varchar(50),
    FROMID	varchar(50) NOT NULL,
    FROMSID	varchar(50),
    FROMEXPR	nvarchar(4000) NOT NULL,
    FROMTYPE	varchar(50) NOT NULL,
    FROMRULE	nvarchar(4000),
    FROMRES	varchar(4000),
    REL	varchar(4) NOT NULL,
    RELA	varchar(100),
    TOID	varchar(50),
    TOSID	varchar(50),
    TOEXPR	nvarchar(4000),
    TOTYPE	varchar(50),
    TORULE	nvarchar(4000),
    TORES	nvarchar(4000),
    MAPRULE	nvarchar(4000),
    MAPRES	nvarchar(4000),
    MAPTYPE	varchar(50),
    MAPATN	varchar(20),
    MAPATV	nvarchar(4000),
    CVF	int 
);


CREATE TABLE  $(db_schema).MRRANK (
    RANK	int  NOT NULL,
    SAB	varchar(20) NOT NULL,
    TTY	varchar(20) NOT NULL,
    SUPPRESS	char(1) NOT NULL
);


CREATE TABLE  $(db_schema).MRREL (
    CUI1	char(8) NOT NULL,
    AUI1	varchar(9),
    STYPE1	varchar(50) NOT NULL,
    REL	varchar(4) NOT NULL,
    CUI2	char(8) NOT NULL,
    AUI2	varchar(9),
    STYPE2	varchar(50) NOT NULL,
    RELA	varchar(100),
    RUI	varchar(10) NOT NULL,
    SRUI	varchar(50),
    SAB	varchar(20) NOT NULL,
    SL	varchar(20) NOT NULL,
    RG	varchar(10),
    DIR	varchar(1),
    SUPPRESS	char(1) NOT NULL,
    CVF	int 
);


CREATE TABLE  $(db_schema).MRSAB (
    VCUI	char(8),
    RCUI	char(8),
    VSAB	varchar(20) NOT NULL,
    RSAB	varchar(20) NOT NULL,
    SON	nvarchar(max) NOT NULL,
    SF	varchar(20) NOT NULL,
    SVER	varchar(20),
    VSTART	char(8),
    VEND	char(8),
    IMETA	varchar(10) NOT NULL,
    RMETA	varchar(10),
    SLC	nvarchar(max),
    SCC	nvarchar(max),
    SRL	int  NOT NULL,
    TFR	int ,
    CFR	int ,
    CXTY	varchar(50),
    TTYL	nvarchar(max),
    ATNL	nvarchar(max),
    LAT	char(3),
    CENC	varchar(20) NOT NULL,
    CURVER	char(1) NOT NULL,
    SABIN	char(1) NOT NULL,
    SSN	nvarchar(max) NOT NULL,
    SCIT	nvarchar(max) NOT NULL
);


CREATE TABLE  $(db_schema).MRSAT (
    CUI	char(8) NOT NULL,
    LUI	varchar(10),
    SUI	varchar(10),
    METAUI	varchar(50),
    STYPE	varchar(50) NOT NULL,
    CODE	varchar(50),
    ATUI	varchar(11) NOT NULL,
    SATUI	varchar(50),
    ATN	varchar(50) NOT NULL,
    SAB	varchar(20) NOT NULL,
    ATV	varchar(4000),
    SUPPRESS	char(1) NOT NULL,
    CVF	int 
);


CREATE TABLE  $(db_schema).MRSMAP (
    MAPSETCUI	char(8) NOT NULL,
    MAPSETSAB	varchar(20) NOT NULL,
    MAPID	varchar(50) NOT NULL,
    MAPSID	varchar(50),
    FROMEXPR	nvarchar(4000) NOT NULL,
    FROMTYPE	varchar(50) NOT NULL,
    REL	varchar(4) NOT NULL,
    RELA	varchar(100),
    TOEXPR	nvarchar(4000),
    TOTYPE	varchar(50),
    CVF	int 
);


CREATE TABLE  $(db_schema).MRSTY (
    CUI	char(8) NOT NULL,
    TUI	char(4) NOT NULL,
    STN	varchar(100) NOT NULL,
    STY	varchar(50) NOT NULL,
    ATUI	varchar(11) NOT NULL,
    CVF	int 
);


CREATE TABLE  $(db_schema).MRXNS_ENG (
    LAT	char(3) NOT NULL,
    NSTR	nvarchar(3000) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXNW_ENG (
    LAT	char(3) NOT NULL,
    NWD	nvarchar(100) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRAUI (
    AUI1	varchar(9) NOT NULL,
    CUI1	char(8) NOT NULL,
    VER	varchar(10) NOT NULL,
    REL	varchar(4),
    RELA	varchar(100),
    MAPREASON	nvarchar(4000) NOT NULL,
    AUI2	varchar(9) NOT NULL,
    CUI2	char(8) NOT NULL,
    MAPIN	char(1) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_BAQ (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_CZE (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_DAN (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_DUT (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_ENG (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_FIN (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_FRE (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_GER (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_HEB (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_HUN (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_ITA (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_JPN (
    LAT char(3) NOT NULL,
    WD  nvarchar(500) NOT NULL,
    CUI char(8) NOT NULL,
    LUI varchar(10) NOT NULL,
    SUI varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_KOR (
    LAT char(3) NOT NULL,
    WD  nvarchar(500) NOT NULL,
    CUI char(8) NOT NULL,
    LUI varchar(10) NOT NULL,
    SUI varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_LAV (
    LAT char(3) NOT NULL,
    WD  nvarchar(200) NOT NULL,
    CUI char(8) NOT NULL,
    LUI varchar(10) NOT NULL,
    SUI varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_NOR (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_POR (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_RUS (
    LAT char(3) NOT NULL,
    WD  nvarchar(200) NOT NULL,
    CUI char(8) NOT NULL,
    LUI varchar(10) NOT NULL,
    SUI varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_SCR (
    LAT char(3) NOT NULL,
    WD  nvarchar(200) NOT NULL,
    CUI char(8) NOT NULL,
    LUI varchar(10) NOT NULL,
    SUI varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_SPA (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).MRXW_SWE (
    LAT	char(3) NOT NULL,
    WD	nvarchar(200) NOT NULL,
    CUI	char(8) NOT NULL,
    LUI	varchar(10) NOT NULL,
    SUI	varchar(10) NOT NULL
);


CREATE TABLE  $(db_schema).AMBIGSUI (
    SUI	varchar(10) NOT NULL,
    CUI	char(8) NOT NULL
);


CREATE TABLE  $(db_schema).AMBIGLUI (
    LUI	varchar(10) NOT NULL,
    CUI	char(8) NOT NULL
);


CREATE TABLE  $(db_schema).DELETEDCUI (
    PCUI	char(8) NOT NULL,
    PSTR	varchar(3000) NOT NULL
);


CREATE TABLE  $(db_schema).DELETEDLUI (
    PLUI	varchar(10) NOT NULL,
    PSTR	varchar(3000) NOT NULL
);


CREATE TABLE  $(db_schema).DELETEDSUI (
    PSUI	varchar(10) NOT NULL,
    LAT	char(3) NOT NULL,
    PSTR	varchar(3000) NOT NULL
);


CREATE TABLE  $(db_schema).MERGEDCUI (
    PCUI	char(8) NOT NULL,
    CUI	char(8) NOT NULL
);


CREATE TABLE  $(db_schema).MERGEDLUI (
    PLUI	varchar(10),
    LUI	varchar(10)
);
