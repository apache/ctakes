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

-- insert triggers to generate primary keys from sequence
create or replace trigger trg_anno_ontology_concept before insert on anno_ontology_concept
for each row
when (new.anno_ontology_concept_id is null)
begin
 select anno_onto_concept_id_sequence.nextval into :new.anno_ontology_concept_id from dual;
end;
/

create or replace trigger trg_anno_link before insert on anno_link
for each row
when (new.anno_link_id is null)
begin
 select anno_link_id_sequence.nextval into :new.anno_link_id from dual;
end;
/

create or replace trigger trg_fracture_demo before insert on fracture_demo
for each row
when (new.note_id is null)
begin
 select demo_note_id_sequence.nextval into :new.note_id from dual;
end;
/

create or replace trigger trg_anno_mm_cuiconcept before insert on anno_mm_cuiconcept
for each row
when (new.anno_mm_cuiconcept_id is null)
begin
 select anno_mm_cuiconcept_id_sequence.nextval into :new.anno_mm_cuiconcept_id from dual;
end;
/
