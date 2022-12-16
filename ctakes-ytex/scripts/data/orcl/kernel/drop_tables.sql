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

drop index nk_feature_eval;
drop index ix_feature_eval;
drop trigger trg_feature_eval;

drop index nk_feature_name;
drop index ix_feature_rank;
drop index ix_feature_evaluation;
drop trigger trg_feature_rank;

drop index NK_feature_parent;
drop trigger trg_feature_parchd;

drop sequence feature_eval_sequence;
drop sequence feature_rank_sequence;
drop sequence feature_parchd_sequence;

drop table feature_parchd;
drop table feature_rank;
drop table feature_eval;
