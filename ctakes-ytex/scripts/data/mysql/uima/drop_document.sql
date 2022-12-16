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

-- drop 'operational' data
-- legacy begin
drop table if exists document_class;
drop table if exists anno_word_token;
drop table if exists anno_base_token;
drop table if exists anno_num_token;
drop table if exists anno_umls_concept; 
drop table if exists anno_source_doc_info;
drop table if exists anno_umls_concept;
-- legacy end
drop table if exists anno_mm_cuiconcept;
drop table if exists anno_mm_candidate;
drop table if exists anno_mm_acronym;
drop table if exists anno_mm_utterance;
drop table if exists anno_mm_negation;
drop table if exists fracture_demo;
drop table if exists anno_drug_mention;
drop table if exists anno_markable;
drop table if exists anno_treebank_node;
drop table if exists anno_link;
drop table if exists anno_contain;
drop table if exists anno_num_token;
drop table if exists anno_token;
drop table if exists anno_segment;
drop table if exists anno_ontology_concept;
drop table if exists anno_named_entity;
drop table if exists anno_med_event;
drop table if exists anno_sentence;
drop table if exists anno_date;
drop table if exists anno_base;
drop table if exists document;

