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

-- mesenteric varices C0267791
-- esophageal varices C0014867
-- clean up
delete from esld.document_class
where document_id in
(select document_id from esld.document where analysis_batch='$(analysis_batch)')
and task = 'VARICES'
;

-- filter to only documents classified as radiology reports (depends on RADIOLOGY_IR being run first)
-- left join on chart reviews
-- if no matching chart reviews, assume negative varices as gold standard
-- count asserted and negated varices cuis in each document
-- varices positive if # assertions > # negations
insert into esld.document_class (document_id, task, class_auto, class_gold)
select 
	r.document_id, 
	'VARICES',
	r.varices class_auto,
	coalesce(ar.varices_reported, 0) class_gold 
from
( 
select
	r.document_id,
	r.study_id,
	r.uid,
	case
		when varices_p > varices_n then 1
		when mvarices_p > mvarices_n then 1
		when evarices_p > evarices_n then 1
		else 0
	end varices
from
	(
	select
		d.document_id, 
		d.study_id,
		d.uid,
		coalesce(varices.varices_p, 0) varices_p,
		coalesce(varices.varices_n, 0) varices_n,
		coalesce(varices2.mvarices_p, 0) mvarices_p,
		coalesce(varices2.mvarices_n, 0) mvarices_n,
		coalesce(varices2.evarices_p, 0) evarices_p,
		coalesce(varices2.evarices_n, 0) evarices_n
	from esld.v_document d
	inner join esld.ref_document_type doctype on d.document_type_id = doctype.document_type_id
	-- only include abdominal radiology documents 
	inner join esld.document_class s on s.document_id = d.document_id
	left join
		(
			select document_id, sum(varices_p) varices_p, sum(varices_n) varices_n
			from
			(
				-- do a select distinct because the joins will amplify the counts
				select distinct o.document_id, o.span_begin,
					case when o.certainty <> -1 then 1 else 0 end varices_p,
					case when o.certainty = -1 then 1 else 0 end varices_n
				from esld.v_document_ontoanno o
				-- limit to terms in the report section and after
				inner join esld.anno_base segda on segda.document_id = o.document_id
				inner join esld.anno_segment seg on seg.anno_base_id = segda.anno_base_id
				where o.code in ('C0042345')
				and not exists (
					-- filter out varices terms contained in more specific varices terms
					select o2.code 
					from esld.v_document_ontoanno o2 
					where o2.document_id = o.document_id
					and o2.code in ('C0267791', 'C0014867')
					and o.span_end <= o2.span_end 
					and o.span_begin >= o2.span_begin
					)
				and seg.segment_id = 'REPORT'
				and o.span_begin >= segda.span_begin
			) v
			group by document_id
		) varices on varices.document_id = d.document_id
	left join
		(
			select o.document_id, 
				sum(case when code = 'C0267791' and certainty <> -1 then 1 else 0 end) mvarices_p,
				sum(case when code = 'C0267791' and certainty = -1 then 1 else 0 end) mvarices_n,
				sum(case when code = 'C0014867' and certainty <> -1 then 1 else 0 end) evarices_p,
				sum(case when code = 'C0014867' and certainty = -1 then 1 else 0 end) evarices_n
			from esld.v_document_ontoanno o
			-- limit to terms in the report section and after
			inner join esld.anno_base segda on segda.document_id = o.document_id
			inner join esld.anno_segment seg on seg.anno_base_id = segda.anno_base_id
			where o.code in ('C0267791', 'C0014867')
			and seg.segment_id = 'REPORT'
			and o.span_begin >= segda.span_begin
			group by o.document_id
		) varices2 on varices2.document_id = d.document_id
	where d.analysis_batch = '$(analysis_batch)'
	-- limit to document previously classified as abdominal radiology reports
	and s.task = 'RADIOLOGY_TYPE'
	and (s.class_gold in (2,3) or s.class_auto in (2,3))
	) r
) r
left join esld.abs_radiology ar 
		on r.study_id = ar.studyid 
		and r.uid = ar.uid 
;

