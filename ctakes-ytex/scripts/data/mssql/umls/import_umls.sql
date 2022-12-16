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

-- this is only executed if you don't have umls installed in your db 
-- load the umls mrconso table from a dump file.
-- the following was copied directly from the umls load script.

-- intentionally do not drop MRCONSO - if it exists then we should use it 
-- instead of overwriting it
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

ALTER TABLE $(db_schema).MRCONSO ADD CONSTRAINT X_MRCONSO_PK  PRIMARY KEY (AUI);

CREATE TABLE  $(db_schema).MRSTY (
    CUI	char(8) NOT NULL,
    TUI	char(4) NOT NULL,
    STN	varchar(100) NOT NULL,
    STY	varchar(50) NOT NULL,
    ATUI	varchar(11) NOT NULL,
    CVF	int 
);

ALTER TABLE $(db_schema).MRSTY ADD CONSTRAINT X_MRSTY_PK  PRIMARY KEY (ATUI);

