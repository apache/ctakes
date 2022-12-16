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

drop view [ESLD].[v_abs_radiology_doclink]
;
go

create view [ESLD].[v_abs_radiology_doclink]
as
select r.studyid, r.id, d.uid, d.doc_date, d.doc_title, d.doc_text 
from esld.abs_radiology r
left join esld.v_document d 
	on r.studyid = d.study_id
	and datepart(yyyy, r.procedure_date) = datepart(yyyy, d.doc_date)
	and datepart(dy, r.procedure_date) = datepart(dy, d.doc_date)
where d.analysis_batch = '$(analysis_batch)'
and d.copy_of_document_id is null
and d.document_type_name = 'RADIOLOGY'
;
go

drop view esld.v_abs_endoscopy_doclink;
go

create view esld.v_abs_endoscopy_doclink
as
SELECT r.studyid, r.id, d.uid, d.site_id, d.doc_date, d.doc_title, d.doc_text
FROM esld.abs_endoscopy r 
LEFT JOIN esld.v_document d 
	ON r.studyid = d .study_id 
	AND datepart(yyyy, r.endoscopy_date) = datepart(yyyy, d .doc_date) 
	AND datepart(dy, r.endoscopy_date) = datepart(dy, d .doc_date)
WHERE d .analysis_batch = '$(analysis_batch)' 
AND d.copy_of_document_id IS NULL
and d.document_type_name = 'PROGRESS_NOTE';
go



drop view esld.v_document_class_review;
go

create view esld.v_document_class_review
as
select dk.document_id, dk.doc_text, c.*
from esld.[document_class_review] c
inner join esld.v_document dk 
	on c.uid = dk.uid 
	and c.studyid = dk.study_id 
	and c.site_id = dk.site_id 
	and c.document_type_id = dk.document_type_id
where dk.analysis_batch = '$(analysis_batch)'
;
go

drop view esld.v_document_current;
go

create view esld.v_document_current
as
select * from esld.v_document where analysis_batch = '$(analysis_batch)'
;