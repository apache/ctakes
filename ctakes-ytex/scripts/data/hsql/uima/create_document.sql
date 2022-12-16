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

CREATE CACHED TABLE document(
	document_id int NOT NULL primary key,
	instance_id bigint default 0 not null,
	instance_key varchar(256) null,
	analysis_batch varchar(50) NOT NULL,
	cas LONGVARBINARY NULL,
	doc_text longvarchar NULL
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
CREATE CACHED TABLE anno_base (
	anno_base_id int not null primary key,
	document_id int not null,
	span_begin int,
	span_end int,
	uima_type_id int not null
)
;


create INDEX IX_type_span on anno_base(document_id, span_begin, span_end, uima_type_id);
create INDEX IX_type on anno_base(document_id, uima_type_id);
CREATE INDEX IX_docanno_doc ON anno_base (document_id);

CREATE CACHED TABLE anno_sentence (
	anno_base_id int not null primary key,
	sentenceNumber int,
	segmentId varchar(20)
);

CREATE CACHED TABLE anno_named_entity (
	anno_base_id int not null primary key,
	discoveryTechnique int,
	status int,
	polarity int,
	uncertainty int,
	conditional bit,
	generic bit,
	typeID int,
	confidence float,
	segmentID varchar(20)
);

CREATE CACHED TABLE anno_med_event (
	anno_base_id int not null primary key,
	discoveryTechnique int,
	status int,
	polarity int,
	uncertainty int,
	conditional bit,
	generic bit,
	typeID int,
	confidence float,
	segmentID varchar(20),
	freqNumber varchar(10) ,
	freqUnit varchar(10) ,
	strengthNumber varchar(10) ,
	strengthUnit varchar(10) ,
	change varchar(10),
	dosage varchar(10)
);


CREATE CACHED TABLE anno_ontology_concept (
	anno_ontology_concept_id int IDENTITY not null primary key,
	anno_base_id int not null ,
	code varchar(20) ,
	cui char(8) ,
	disambiguated bit default 0 not null
);
create index IX_anno_base_id on anno_ontology_concept(anno_base_id);
create index IX_code on anno_ontology_concept(code);
create index IX_anno_code on anno_ontology_concept(anno_base_id,code);
create index IX_anno_cui on anno_ontology_concept(anno_base_id,cui);

CREATE CACHED TABLE anno_segment(
	anno_base_id int NOT NULL  ,
	id varchar(20) NULL,
PRIMARY KEY
(
	anno_base_id 
)
)
;

CREATE INDEX IX_segment_anno_seg ON anno_segment
(
	anno_base_id,
	id
)
;

-- mapped to BaseToken, WordToken
CREATE CACHED TABLE anno_token (
	anno_base_id int NOT NULL PRIMARY KEY ,
	tokenNumber int default 0 NOT NULL ,
	normalizedForm varchar(20) ,
	partofSpeech varchar(5) ,
	coveredText varchar(20) null,
	capitalization int default 0 not null ,
	numPosition int default 0 not null ,
	suggestion varchar(20) ,
	canonicalForm varchar(20)
) ;
create index IX_coveredText on anno_token(coveredText);
create index IX_canonicalForm on anno_token(canonicalForm);

CREATE CACHED TABLE anno_date (
	anno_base_id int not null primary key,
	tstamp datetime
) ;

CREATE CACHED TABLE anno_markable (
	anno_base_id int not null primary key ,
	id int default 0,
	anaphoric_prob double default 0,
	content int default 0
) ;

CREATE CACHED TABLE anno_treebank_node (
	anno_base_id int not null primary key ,
	parent int default 0,
	nodeType varchar(10),
	nodeValue varchar(10),
	leaf bit default 0,
	headIndex int default 0,
	index int default 0,
	tokenIndex int default 0
) ;

CREATE CACHED TABLE anno_link (
	anno_link_id int IDENTITY not null primary key,
	parent_anno_base_id int not null ,
	child_anno_base_id int not null ,
	feature varchar(20)
) ;
create index IX_link on anno_link(parent_anno_base_id, child_anno_base_id, feature);

CREATE CACHED TABLE anno_contain (
  parent_anno_base_id int not null ,
  parent_uima_type_id int not null ,
  child_anno_base_id int not null ,
  child_uima_type_id int not null ,
  primary key (parent_anno_base_id, child_anno_base_id)
);
create index IX_parent_id_child_type on anno_contain(parent_anno_base_id, child_uima_type_id);
create index IX_child_id_parent_type on anno_contain(child_anno_base_id, parent_uima_type_id);

CREATE CACHED TABLE fracture_demo(
	note_id int IDENTITY NOT NULL primary key,
	site_id varchar(10) NULL,
	note_text longvarchar NULL,
	fracture varchar(20) NULL,
	note_set varchar(10) NULL
);


-- metamap tables
CREATE CACHED TABLE anno_mm_candidate (
	anno_base_id int not null primary key,
	cui char(8),
	score int default 0,
	head bit default 0,
	overmatch bit default 0
);

CREATE CACHED TABLE anno_mm_acronym (
	anno_base_id int not null primary key,
	acronym varchar(10),
    expansion varchar(30)    
);

CREATE CACHED TABLE anno_mm_utterance (
	anno_base_id int not null primary key,
	pmid varchar(10),
    location varchar(30)    
);


CREATE CACHED TABLE anno_mm_cuiconcept (
    anno_mm_cuiconcept_id int IDENTITY not null primary key,
    anno_base_id int,
    negExCui char(8)
);

CREATE CACHED TABLE anno_mm_negation (
    anno_base_id int not null primary key,
    negType varchar(10),
    negTrigger varchar(10)
);