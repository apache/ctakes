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

delete from esld.document
where analysis_batch='$(analysis_batch)'
;
/*
delete from esld.document_class
where document_id in (select document_id from esld.document where analysis_batch='$(analysis_batch)')
;

delete from esld.segment_annotation
where document_annotation_id in 
	(
	select document_annotation_id 
	from esld.document d 
	inner join esld.document_annotation da on d.document_id = da.document_id 
	where analysis_batch='$(analysis_batch)'
	)
;

delete from esld.umls_concept_annotation
where ontology_concept_annotation_id in 
	(
	select ontology_concept_annotation_id 
	from esld.document d 
	inner join esld.document_annotation da on d.document_id = da.document_id 
	inner join esld.named_entity_annotation ne on ne.document_annotation_id = da.document_annotation_id
	inner join esld.ontology_concept_annotation o on o.document_annotation_id = da.document_annotation_id
	where analysis_batch='$(analysis_batch)'
	)
;

delete from esld.ontology_concept_annotation
where ontology_concept_annotation_id in 
	(
	select ontology_concept_annotation_id 
	from esld.document d 
	inner join esld.document_annotation da on d.document_id = da.document_id 
	inner join esld.named_entity_annotation ne on ne.document_annotation_id = da.document_annotation_id
	where analysis_batch='$(analysis_batch)'
	)
;

delete from esld.named_entity_annotation
where document_annotation_id in 
	(
	select ne.document_annotation_id 
	from esld.document d 
	inner join esld.document_annotation da on d.document_id = da.document_id 
	inner join esld.named_entity_annotation ne on ne.document_annotation_id = da.document_annotation_id
	where analysis_batch='$(analysis_batch)'
	)
;
delete from esld.sentence_annotation
where document_annotation_id in 
	(
	select ne.document_annotation_id 
	from esld.document d 
	inner join esld.document_annotation da on d.document_id = da.document_id 
	inner join esld.sentence_annotation ne on ne.document_annotation_id = da.document_annotation_id
	where analysis_batch='$(analysis_batch)'
	)
;
delete from esld.docdate_annotation
where document_annotation_id in 
	(
	select ne.document_annotation_id 
	from esld.document d 
	inner join esld.document_annotation da on d.document_id = da.document_id 
	inner join esld.docdate_annotation ne on ne.document_annotation_id = da.document_annotation_id
	where analysis_batch='$(analysis_batch)'
	)
;
delete from esld.document_annotation
where document_annotation_id in 
	(
	select da.document_annotation_id 
	from esld.document d 
	inner join esld.document_annotation da on d.document_id = da.document_id 
	where analysis_batch='$(analysis_batch)'
	)
;
*/
