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
and task = 'RADIOLOGY_TYPE'
;

-- insert the classification for all radiology documents for this analysis batch
-- set the gold class to empty for now
insert into esld.document_class (document_id, task, class_auto, class_gold)
select
	r.document_id,
	'RADIOLOGY_TYPE' task,
	case 
	-- this document has a reference to another document - throw it out
		when docref_count > 0 and doc_len < 500 then 0
		when title_biopsy_count > 0 then 0
	-- first see if abdomen or liver and procedure in title
		when title_abds_count > 0 and title_ultrasound_count > 0 then 1
		when title_abds_count > 0 and title_ct_count > 0 then 3
		when title_abds_count > 0 and title_mri_count > 0 then 2
		when title_mrcp_count > 0 then 2
	-- first see if some abdominal term and procedure in title, and liver or abdomen in body
		when title_abd_count > 0 and title_ultrasound_count > 0 and abds_count > 0 then 1
		when title_abd_count > 0 and title_ct_count > 0 and abds_count > 0 then 3
		when title_abd_count > 0 and title_mri_count > 0 and abds_count > 0 then 2
	-- then see if abdomen in title and procedure somewhere in body
	/*
		when title_abd_count > 0 and mri_count > 0 then 2
		when title_abd_count > 0 and ct_count > 0 then 3
		when title_abd_count > 0 and ultrasound_count > 0 then 1
	*/
		when mri_sent_count > 0 and abds_count > 0 then 2
		when ct_sent_count > 0 and abds_count > 0 then 3
		when us_sent_count > 0 and abds_count > 0 then 1
		else 0
	end class_auto,
	0 class_gold
from
(
	select 	
		d.document_id, 
		d.doc_len,
		coalesce(title_biopsy_count,0) title_biopsy_count,
		coalesce(title_abd_count,0) title_abd_count,
		coalesce(title_abds_count,0) title_abds_count,
		coalesce(title_ultrasound_count,0) title_ultrasound_count,
		coalesce(title_ct_count,0) title_ct_count,
		coalesce(title_mri_count,0) title_mri_count,
		coalesce(title_mrcp_count,0) title_mrcp_count,
		coalesce(sentence.mri_sent_count, 0) mri_sent_count,
		coalesce(sentence.ct_sent_count, 0) ct_sent_count,
		coalesce(sentence.us_sent_count, 0) us_sent_count,
		coalesce(s.ultrasound_count, 0) ultrasound_count,
		coalesce(s.mri_count, 0) mri_count,
		coalesce(s.ct_count, 0) ct_count,
		coalesce(s.docref_count, 0) docref_count,
		coalesce(abds.abds_count, 0) abds_count
	from 
		(
			select d.*, len(d.doc_text) doc_len
			from esld.v_document d
			where d.analysis_batch = '$(analysis_batch)'
			and d.copy_of_document_id is null
			and d.document_type_name = 'RADIOLOGY'
		) d
		left join 
		-- radiology and abdomen terms in title
		(
			select document_id, 
				sum(us) title_ultrasound_count, 
				sum(mri) title_mri_count,
				sum(mrcp) title_mrcp_count,
				sum(ct) title_ct_count,
				sum(abd) title_abd_count,
				sum(abds) title_abds_count,
				sum(biopsy) title_biopsy_count
			from
			(
				select oda.document_id, 
					case when oa.code in ('C0041618', 'C0554756') then 1 else 0 end us,
					case when oa.code in ('C0024485', 'C0994163', 'C0243032') then 1 else 0 end mri,
					case when oa.code in ('C0994163') then 1 else 0 end mrcp,
					case when oa.code in ('C0040405') then 1 else 0 end ct,
					case when oa.code in ('C1278929', 'C0023884', 'C0000726', 'C1281594', 'C0017189', 'C1281182', 'C0817096', 'C0449202', 'C1279864', 'C0030797', 'C0581480', 'C0035359', 'C0439734')
						then 1 else 0 end abd,
					case when oa.code in ('C0000726', 'C0023884', 'C0230165', 'C0439734')
						then 1 else 0 end abds,
					case when oa.code in ('C0005560')
						then 1 else 0 end biopsy
				from esld.anno_base oda 
				inner join esld.anno_ontology_concept oa on oda.anno_base_id = oa.anno_base_id
				inner join esld.anno_base tda 
					on oda.document_id = tda.document_id
					and oda.span_begin >= tda.span_begin 
					and oda.span_end <= tda.span_end
				inner join esld.ref_uima_type ut on tda.uima_type_id = ut.uima_type_id
				where ut.uima_type_name = 'gov.va.vacs.esld.uima.types.DocumentTitle'
				and oa.code in 
					( 
					'C0041618', 'C0554756', /* ultrasound */ 
					'C0024485', 'C0994163', 'C0243032', /* mri */
					'C0040405', /* ct */ 
					'C1278929', 'C0023884', 'C0000726', 'C1281594', 'C0017189', 'C1281182', 'C0817096', 'C0449202', 'C1279864', 'C0030797', 'C0581480', 'C0035359', 'C0439734', /* abdomen */
					'C0005560' /* biopsy */
					)
			) t
			group by t.document_id
		) title_rad 
			on title_rad.document_id = d.document_id 
		left join 
		-- radiology and abdomen terms in a sentence
		(
			select document_id, 
				sum(coalesce(us,0)) us_sent_count, 
				sum(coalesce(mri,0)) mri_sent_count,
				sum(coalesce(ct,0)) ct_sent_count
			from
			(
				select oda.document_id, 
					case when oa.code in ('C0041618', 'C0554756') then 1 else 0 end us,
					case when oa.code in ('C0024485', 'C0994163', 'C0243032') then 1 else 0 end mri,
					case when oa.code in ('C0040405') then 1 else 0 end ct
				from 
				-- radiology annotion
				esld.anno_base oda 
				inner join esld.anno_ontology_concept oa on oda.anno_base_id = oa.anno_base_id
				-- abdomen annotation
				inner join esld.anno_base oda_abd on oda_abd.document_id = oda.document_id
				inner join esld.anno_ontology_concept oa_abd on oda_abd.anno_base_id = oa_abd.anno_base_id
				-- segment annotation
				inner join esld.anno_base segda on segda.document_id = oda.document_id
				inner join esld.anno_segment seg on seg.anno_base_id = segda.anno_base_id 
				-- sentence annotation
				inner join esld.anno_base s on oda.document_id = s.document_id
				inner join esld.ref_uima_type ut on s.uima_type_id = ut.uima_type_id
				where ut.uima_type_name = 'org.apache.ctakes.typesystem.type.textspan.Sentence'
				-- get the report section
				and seg.segment_id = 'REPORT'
				-- radiology & abdomen annotations in same sentence
				and oda.span_begin >= s.span_begin 
				and oda.span_end <= s.span_end
				and oda_abd.span_begin >= s.span_begin 
				and oda_abd.span_end <= s.span_end
				-- sentence within 200 characters of report beginning
				and s.span_end <= (segda.span_begin + 200)
				and s.span_begin >= segda.span_begin 
				and oa.code in 
					( 
					'C0041618', 'C0554756', /* ultrasound */ 
					'C0024485', 'C0994163', 'C0243032', /* mri */
					'C0040405' /* ct */ 
					)
				and oa_abd.code in
					('C1278929', 'C0023884', 'C0000726', 'C1281594', 'C0017189', 'C1281182', 'C0817096', 'C0449202', 'C1279864', 'C0030797', 'C0581480', 'C0035359', 'C0439734') /* abdomen */
			) s
			group by s.document_id
		) sentence
			on sentence.document_id = d.document_id 
		left join 
		-- radiology term somewher in document
		(
			select document_id, 
				sum(us) ultrasound_count, 
				sum(mri) mri_count,
				sum(ct) ct_count,
				sum(docref) docref_count
			from
			(
				select oda.document_id, 
					case when oa.code = 'C0041618' then 1 else 0 end us,
					case when oa.code in ('C0024485', 'C0994163', 'C0243032') then 1 else 0 end mri,
					case when oa.code in ('C0040405') then 1 else 0 end ct,
					case when oa.code in ('DOCREF') then 1 else 0 end docref
				from esld.anno_base oda 
				inner join esld.anno_ontology_concept oa on oda.anno_base_id = oa.anno_base_id
				-- segment annotation
				inner join esld.anno_base segda on segda.document_id = oda.document_id
				inner join esld.anno_segment seg on seg.anno_base_id = segda.anno_base_id 
				where oa.code in 
					( 
					'C0041618', /* ultrasound */ 
					'C0024485', 'C0994163', 'C0243032', /* mri */
					'C0040405', /* ct */ 
					'DOCREF' /* document reference - see if the document is a stub */
					)
				and seg.segment_id = 'REPORT'
				and oda.span_begin >= segda.span_begin
				and oda.span_end <= (segda.span_begin + 200)
			) s
			group by s.document_id
		) s on s.document_id = d.document_id 
		left join
		(
			select oda.document_id, 
				count(*) abds_count
			from esld.anno_base oda 
			inner join esld.anno_ontology_concept oa on oda.anno_base_id = oa.anno_base_id
			-- segment annotation
			inner join esld.anno_base segda on segda.document_id = oda.document_id
			inner join esld.anno_segment seg on seg.anno_base_id = segda.anno_base_id 
			where oa.code in 
				( 
				'C0000726', 'C0023884', 'C0230165', 'C0439734', /* abdomen/liver terms */
				'C0003962', 'C0003964', /* ascites terms */
				'C0014867' /* varices terms */
				)
			and seg.segment_id = 'REPORT'
			and oda.span_begin >= segda.span_begin
			group by oda.document_id
		) abds on abds.document_id = d.document_id 		
) r
;

-- update the gold standard classifications
update esld.document_class
set class_gold = 
	(
	case 
		when ar.procedure_type = 'Ultrasound' then 1 
		when ar.procedure_type = 'MRI' then 2
		when ar.procedure_type = 'CT' then 3
	end
	)
	from esld.document_class c
	inner join esld.v_document d 
		on c.document_id = d.document_id
	inner join esld.abs_radiology ar 
		on d.study_id = ar.studyid 
		and d.uid = ar.uid 
	where c.task = 'RADIOLOGY_TYPE'
	and d.analysis_batch='$(analysis_batch)'
;


-- delete classifications for documents where the gold standard is 'unclear'
-- these are radiology documents for which there are multiple potential
-- matches
delete esld.document_class
from esld.document_class
inner join esld.v_document d 
	on esld.document_class.document_id = d.document_id
inner join esld.abs_radiology ar 
	on d.study_id = ar.studyid 
	and ar.uid is null
	and datepart(yyyy, d.doc_date) = datepart(yyyy, ar.procedure_date)
	and datepart(dy, d.doc_date) = datepart(dy,ar.procedure_date)
where esld.document_class.task = 'RADIOLOGY_TYPE'
and d.analysis_batch='$(analysis_batch)'
;

/*
 * Even if the document is linked to the chart review, 
 * there may be multiple abdominal radiology  documents for the given date.
 * Delete other documents for the date, because they may be abdominal radiology
 * documents, but were not reviewed (no gold standard for these)
 */
delete esld.document_class
from esld.document_class
inner join esld.v_document d 
	on esld.document_class.document_id = d.document_id
inner join esld.abs_radiology ar 
	on d.study_id = ar.studyid 
	and datepart(yyyy, d.doc_date) = datepart(yyyy, ar.procedure_date)
	and datepart(dy, d.doc_date) = datepart(dy,ar.procedure_date)
where esld.document_class.task = 'RADIOLOGY_TYPE'
and d.analysis_batch='$(analysis_batch)'
and d.uid <> ar.uid
;

/*
 * The chart review was only on notes 1 year prior to the 1st abnormal lab/icd-9.
 * We currently don't have those dates; so just use the earliest note for the study id 
 * as the cutoff date
 */
delete esld.document_class
from esld.document_class
inner join esld.v_document d 
	on esld.document_class.document_id = d.document_id
inner join
	(
	select ar.studyid, min(ar.procedure_date) procedure_date
	from esld.abs_radiology ar
	group by ar.studyid
	) mindate on mindate.studyid = d.study_id
where d.doc_date < mindate.procedure_date
and d.analysis_batch='$(analysis_batch)'
;

