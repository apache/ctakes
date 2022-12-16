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

delete anno_base 
from anno_base 
left join document
	on anno_base.document_id = document.document_id
where document.document_id is null
; 

delete anno_sentence 
from anno_sentence 
left join anno_base
	on anno_base.anno_base_id = anno_sentence.anno_base_id
where anno_base.anno_base_id is null
; 

delete anno_date
from anno_date 
left join anno_base
	on anno_base.anno_base_id = anno_date.anno_base_id
where anno_base.anno_base_id is null
;

delete anno_named_entity
from anno_named_entity 
left join anno_base
	on anno_base.anno_base_id = anno_named_entity.anno_base_id
where anno_base.anno_base_id is null
;

delete anno_source_doc_info
from anno_source_doc_info 
left join anno_base
	on anno_base.anno_base_id = anno_source_doc_info.anno_base_id
where anno_base.anno_base_id is null
;

delete anno_docdate
from anno_docdate 
left join anno_base
	on anno_base.anno_base_id = anno_docdate.anno_base_id
where anno_base.anno_base_id is null
;

delete anno_dockey
from anno_dockey 
left join anno_base
	on anno_base.anno_base_id = anno_dockey.anno_base_id
where anno_base.anno_base_id is null
;

delete anno_segment
from anno_segment 
left join anno_base
	on anno_base.anno_base_id = anno_segment.anno_base_id
where anno_base.anno_base_id is null
;

delete anno_num_token
from anno_num_token 
left join anno_base
	on anno_base.anno_base_id = anno_num_token.anno_base_id
where anno_base.anno_base_id is null
;

delete anno_ontology_concept
from anno_ontology_concept 
left join anno_base
	on anno_base.anno_base_id = anno_ontology_concept.anno_base_id
where anno_base.anno_base_id is null
;

delete anno_umls_concept
from anno_umls_concept 
left join anno_ontology_concept
	on anno_umls_concept.anno_ontology_concept_id = anno_ontology_concept.anno_ontology_concept_id
where anno_ontology_concept.anno_ontology_concept_id is null
;

delete anno_word_token
from anno_word_token 
left join anno_base
	on anno_base.anno_base_id = anno_word_token.anno_base_id
where anno_base.anno_base_id is null
;