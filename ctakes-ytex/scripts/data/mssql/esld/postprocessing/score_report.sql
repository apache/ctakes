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

select *, 2*precision*recall/(precision+recall) fscore
from
(
	select *, cast(tp as numeric(6,2))/(tp + fp) precision, cast(tp as numeric(6,2))/(tp+fn) recall
	from
	(
		select task, sum(tp) tp, sum(fn) fn, sum(fp) fp, sum(tn) tn
		from
		(
		-- 'raw' scores
			select
				task, 
				case
					when class_auto = class_gold and class_gold <> 0 then 1
					else 0
				end tp,
				case
					when class_auto <> class_gold and class_gold <> 0 then 1
					else 0
				end fn,
				case
					when class_auto <> class_gold and class_gold = 0 then 1
					else 0
				end fp,
				case
					when class_auto = class_gold and class_gold = 0 then 1
					else 0
				end tn
			from esld.document_class s
			inner join esld.v_document d on s.document_id = d.document_id 
			inner join esld.abs_studyid s2 on s2.studyid = d.study_id
			where d.analysis_batch = '$(analysis_batch)'
			and s2.gold_standard = 1
		) s 
		group by task
		-- a good deal of the 'false positive' radiology classifications are indeed true positives
		-- for subsequent classification tasks, these 'false positives' cumulate
		-- calculate adjusted scores without these 'false positives'
		-- scores for ascites, varices, liver masses with just radiology true positives 
		union
		select task, sum(tp) tp, sum(fn) fn, sum(fp) fp, sum(tn) tn
		from
		(
			select
				s.task + '-RAD FP' task, 
				case
					when s.class_auto = s.class_gold and s.class_gold <> 0 then 1
					else 0
				end tp,
				case
					when s.class_auto <> s.class_gold and s.class_gold <> 0 then 1
					else 0
				end fn,
				case
					when s.class_auto <> s.class_gold and s.class_gold = 0 then 1
					else 0
				end fp,
				case
					when s.class_auto = s.class_gold and s.class_gold = 0 then 1
					else 0
				end tn
			from esld.document_class s
			inner join esld.v_document d on s.document_id = d.document_id 
			inner join esld.document_class s2 on s2.document_id = d.document_id
			inner join esld.abs_studyid st on st.studyid = d.study_id
			where d.analysis_batch = '$(analysis_batch)'
			and st.gold_standard = 1
			and s2.task = 'RADIOLOGY_TYPE'
			and s2.class_gold <> 0 
			and s.task in ('ASCITES', 'VARICES', 'LIVER_MASSES')
		) s 
		group by task
		union
		-- scores for radiology with reviewed documents
		select task, sum(tp) tp, sum(fn) fn, sum(fp) fp, sum(tn) tn
		from
		(
			select
				'RADIOLOGY_TYPE_REVIEWED' task, 
				case
					when s.class_auto = coalesce(r.rad_procedure_type_id, s.class_gold) and coalesce(r.rad_procedure_type_id, s.class_gold) <> 0 then 1
					else 0
				end tp,
				case
					when s.class_auto <> coalesce(r.rad_procedure_type_id, s.class_gold) and coalesce(r.rad_procedure_type_id, s.class_gold) <> 0 then 1
					else 0
				end fn,
				case
					when s.class_auto <> coalesce(r.rad_procedure_type_id, s.class_gold) and coalesce(r.rad_procedure_type_id, s.class_gold) = 0 then 1
					else 0
				end fp,
				case
					when s.class_auto = coalesce(r.rad_procedure_type_id, s.class_gold) and coalesce(r.rad_procedure_type_id, s.class_gold) = 0 then 1
					else 0
				end tn
			from esld.document_class s
			inner join esld.v_document d on s.document_id = d.document_id 
			inner join esld.abs_studyid st on st.studyid = d.study_id
			left join esld.abs_radiology_review r on r.uid = d.uid and r.studyid = d.study_id
			where d.analysis_batch = '$(analysis_batch)'
			and st.gold_standard = 1
			and s.task = 'RADIOLOGY_TYPE'
		) s 
		group by task
	) s
) s
