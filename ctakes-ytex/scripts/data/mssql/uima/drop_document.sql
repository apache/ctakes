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

-- legacy
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[document_class]') AND type in (N'U'))
	drop table $(db_schema).document_class
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_source_doc_info]') AND type in (N'U'))
	drop table $(db_schema).anno_source_doc_info
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_num_token]') AND type in (N'U'))
	drop table $(db_schema).anno_num_token
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_word_token]') AND type in (N'U'))
	drop table $(db_schema).anno_word_token
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_base_token]') AND type in (N'U'))
	drop table $(db_schema).anno_base_token
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_umls_concept]') AND type in (N'U'))
	drop table $(db_schema).anno_umls_concept
;
-- drop 'operational' data
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[fracture_demo]') AND type in (N'U'))
	drop table $(db_schema).fracture_demo
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_contain]') AND type in (N'U'))
	drop table $(db_schema).anno_contain
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_link]') AND type in (N'U'))
	drop table $(db_schema).anno_link
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_mm_cuiconcept]') AND type in (N'U'))
	drop table $(db_schema).anno_mm_cuiconcept
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_mm_candidate]') AND type in (N'U'))
	drop table $(db_schema).anno_mm_candidate
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_mm_acronym]') AND type in (N'U'))
	drop table $(db_schema).anno_mm_acronym
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_mm_utterance]') AND type in (N'U'))
	drop table $(db_schema).anno_mm_utterance
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_mm_negation]') AND type in (N'U'))
	drop table $(db_schema).anno_mm_negation
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_token]') AND type in (N'U'))
	drop table $(db_schema).anno_token
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_segment]') AND type in (N'U'))
	drop table $(db_schema).anno_segment
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_ontology_concept]') AND type in (N'U'))
	drop table $(db_schema).anno_ontology_concept
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_named_entity]') AND type in (N'U'))
	drop table $(db_schema).anno_named_entity
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_med_event]') AND type in (N'U'))
	drop table $(db_schema).anno_med_event
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_sentence]') AND type in (N'U'))
	drop table $(db_schema).anno_sentence
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_date]') AND type in (N'U'))
	drop table $(db_schema).anno_date
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_markable]') AND type in (N'U'))
	drop table $(db_schema).anno_markable
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_treebank_node]') AND type in (N'U'))
	drop table $(db_schema).anno_treebank_node
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_base]') AND type in (N'U'))
	drop table $(db_schema).anno_base
;
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[document]') AND type in (N'U'))
	drop table $(db_schema).document
;
