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
 * update the abs_radiology_review table with 'false positives'
 * manually review these to see if these are indeed false positives
 */
insert into esld.abs_radiology_review (uid, studyid, rad_procedure_type_id)
select d.uid, d.study_id, c.class_auto
from esld.document_class c
inner join esld.v_document d on c.document_id = d.document_id
left join esld.abs_radiology_review r on r.uid = d.uid and r.studyid = d.study_id
where c.class_gold = 0
and c.class_auto <> c.class_gold
and c.task = 'RADIOLOGY_TYPE'
and d.analysis_batch = '$(analysis_batch)'
and r.id is null
;