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

-- create a feature_eval record for the concept graph
delete r
from feature_eval e inner join feature_rank r on e.feature_eval_id = r.feature_eval_id
where param2 = '@ytex.conceptGraphName@' 
and type = 'intrinsic-infocontent';

delete e
from feature_eval e
where param2 = '@ytex.conceptGraphName@' 
and type = 'intrinsic-infocontent';

insert into feature_eval (corpus_name, param2, type) values ('', '@ytex.conceptGraphName@', 'intrinsic-infocontent');

-- copy the feature_rank records from tmp_ic
insert into feature_rank (feature_eval_id, feature_name, evaluation, rank)
select feature_eval_id, feature_name, evaluation, rank
from feature_eval, tmp_ic
where param2 = '@ytex.conceptGraphName@' and type = 'intrinsic-infocontent';

-- cleanup
drop table tmp_ic;