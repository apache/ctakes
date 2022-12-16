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

-- set uid for radiology reviews where the corresponding document can be identified


update esld.abs_radiology 
set esld.abs_radiology.uid = r.uid
from esld.abs_radiology ar 
inner join esld.v_document r
	on r.study_id = ar.studyid 
	and datepart(yyyy, r.doc_date) = datepart(yyyy, ar.procedure_date)
	and datepart(dy, r.doc_date) = datepart(dy,ar.procedure_date)
left join esld.v_document_ontoanno o on r.document_id = o.document_id and o.code = 'DOCREF' and o.coding_scheme = 'ESLD'
where r.document_type_name = 'RADIOLOGY'
-- and CHARINDEX ('CHEST', r.doc_title) = 0
and r.copy_of_document_id is null
and r.analysis_batch = '$(analysis_batch)'
and o.code is null
and ar.uid is null
and ar.id not in
(
	select id 
	from
	(
		select ar.id, ar.studyid, ar.procedure_date, ar.procedure_type, count(r.document_id) doc_count
		from esld.abs_radiology ar 
		inner join esld.v_document r
			on r.study_id = ar.studyid 
			and datepart(yyyy, r.doc_date) = datepart(yyyy, ar.procedure_date)
			and datepart(dy, r.doc_date) = datepart(dy,ar.procedure_date)
		left join esld.v_document_ontoanno o on r.document_id = o.document_id and o.code = 'DOCREF' and o.coding_scheme = 'ESLD'
		where r.document_type_name = 'RADIOLOGY'
		-- and CHARINDEX ('CHEST', r.doc_title) = 0
		and r.copy_of_document_id is null
		and r.analysis_batch = '$(analysis_batch)'
		and o.code is null
		and ar.uid is null
		group by ar.id, ar.studyid, ar.procedure_date, ar.procedure_type
	) d 
	where d.doc_count > 2
)