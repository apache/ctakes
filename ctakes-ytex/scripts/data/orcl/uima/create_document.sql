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

-- create sequence document_id_sequence;
-- create sequence anno_base_id_sequence;
create sequence anno_onto_concept_id_sequence;
create sequence anno_contain_id_sequence;
create sequence anno_link_id_sequence;
create sequence demo_note_id_sequence;
create sequence anno_mm_cuiconcept_id_sequence;

CREATE TABLE document(
	document_id int  NOT NULL,
	instance_id NUMBER(19) default 0 not null,
	instance_key varchar2(256) null,
	analysis_batch varchar2(50) default ' ' NOT NULL,
	cas blob NULL,
	doc_text clob NULL,
	CONSTRAINT PK_document PRIMARY KEY
	(
		document_id
	)	
) 
;

CREATE INDEX IX_document_analysis_batch ON document 
(
	analysis_batch,
	document_id
)
;

CREATE INDEX IX_instance_id ON document 
(
	instance_id
)
;

CREATE INDEX IX_instance_key ON document 
(
	instance_key
)
;

create table anno_base (
	anno_base_id int  not null,
	document_id int not null,
	span_begin int,
	span_end int,
	uima_type_id int not null,
	primary key (anno_base_id),
	foreign key (document_id) references document (document_id) ON DELETE CASCADE,
	foreign key (uima_type_id) references ref_uima_type (uima_type_id)
)
;

CREATE INDEX IX_docanno_doc ON anno_base (document_id)
;


create table anno_sentence (
	anno_base_id int not null,
	sentenceNumber int,
	segmentId varchar2(20),
	primary key (anno_base_id),
	foreign key (anno_base_id) references anno_base(anno_base_id)  ON DELETE CASCADE
);

create table anno_named_entity (
	anno_base_id int not null,
	discoveryTechnique int,
	status int,
	polarity int,
	uncertainty int,
	conditional numeric(1),
	generic numeric(1),
	typeID int,
	confidence float,
	segmentID varchar2(20),
	primary key (anno_base_id),
	foreign key (anno_base_id) references anno_base(anno_base_id)  ON DELETE CASCADE
);

create table anno_med_event (
	anno_base_id int,
	discoveryTechnique int,
	status int,
	polarity int,
	uncertainty int,
	conditional numeric(1),
	generic numeric(1),
	typeID int,
	confidence float,
	segmentID varchar2(20),
	freqNumber varchar2(10),
	freqUnit varchar2(10),
	strengthNumber varchar2(10),
	strengthUnit varchar2(10),
	"change" varchar2(10),
	dosage varchar2(10),
	primary key (anno_base_id),
	foreign key (anno_base_id) references anno_base(anno_base_id)  ON DELETE CASCADE
);

create table anno_ontology_concept (
	anno_ontology_concept_id int  not null,
	anno_base_id int not null,
	code varchar2(20),
	cui char(8),
	disambiguated numeric(1) default 0 not null,
	primary key (anno_ontology_concept_id),
	foreign key (anno_base_id) references anno_base(anno_base_id)  ON DELETE CASCADE
);

CREATE INDEX IX_ontology_concept_code ON anno_ontology_concept (code)
;


CREATE TABLE anno_segment(
	anno_base_id int NOT NULL,
	id varchar2(20) NULL,
	PRIMARY KEY (anno_base_id),
	foreign key (anno_base_id) references anno_base(anno_base_id)  ON DELETE CASCADE
)
;


CREATE INDEX IX_segment_anno_seg ON anno_segment
(
	id 
)
;


-- mapped to BaseToken
create table anno_token (
	anno_base_id int NOT NULL,
	tokenNumber int,
	normalizedForm varchar2(20),
	partOfSpeech varchar2(5),
	coveredText varchar2(20) null,
	capitalization int default 0 not null,
	numPosition int default 0 not null,
	suggestion varchar2(20),
	canonicalForm varchar2(20),
	negated NUMERIC(1) default 0 not null,
	possible NUMERIC(1) default 0 not null,
	PRIMARY KEY
	(
		anno_base_id 
	),
	foreign key (anno_base_id)
		references anno_base(anno_base_id)
		ON DELETE CASCADE
) ;

create index IX_covered_text on anno_token(coveredText);
create index IX_canonical_form on anno_token(canonicalForm);

create table anno_date (
	anno_base_id int not null,
	tstamp timestamp,
	primary key (anno_base_id),
	foreign key (anno_base_id) references anno_base(anno_base_id) ON DELETE CASCADE 
) ;

create table anno_markable (
	anno_base_id int not null,
	id int default 0,
	anaphoric_prob double PRECISION default 0,
	content int default 0,
	primary key (anno_base_id),
	foreign key (anno_base_id) references anno_base(anno_base_id) ON DELETE CASCADE 
);

create table anno_treebank_node (
	anno_base_id int not null,
	parent int default 0,
	nodeType varchar2(10),
	nodeValue varchar2(10),
	leaf numeric(1) default 0,
	headIndex int default 0,
	"index" int default 0,
	tokenIndex int default 0,
	primary key (anno_base_id),
	foreign key (anno_base_id) references anno_base(anno_base_id) ON DELETE CASCADE 
);

create table anno_link (
	anno_link_id int not null,
	parent_anno_base_id int not null,
	child_anno_base_id int not null,
	feature varchar2(20),
	primary key (anno_link_id),
	foreign key (parent_anno_base_id) references anno_base(anno_base_id) ON DELETE CASCADE 
);
create index IX_link on anno_link(parent_anno_base_id, child_anno_base_id, feature);

create table anno_contain (
  parent_anno_base_id int not null,
  parent_uima_type_id int not null,
  child_anno_base_id int not null,
  child_uima_type_id int not null,
  primary key (parent_anno_base_id, child_anno_base_id),
  foreign key (parent_anno_base_id)
		references anno_base(anno_base_id)
		ON DELETE CASCADE
);

CREATE INDEX IX_anno_contain_p ON anno_contain (parent_anno_base_id, child_uima_type_id)
;

CREATE INDEX IX_anno_contain_c ON anno_contain (child_anno_base_id, parent_uima_type_id)
;


CREATE TABLE fracture_demo (
	note_id int NOT NULL primary key,
	site_id varchar2(10) NULL,
	note_text clob NULL,
	fracture varchar2(20) NULL,
	note_set varchar2(10) NULL
);


-- metamap tables
create table anno_mm_candidate (
	anno_base_id int not null,
	cui char(8),
	score int default 0,
	head numeric(1) default 0,
	overmatch numeric(1) default 0,
	primary key (anno_base_id),
	foreign key (anno_base_id) references anno_base(anno_base_id) ON DELETE CASCADE
);

create table anno_mm_acronym (
	anno_base_id int not null,
	acronym varchar2(10),
    "expansion" varchar2(30),
	primary key (anno_base_id),
	foreign key (anno_base_id) references anno_base(anno_base_id) ON DELETE CASCADE
);

create table anno_mm_utterance (
	anno_base_id int not null,
	pmid varchar2(10),
    location varchar2(30),
	primary key (anno_base_id),
	foreign key (anno_base_id) references anno_base(anno_base_id) ON DELETE CASCADE
);


create table anno_mm_cuiconcept (
    anno_mm_cuiconcept_id int not null,
    anno_base_id int,
    negExCui char(8),
	primary key (anno_mm_cuiconcept_id),
	foreign key (anno_base_id) references anno_base(anno_base_id) ON DELETE CASCADE
);

create table anno_mm_negation (
    anno_base_id int not null,
    negType varchar2(10),
    negTrigger varchar2(10),
	primary key (anno_base_id),
	foreign key (anno_base_id) references anno_base(anno_base_id) ON DELETE CASCADE
);