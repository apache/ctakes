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

-- clean up
delete from esld.document_class
where document_id in
(select document_id from esld.document where analysis_batch='$(analysis_batch)')
and task = 'ASCITES'
;

-- filter to only documents classified as radiology reports (depends on RADIOLOGY_IR being run first)
-- left join on chart reviews
-- if no matching chart reviews, assume negative ascites as gold standard
-- count asserted and negated ascites cuis in each document
-- ascites positive if # assertions > # negations
insert into esld.document_class (document_id, task, class_auto, class_gold)
select 
	r.document_id, 
	'ASCITES',
	r.ascites class_auto,
	coalesce(ar.ascites_reported, 0) class_gold 
from
( 
select
	r.document_id,
	r.study_id,
	r.uid,
	case
		when ascites_p > ascites_n then 1
		else 0
	end ascites
from
	(
	select
		d.document_id, 
		d.study_id,
		d.uid,
		coalesce(ascites.ascites_p, 0) ascites_p,
		coalesce(ascites.ascites_n, 0) ascites_n
	from esld.v_document d
	inner join esld.ref_document_type doctype on d.document_type_id = doctype.document_type_id
	-- only include abdominal radiology documents 
	inner join esld.document_class s on s.document_id = d.document_id
	left join
		(
		select o.document_id, 
			sum(case when certainty <> -1 then 1 else 0 end) ascites_p,
			sum(case when certainty = -1 then 1 else 0 end) ascites_n
		from esld.v_document_ontoanno o
		-- limit to terms in the report section and after
		inner join esld.anno_base segda on segda.document_id = o.document_id
		inner join esld.anno_segment seg on seg.anno_base_id = segda.anno_base_id
		where code in ('C0003962', 'C0003964', 'C0401020')
		and seg.segment_id = 'REPORT'
		and o.span_begin >= segda.span_begin
		group by o.document_id
		) ascites on ascites.document_id = d.document_id
	where d.analysis_batch = '$(analysis_batch)'
	-- limit to document previously classified as abdominal radiology reports
	and s.task = 'RADIOLOGY_TYPE'
	and (s.class_gold <> 0 or s.class_auto <> 0 )
	) r
) r
left join esld.abs_radiology ar 
		on r.study_id = ar.studyid 
		and r.uid = ar.uid 
