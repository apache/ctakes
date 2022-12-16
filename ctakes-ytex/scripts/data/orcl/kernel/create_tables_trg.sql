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
create trigger trg_feature_eval before insert on feature_eval
for each row
when (new.feature_eval_id is null)
begin
 select feature_eval_sequence.nextval into :new.feature_eval_id from dual;
end;
/


create trigger trg_feature_rank before insert on feature_rank
for each row
when (new.feature_rank_id is null)
begin
 select feature_rank_sequence.nextval into :new.feature_rank_id from dual;
end;
/

create trigger trg_feature_parchd before insert on feature_parchd
for each row
when (new.feature_parchd_id is null)
begin
 select feature_parchd_sequence.nextval into :new.feature_parchd_id from dual;
end;
/
