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

create sequence feature_eval_sequence;
create sequence feature_rank_sequence;
create sequence feature_parchd_sequence;

create table feature_eval (
  feature_eval_id int not null primary key,
  corpus_name varchar2(50) not null ,
  featureset_name varchar2(50) default ' ' not null ,
  label varchar2(50) default ' '  not null ,
  cv_fold_id int default 0  not null ,
  param1 DOUBLE PRECISION default 0 not null ,
  param2 varchar2(50) default ' ' not null ,
  type varchar2(50) not null
);
create unique index nk_feature_eval on feature_eval(corpus_name, featureset_name, label, cv_fold_id, param1, param2, type);
create index ix_feature_eval on feature_eval (corpus_name, cv_fold_id, type);

create table  feature_rank (
  feature_rank_id int not null primary key,
  feature_eval_id int not null ,
  feature_name varchar2(50) not null ,
  evaluation DOUBLE PRECISION default 0 not null ,
  rank int default 0 not null ,
  foreign key (feature_eval_id) references feature_eval (feature_eval_id) ON DELETE CASCADE
) ;
create unique index nk_feature_name on  feature_rank(feature_eval_id, feature_name);
create index ix_feature_rank  on  feature_rank(feature_eval_id, rank);
create index ix_feature_evaluation  on  feature_rank(feature_eval_id, evaluation);


CREATE TABLE feature_parchd (
  feature_parchd_id int NOT NULL primary key,
  par_feature_rank_id int NOT NULL ,
  chd_feature_rank_id int NOT NULL
);
create UNIQUE index NK_feature_parent on feature_parchd(par_feature_rank_id,chd_feature_rank_id);
