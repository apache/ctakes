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

/*
 * Liver Mass?
 * structure in right upper abdomen related to liver could be gall bladder filled with echogenic sludge
 * However, there is an area of decreased attenuation seen at the junction of the anterior segment of the right lobe of the liver and the medial segment of the left lobe of the liver, measuring approximately 1.5 cm in diameter
 * A rounded, echogenic focus is seen in the left lobe of the liver,
 * 
 * Not a liver Mass:
 * Cirrhotic appearing liver without evidence of any abnormal arterial phase enhancement to suggest the presence of a hepatoma.  
 * There are no abnormal areas of early arterial enhancement within the liver to suggest focal hepatocellular carcinoma
 * Small lesion in the liver, possibly a cyst
 */

-- clean up
/*
 * Liver Mass?
 * structure in right upper abdomen related to liver could be gall bladder filled with echogenic sludge
 * However, there is an area of decreased attenuation seen at the junction of the anterior segment of the right lobe of the liver and the medial segment of the left lobe of the liver, measuring approximately 1.5 cm in diameter
 * A rounded, echogenic focus is seen in the left lobe of the liver,
 * 
 * Not a liver Mass:
 * Cirrhotic appearing liver without evidence of any abnormal arterial phase enhancement to suggest the presence of a hepatoma.  
 * There are no abnormal areas of early arterial enhancement within the liver to suggest focal hepatocellular carcinoma
 * Small lesion in the liver, possibly a cyst
 */

-- clean up
delete from esld.document_class
where document_id in
(select document_id from esld.document where analysis_batch='$(analysis_batch)')
and task = 'LIVER_MASSES'
;

insert into esld.document_class (document_id, task, class_auto, class_gold)
select 
	r.document_id, 
	'LIVER_MASSES',
	r.LIVER_MASSES class_auto ,
	coalesce(ar.LIVER_MASSES_reported, 0) class_gold
from
( 
	select
		r.document_id,
		r.study_id,
		r.uid,
		case
			when r.LIVER_MASSES_p > 0 and r.LIVER_MASSES_p > r.LIVER_MASSES_n then 1
			else 0
		end LIVER_MASSES
	from
	(
		select
			d.document_id, 
			d.study_id,
			d.uid,
			coalesce(l.LIVER_MASSES_p, 0)+coalesce(l2.LIVER_MASSES_p, 0) LIVER_MASSES_p,
			coalesce(l.LIVER_MASSES_n, 0)+coalesce(l2.LIVER_MASSES_n, 0) LIVER_MASSES_n
		from esld.v_document d
		inner join esld.document_class c on d.document_id = c.document_id
		left join 
		-- single-term liver masses (like hepatoma)
		(
			select da.document_id, 
			sum(case when ne.certainty <> -1 then 1 else 0 end) LIVER_MASSES_p, 
			sum(case when ne.certainty = -1 then 1 else 0 end) LIVER_MASSES_n
			FROM ESLD.anno_base AS da 
			INNER JOIN ESLD.anno_named_entity AS ne ON da.anno_base_id = ne.anno_base_id 
			INNER JOIN ESLD.anno_ontology_concept AS o ON o.anno_base_id = ne.anno_base_id
			inner join esld.anno_base sda on sda.document_id = da.document_id
			inner join esld.anno_segment s 
				on sda.anno_base_id = s.anno_base_id 
				and s.segment_id = 'REPORT'
			where o.code in ('C0240225' /*, 'C0019204', 'C0023903' */)
			and da.span_begin >= sda.span_begin
			group by da.document_id
		) l on l.document_id = d.document_id
		-- count for multiple-term liver mass (like hepatic lesion)
		-- look for sentences that have both words
		left join 
		(
			select document_id, sum(LIVER_MASSES_p) LIVER_MASSES_p, sum(LIVER_MASSES_n) LIVER_MASSES_n
			from
			(
				-- select distinct to avoid duplicate counts due to joins
				select distinct s.document_id, s.anno_base_id,
					(case when mass.certainty <> -1 then 1 else 0 end) LIVER_MASSES_p, 
					(case when mass.certainty = -1 then 1 else 0 end) LIVER_MASSES_n
				from esld.v_annotation s
				inner join esld.v_document_ontoanno liv
					on liv.document_id = s.document_id
					and liv.span_begin >= s.span_begin
					and liv.span_end <= s.span_end
				inner join esld.v_document_ontoanno mass
					on mass.document_id = s.document_id
					and mass.span_begin >= s.span_begin
					and mass.span_end <= s.span_end
				inner join esld.anno_base sda on sda.document_id = s.document_id
				inner join esld.anno_segment seg
					on sda.anno_base_id = seg.anno_base_id 
					and seg.segment_id = 'REPORT'
				where s.uima_type_id in 
					(select uima_type_id 
					from esld.ref_uima_type 
					where uima_type_name = 'org.apache.ctakes.typesystem.type.textspan.Sentence')
				and liv.code in ('C1278929', 'C0023884', 'C0205054' /* hepatic */, 'C0227486' /* left lobe */, 'C0227481' /* right lobe */)
				and mass.code in ('C0221198', 'C0577559', 'ESLD_MASS')
				and mass.span_begin >= sda.span_begin
			) l2 
			group by document_id
		) l2 on l2.document_id = d.document_id
		where d.analysis_batch = '$(analysis_batch)'
		and c.task = 'RADIOLOGY_TYPE'
		and (c.class_gold <> 0 or c.class_auto <> 0)
	) r
) r
left join esld.abs_radiology ar 
		on r.study_id = ar.studyid 
		and r.uid = ar.uid 
;