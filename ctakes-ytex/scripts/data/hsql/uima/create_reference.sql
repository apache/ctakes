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

CREATE CACHED TABLE hibernate_sequences (
	sequence_name varchar(100) not null primary key,
	next_val int default 1 not null
);
insert into hibernate_sequences(sequence_name, next_val) values ('document_id_sequence', 1);

CREATE CACHED TABLE anno_base_sequence (
	sequence_name varchar(100) not null primary key,
	next_val int default 1 not null 
);
insert into anno_base_sequence(sequence_name, next_val) values ('anno_base_id_sequence', 1);


CREATE CACHED TABLE ref_named_entity_regex (
	named_entity_regex_id int IDENTITY NOT NULL primary key,
	regex varchar(512) not null,
	coding_scheme varchar(20) not null,
	code varchar(20) not null,
	oid varchar(10),
	context varchar(256)
) ;

CREATE CACHED TABLE ref_segment_regex (
	segment_regex_id int IDENTITY NOT NULL primary key,
	regex varchar(256) not null,
	segment_id varchar(20),
	limit_to_regex bit  default 0 null
) ;

CREATE CACHED TABLE ref_uima_type (
	uima_type_id int not null primary key,
	uima_type_name varchar(256) not null,
	table_name varchar(100) null
) ;

CREATE UNIQUE  INDEX NK_ref_uima_type ON ref_uima_type
(
	uima_type_name
)
;

CREATE CACHED TABLE ref_stopword (
	stopword varchar(50) not null primary key
) 
;
