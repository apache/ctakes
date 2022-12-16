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



CREATE TABLE $(db_schema).[document](
	[document_id] [int] /* IDENTITY(1,1) */ NOT NULL primary key,
	instance_id bigint not null default 0,
	instance_key varchar(256) null,
	[analysis_batch] [varchar](50) NOT NULL,
	[cas] [varbinary](max) NULL,
	[doc_text] [nvarchar](max) NULL
)
;


CREATE NONCLUSTERED INDEX [IX_document_analysis_batch] ON $(db_schema).[document] 
(
	[document_id],
	[analysis_batch]
)
;

CREATE NONCLUSTERED INDEX [IX_uid] ON $(db_schema).[document] 
(
	[instance_id]
)
;

CREATE NONCLUSTERED INDEX [IX_instance_key] ON $(db_schema).[document] 
(
	[instance_key]
)
;

create table $(db_schema).anno_base (
	anno_base_id int /* identity */ not null, 
	document_id int not null, 
	span_begin int,
	span_end int,
	uima_type_id int not null
	primary key (anno_base_id),
	foreign key (document_id) references $(db_schema).document (document_id) ON DELETE CASCADE,
	foreign key (uima_type_id) references $(db_schema).ref_uima_type (uima_type_id)
)
;

CREATE INDEX IX_docanno_doc ON $(db_schema).anno_base (document_id)
;

create table $(db_schema).anno_sentence (
	anno_base_id int not null,
	sentenceNumber int,
	segmentId varchar(20),
	primary key (anno_base_id),
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id)  ON DELETE CASCADE
);

create table $(db_schema).anno_named_entity (
	anno_base_id int not null, 
	discoveryTechnique int,
	status int,
	polarity int,
	uncertainty int,
	conditional bit,
	generic bit,
	typeID int,
	confidence float,
	segmentID varchar(20),
	primary key (anno_base_id),
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id)  ON DELETE CASCADE
);

create table $(db_schema).anno_med_event (
	anno_base_id int not null,
	discoveryTechnique int,
	status int,
	polarity int,
	uncertainty int,
	conditional bit,
	generic bit,
	typeID int,
	confidence float,
	segmentID varchar(20),
	freqNumber varchar(10),
	freqUnit varchar(10),
	strengthNumber varchar(10),
	strengthUnit varchar(10),
	[change] varchar(10),
	dosage varchar(10),
	primary key (anno_base_id),
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id)  ON DELETE CASCADE
);


create table $(db_schema).anno_ontology_concept (
	anno_ontology_concept_id int identity not null, 
	anno_base_id int not null,
	code varchar(20),
	cui char(8),
	disambiguated bit not null default 0,
	primary key (anno_ontology_concept_id),
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id)  ON DELETE CASCADE
);

create index IX_onto_concept_code on $(db_schema).anno_ontology_concept (code);
create index IX_onto_concept_anno_cui on $(db_schema).anno_ontology_concept (anno_base_id, cui);
create index IX_onto_concept_anno_code on $(db_schema).anno_ontology_concept (anno_base_id, code);

CREATE TABLE $(db_schema).[anno_segment](
	[anno_base_id] [int] NOT NULL,
	id varchar(20) NULL,
PRIMARY KEY CLUSTERED 
(
	[anno_base_id] ASC
)
)
;

ALTER TABLE $(db_schema).[anno_segment]  WITH CHECK ADD FOREIGN KEY([anno_base_id])
REFERENCES $(db_schema).[anno_base] ([anno_base_id])
ON DELETE CASCADE
;

CREATE NONCLUSTERED INDEX [IX_segment_anno_seg] ON $(db_schema).[anno_segment] 
(
	[anno_base_id] ASC,
	[id] ASC
)
;


-- mapped to BaseToken
create table $(db_schema).anno_token (
	[anno_base_id] [int] NOT NULL,
	tokenNumber int NOT NULL default 0,
	normalizedForm nvarchar(20),
	partofSpeech varchar(5),
	coveredText nvarchar(20) null,
	capitalization int not null default 0,
	numPosition int not null default 0,
	suggestion varchar(20),
	canonicalForm nvarchar(20),
	negated bit not null default 0,
	possible bit not null default 0,
	PRIMARY KEY CLUSTERED 
	(
		[anno_base_id] ASC
	),
	foreign key (anno_base_id) 
		references $(db_schema).anno_base(anno_base_id)  
		ON DELETE CASCADE
);


create index IX_word_stem on $(db_schema).anno_token (canonicalForm);
create index IX_coveredText on $(db_schema).anno_token (coveredText);

create table $(db_schema).anno_date (
	anno_base_id int not null,
	tstamp datetime,
	primary key (anno_base_id),
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id) ON DELETE CASCADE
);

create table $(db_schema).anno_markable (
	anno_base_id int not null primary key ,
	id int default 0,
	anaphoric_prob float default 0,
	content int default 0,
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id) ON DELETE CASCADE
);

create table $(db_schema).anno_treebank_node (
	anno_base_id int not null primary key ,
	parent int default 0,
	nodeType varchar(10),
	nodeValue varchar(10),
	leaf bit default 0,
	headIndex int default 0,
	[index] int default 0,
	tokenIndex int default 0,
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id) ON DELETE CASCADE
);

create table $(db_schema).anno_link (
	anno_link_id int IDENTITY not null primary key,
	parent_anno_base_id int not null,
	child_anno_base_id int not null,
	feature varchar(20),
	foreign key (parent_anno_base_id) references $(db_schema).anno_base(anno_base_id) ON DELETE CASCADE
);
create index IX_link on $(db_schema).anno_link (parent_anno_base_id, child_anno_base_id, feature);
create index IX_parent on $(db_schema).anno_link (parent_anno_base_id);

-- we run into deadlocks if we have a clustered index and foreign key constraint
-- on anno_contain.  use a nonclustered primary key and throw out the fk.
create table $(db_schema).anno_contain (
  parent_anno_base_id int not null,
  parent_uima_type_id int not null,
  child_anno_base_id int not null,
  child_uima_type_id int not null,
  primary key nonclustered (parent_anno_base_id, child_anno_base_id)
);

create index ix_child_id on $(db_schema).anno_contain(child_anno_base_id);
create index ix_parent_id on $(db_schema).anno_contain(parent_anno_base_id);
create index IX_parent_id_child_type on $(db_schema).anno_contain(parent_anno_base_id, child_uima_type_id);
create index IX_child_id_parent_type on $(db_schema).anno_contain(child_anno_base_id, parent_uima_type_id);



CREATE TABLE $(db_schema).fracture_demo(
	note_id int IDENTITY(1,1) NOT NULL primary key,
	site_id varchar(10) NULL,
	note_text varchar(max) NULL,
	fracture varchar(20) NULL,
	note_set varchar(10) NULL
);


-- metamap tables
create table $(db_schema).anno_mm_candidate (
	anno_base_id int primary key,
	cui char(8),
	score int default 0,
	head bit default 0,
	overmatch bit default 0,
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id) ON DELETE CASCADE
);

create table $(db_schema).anno_mm_acronym (
	anno_base_id int primary key,
	acronym varchar(10),
    [expansion] varchar(30),
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id) ON DELETE CASCADE
);

create table $(db_schema).anno_mm_utterance (
	anno_base_id int primary key,
	pmid varchar(10),
    location varchar(30),
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id) ON DELETE CASCADE
);

create table $(db_schema).anno_mm_cuiconcept (
    anno_mm_cuiconcept_id int identity primary key,
    anno_base_id int,
    negExCui char(8),
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id) ON DELETE CASCADE
);

create table $(db_schema).anno_mm_negation (
    anno_base_id int primary key,
    negType varchar(10),
    negTrigger varchar(10),
	foreign key (anno_base_id) references $(db_schema).anno_base(anno_base_id) ON DELETE CASCADE
);