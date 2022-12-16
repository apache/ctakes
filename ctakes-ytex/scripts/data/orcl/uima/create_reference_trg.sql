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

create or replace trigger trg_ref_named_entity_regex before insert on ref_named_entity_regex
for each row
when (new.named_entity_regex_id is null)
begin
 select named_entity_regex_id_sequence.nextval into :new.named_entity_regex_id from dual;
end;
/

create or replace trigger trg_ref_segment_regex before insert on ref_segment_regex
for each row
when (new.segment_regex_id is null)
begin
 select segment_regex_id_sequence.nextval into :new.segment_regex_id from dual;
end;
/