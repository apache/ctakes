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

create view $(db_schema).v_document as
-- We assume the metadata about documents (patient id, date) come from
-- some table in your database.  modify this view to join with that table
-- and get this info
select analysis_batch, document_id, doc_text, instance_id, cast(null as int) patient_id, cast(null as datetime) doc_date, cast(null as varchar(256)) doc_title, cast(null as varchar(256)) document_type_name
from $(db_schema).document;
go

create view $(db_schema).[v_annotation]
AS
SELECT anno.*, ur.uima_type_name, substring(doc.doc_text, anno.span_begin+1, anno.span_end-anno.span_begin) anno_text, doc.analysis_batch
FROM $(db_schema).anno_base AS anno 
INNER JOIN $(db_schema).v_document AS doc ON doc.document_id = anno.document_id
INNER JOIN $(db_schema).REF_UIMA_TYPE AS ur on ur.uima_type_id = anno.uima_type_id
;
GO

create view $(db_schema).v_document_cui_sent
as
-- this view gives the document info, cui info, and sentence info in which a cui is found
SELECT 
  da.anno_base_id,
  d.analysis_batch,
  da.document_id, 
  ne.polarity, 
  o.code, 
  substring(d.doc_text, da.span_begin+1, da.span_end-da.span_begin) cui_text, 
  substring(d.doc_text, s.span_begin+1, s.span_end-s.span_begin) sentence_text,
  o.disambiguated,
  d.patient_id,
  d.doc_date,
  d.doc_title,
  d.document_type_name
FROM $(db_schema).anno_base da 
INNER JOIN $(db_schema).anno_named_entity  ne ON da.anno_base_id = ne.anno_base_id 
INNER JOIN $(db_schema).anno_ontology_concept  o ON o.anno_base_id = ne.anno_base_id 
left join 
( 
  --  get the sentence that contains the cui
  select ac.child_anno_base_id, s.span_begin, s.span_end
  from $(db_schema).anno_contain ac 
  INNER join $(db_schema).anno_base s on ac.parent_anno_base_id = s.anno_base_id
  where s.uima_type_id in (select uima_type_id from $(db_schema).ref_uima_type where uima_type_name = 'org.apache.ctakes.typesystem.type.textspan.Sentence')
  and ac.child_uima_type_id in (select uima_type_id from $(db_schema).ref_uima_type where uima_type_name = 'org.apache.ctakes.typesystem.type.textsem.EntityMention')
) s on da.anno_base_id = s.child_anno_base_id
INNER JOIN $(db_schema).v_document d on da.document_id = d.document_id
;
go

CREATE VIEW $(db_schema).[v_document_ontoanno]
AS
SELECT d.document_id, da.span_begin, da.span_end, ne.polarity, o.code, o.cui, d.analysis_batch, substring(d.doc_text, da.span_begin+1, da.span_end-da.span_begin) cui_text, o.disambiguated
FROM $(db_schema).v_document AS d INNER JOIN
$(db_schema).anno_base AS da ON d.document_id = da.document_id INNER JOIN
$(db_schema).anno_named_entity AS ne ON da.anno_base_id = ne.anno_base_id INNER JOIN
$(db_schema).anno_ontology_concept AS o ON o.anno_base_id = ne.anno_base_id
;
GO

