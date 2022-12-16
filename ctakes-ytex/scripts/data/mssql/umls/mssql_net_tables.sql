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


CREATE TABLE  $(db_schema).SRDEF (
	RT	VARCHAR (3)  NOT NULL,
	UI	CHAR (4)  NOT NULL,
	STY_RL	VARCHAR (41)  NOT NULL,
	STN_RTN	VARCHAR (14)  NOT NULL,
	DEF	NVARCHAR(4000)	NOT NULL,
	EX	VARCHAR (185) ,
	UN	NVARCHAR(4000),
	NH	VARCHAR (1) ,
	ABR	VARCHAR (4)  NOT NULL,
	RIN	VARCHAR (23) 
) ;


CREATE TABLE  $(db_schema).SRFIL (
	FIL	VARCHAR (7)  NOT NULL,
	DES	VARCHAR (56)  NOT NULL,
	FMT	VARCHAR (41)  NOT NULL,
	CLS	VARCHAR (2)  NOT NULL,
	RWS	VARCHAR (4)  NOT NULL,
	BTS	VARCHAR (6)  NOT NULL
) ;



CREATE TABLE  $(db_schema).SRFLD (
	COL	VARCHAR (3)  NOT NULL,
	DES	VARCHAR (32)  NOT NULL,
	REF	VARCHAR (3)  NOT NULL,
	FIL	VARCHAR (19)  NOT NULL
) ;


CREATE TABLE  $(db_schema).SRSTR (
	STY_RL1	VARCHAR (41)  NOT NULL,
	RL	VARCHAR (23)  NOT NULL,
	STY_RL2	VARCHAR (39) ,
	LS	VARCHAR (3)  NOT NULL
) ;


CREATE TABLE  $(db_schema).SRSTRE1 (
	UI1	CHAR (4)  NOT NULL,
	UI2	CHAR (4)  NOT NULL,
	UI3	CHAR (4)  NOT NULL
) ;


CREATE TABLE  $(db_schema).SRSTRE2 (
	STY1	VARCHAR (41)  NOT NULL,
	RL	VARCHAR (23)  NOT NULL,
	STY2	VARCHAR (41)  NOT NULL
) ;
