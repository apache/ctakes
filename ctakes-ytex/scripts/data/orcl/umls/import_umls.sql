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

CREATE TABLE MRCONSO (
    CUI	char(8) NOT NULL,
    LAT	char(3) NOT NULL,
    TS	char(1) NOT NULL,
    LUI	varchar2(10) NOT NULL,
    STT	varchar2(3) NOT NULL,
    SUI	varchar2(10) NOT NULL,
    ISPREF	char(1) NOT NULL,
    AUI	varchar2(9) NOT NULL primary key,
    SAUI	varchar2(50),
    SCUI	varchar2(50),
    SDUI	varchar2(50),
    SAB	varchar2(20) NOT NULL,
    TTY	varchar2(20) NOT NULL,
    CODE	varchar2(50) NOT NULL,
    STR	varchar2(3000) NOT NULL,
    SRL	int NOT NULL,
    SUPPRESS	char(1) NOT NULL,
    CVF	int
) ;


CREATE TABLE MRSTY (
    CUI	char(8) NOT NULL,
    TUI	char(4) NOT NULL,
    STN	varchar2(100) NOT NULL,
    STY	varchar2(50) NOT NULL,
    ATUI varchar2(11) NOT NULL,
    CVF	int 
);

