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

create table $(db_schema).hibernate_sequences (
	sequence_name varchar(100) not null primary key,
	next_val int not null default 1
);
insert into $(db_schema).hibernate_sequences(sequence_name, next_val) values ('document_id_sequence', 1);

create table $(db_schema).anno_base_sequence (
	sequence_name varchar(100) not null primary key,
	next_val int not null default 1
);
insert into $(db_schema).anno_base_sequence(sequence_name, next_val) values ('anno_base_id_sequence', 1);


create table $(db_schema).ref_named_entity_regex (
	named_entity_regex_id int IDENTITY(1,1) NOT NULL,
	regex varchar(512) not null,
	coding_scheme varchar(20) not null,
	code varchar(20) not null,
	oid varchar(10),
	context varchar(256),
	primary key (named_entity_regex_id)
);

create table $(db_schema).ref_segment_regex (
	segment_regex_id int IDENTITY(1,1) NOT NULL,
	regex varchar(256) not null,
	segment_id varchar(256),
	limit_to_regex bit null default 0, 
	primary key (segment_regex_id)
);

create table $(db_schema).ref_uima_type (
	uima_type_id int not null,
	uima_type_name varchar(256) not null,
	table_name varchar(100) null,
	CONSTRAINT PK_ref_uima_type PRIMARY KEY  
	(
		uima_type_id ASC
	)
)
;

CREATE TABLE $(db_schema).ref_stopword (
	stopword varchar(50) not null primary key
)
;