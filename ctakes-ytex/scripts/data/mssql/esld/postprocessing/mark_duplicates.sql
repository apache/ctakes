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

-- documents for the same subject with the same date that are identical are marked as duplicates
-- for each document for the same subject on the same day
-- compare the sum of sentence lengths of each sentence after the 'Report:' string
-- the copy_of_document_id links the duplicate to the 'original'
-- one of the documents will arbitrarily be picked as an original
-- parameter: analysis_batch

-- :setvar analysis_batch '02-04'

update esld.document
set copy_of_document_id = null
where analysis_batch = '$(analysis_batch)'
;


update esld.document
set esld.document.copy_of_document_id = doc_parent.parent_document_id
from
esld.document inner join
(
select d1.study_id, min(d1.document_id) parent_document_id, d2.document_id, datepart(yyyy, d1.doc_date) doc_year, datepart(dy, d1.doc_date) doc_day, d1.rep_size
from
(
select d.document_id, d.study_id, d.doc_date, d.document_type_id,  d.analysis_batch, sum(s.span_end - s.span_begin) rep_size
from esld.v_document d
inner join esld.anno_base s on d.document_id = s.document_id
inner join esld.anno_sentence s2 on s2.anno_base_id = s.anno_base_id
where s.span_begin > charindex('Report:', d.doc_text)
group by d.document_id, d.study_id, d.doc_date, d.document_type_id,  d.analysis_batch
) d1
inner join
(
select d.document_id, d.study_id, d.doc_date, d.document_type_id, d.analysis_batch, sum(s.span_end - s.span_begin) rep_size
from esld.v_document d
inner join esld.anno_base s on d.document_id = s.document_id
inner join esld.anno_sentence s2 on s2.anno_base_id = s.anno_base_id
where s.span_begin > charindex('Report:', d.doc_text)
group by d.document_id, d.study_id, d.doc_date, d.document_type_id,  d.analysis_batch
) d2
	on d1.study_id = d2.study_id 
	and datepart(yyyy, d1.doc_date) = datepart(yyyy, d2.doc_date)
	and datepart(dy, d1.doc_date) = datepart(dy, d2.doc_date)
	and d1.rep_size = d2.rep_size
	and d1.document_type_id = d2.document_type_id
	and d1.document_id < d2.document_id
	and d1.analysis_batch = d2.analysis_batch
where
	d1.analysis_batch = '$(analysis_batch)'
group by d1.study_id, d2.document_id, datepart(yyyy, d1.doc_date), datepart(dy, d1.doc_date), d1.rep_size
) doc_parent on esld.document.document_id = doc_parent.document_id
;