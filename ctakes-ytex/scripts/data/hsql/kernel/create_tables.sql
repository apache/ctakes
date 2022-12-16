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


CREATE CACHED TABLE  kernel_eval (
	kernel_eval_id IDENTITY,
	corpus_name varchar(50) DEFAULT '' NOT NULL  ,
	experiment varchar(50) not null ,
	label varchar(50) default '' not NULL ,
	cv_fold_id int default 0 not null ,
	param1 double default 0 not null ,
	param2 varchar(50) default '' not null 
) 
;
CREATE unique index NK_kernel_eval on kernel_eval (corpus_name, experiment, label, cv_fold_id, param1, param2)
;

CREATE CACHED TABLE kernel_eval_instance (
	kernel_eval_instance IDENTITY,
	kernel_eval_id int not null ,
	instance_id1 bigint NOT NULL,
	instance_id2 bigint NOT NULL,
	similarity double NOT NULL
) ;
CREATE index IX_kernel_eval1 on kernel_eval_instance(kernel_eval_id, instance_id1);
CREATE index IX_kernel_eval2  on kernel_eval_instance(kernel_eval_id, instance_id2);
create UNIQUE index NK_kernel_eval_instance on kernel_eval_instance(kernel_eval_id, instance_id1, instance_id2);

CREATE CACHED TABLE classifier_eval (
	classifier_eval_id IDENTITY,
	name varchar(50) not null,
	experiment varchar(50) default '' null ,
	fold int null,
	run int null,
	algorithm varchar(50) default '' null ,
	label varchar(50) default '' null ,
	options varchar(1000) default '' null ,
	model LONGVARBINARY null,
	param1 double NULL,
	param2 varchar(50) NULL
) ;

CREATE CACHED TABLE classifier_eval_svm (
	classifier_eval_id int not null ,
	cost double DEFAULT 0,
  	weight varchar(50),
	degree int DEFAULT 0,
	gamma double DEFAULT 0,
	kernel int NULL,
	supportVectors int null,
	vcdim double null
) ;

CREATE CACHED TABLE classifier_eval_semil (
	classifier_eval_id int not null ,
	distance varchar(50),
	degree int default 0 not null ,
	gamma double default 0 not null,
	soft_label bit default 0 not null,
	norm_laplace bit default 0 not null,
	mu double default 0 not null,
	lambda double default 0 not null,
	pct_labeled double default 0 not null
) ;

CREATE CACHED TABLE classifier_eval_ir (
	classifier_eval_ir_id int IDENTITY,
	classifier_eval_id int not null ,
	ir_type varchar(5) not null ,
	ir_class varchar(5) not null ,
	ir_class_id int null ,
	tp int not null,
	tn int not null,
	fp int not null,
	fn int not null,
	ppv double default 0 not null,
	npv double default 0 not null ,
	sens double default 0 not null,
	spec double default 0 not null,
	f1 double default 0 not null
) ;
create unique index NK_classifier_eval_ircls on classifier_eval_ir(classifier_eval_id, ir_type, ir_class);
create index IX_classifier_eval_id on classifier_eval_ir (classifier_eval_id);

CREATE CACHED TABLE classifier_instance_eval (
	classifier_instance_eval_id int identity,
	classifier_eval_id int not null ,
	instance_id bigint not null,
	pred_class_id int not null,
	target_class_id int null
) 
;
create unique index nk_result on classifier_instance_eval(classifier_eval_id, instance_id)
;

CREATE CACHED TABLE classifier_instance_eval_prob (
	classifier_eval_result_prob_id int identity,
	classifier_instance_eval_id int ,
	class_id int not null,
	probability double not null
) ;
create unique index nk_result_prob on classifier_instance_eval_prob (classifier_instance_eval_id, class_id);


CREATE CACHED TABLE cv_fold (
  cv_fold_id int identity,
  corpus_name varchar(50) not null ,
  split_name varchar(50) default '' not null ,
  label varchar(50) default ''  not null ,
  run int default 0 not null ,
  fold int default 0 not null 
)
;
create unique index  nk_cv_fold on cv_fold(corpus_name, split_name, label, run, fold)
;

CREATE CACHED TABLE cv_fold_instance (
  cv_fold_instance_id int identity,
  cv_fold_id int not null,
  instance_id bigint not null,
  train bit default 0 not null 
) 
;
create  unique index nk_cv_fold_instance on cv_fold_instance (cv_fold_id, instance_id, train)
;

CREATE CACHED TABLE cv_best_svm (
  corpus_name varchar(50) NOT NULL,
  label varchar(50) NOT NULL,
  experiment varchar(50) DEFAULT '' NOT NULL,
  f1 double NULL,
  kernel int NULL,
  cost double NULL,
  weight varchar(50) NULL,
  param1 double NULL,
  param2 varchar(50) NULL,
  PRIMARY KEY (corpus_name,label,experiment)
) ;

CREATE CACHED TABLE feature_eval (
  feature_eval_id int identity,
  corpus_name varchar(50) not null ,
  featureset_name varchar(50) DEFAULT '' NOT NULL ,
  label varchar(50) DEFAULT '' NOT NULL  ,
  cv_fold_id int default 0 not null  ,
  param1 double default 0 not null  ,
  param2 varchar(50) DEFAULT '' NOT NULL ,
  type varchar(50) not null
)
;
create unique index nk_feature_eval on feature_eval (corpus_name, featureset_name, label, cv_fold_id, param1, param2, type)
;
create index ix_feature_eval on feature_eval(corpus_name, cv_fold_id, type)
;

CREATE CACHED TABLE feature_rank (
  feature_rank_id int identity,
  feature_eval_id int not null ,
  feature_name varchar(50) not null ,
  evaluation double default 0 not null  ,
  rank int default 0 not null
) ;
create unique index nk_feature_name on feature_rank(feature_eval_id, feature_name);
create index ix_feature_rank on feature_rank(feature_eval_id, rank);
create index ix_feature_evaluation on feature_rank(feature_eval_id, evaluation);
create index fk_feature_eval on feature_rank(feature_eval_id);

CREATE CACHED TABLE feature_parchd (
  feature_parchd_id int IDENTITY,
  par_feature_rank_id int NOT NULL ,
  chd_feature_rank_id int NOT NULL
) 
;
create UNIQUE index NK_feature_parent on feature_parchd(par_feature_rank_id,chd_feature_rank_id)
;

CREATE CACHED TABLE tfidf_doclength (
  tfidf_doclength_id int identity,
  feature_eval_id int NOT NULL ,
  instance_id bigint NOT NULL,
  length int DEFAULT 0 NOT NULL
)
;
create  UNIQUE index nk_instance_id on tfidf_doclength(feature_eval_id,instance_id)
;

CREATE CACHED TABLE hotspot (
  hotspot_id int identity,
  instance_id int not null ,
  anno_base_id int not null ,
  feature_rank_id int not null
) ;
create unique index NK_hotspot on hotspot(instance_id, anno_base_id, feature_rank_id);
create INDEX ix_hotspot_instance_id on hotspot(instance_id);
create INDEX ix_hotspot_anno_base_id on hotspot(anno_base_id);
create INDEX ix_hotspot_feature_rank_id on hotspot(feature_rank_id);

CREATE CACHED TABLE hotspot_instance (
    hotspot_instance_id int identity,
    corpus_name varchar(50) not null,
    experiment varchar(50) DEFAULT '' NOT NULL,
    label varchar(50) DEFAULT '' NOT NULL,
    instance_id int not null,
    max_evaluation double default 0 not null ,
    min_rank int default 0 not null
) 
;
create unique index NK_hotspot_instance on hotspot_instance (corpus_name, experiment, label, instance_id);

CREATE CACHED TABLE hotspot_sentence (
    hotspot_sentence_id int identity,
    hotspot_instance_id int not null ,
    anno_base_id int not null ,
    evaluation double default 0 not null  ,
    rank int default 0 not null 
)
;
create unique index NK_hotspot_sentence on hotspot_sentence(hotspot_instance_id, anno_base_id);
create index FK_hotspot_instance_id on hotspot_sentence(hotspot_instance_id);
create index FK_anno_base_id on hotspot_sentence(anno_base_id);
create index IX_evaluation on hotspot_sentence(hotspot_instance_id, evaluation);
create index IX_rank on hotspot_sentence(hotspot_instance_id, rank);

CREATE CACHED TABLE corpus_doc (
	corpus_name varchar(50) not null ,
	instance_id bigint not null ,
	doc_text LONGVARCHAR,
	doc_group varchar(50) NULL,
	primary key (corpus_name, instance_id)
)
;
create index IX_doc_group on corpus_doc(corpus_name, doc_group)
;

CREATE CACHED TABLE corpus_label (
	corpus_name varchar(50) not null ,
	instance_id bigint not null ,
	label varchar(20) DEFAULT '' NOT NULL,
	class varchar(5) DEFAULT '' NOT NULL,
	primary key (corpus_name, instance_id, label)
)
;
create index FK_corpus_doc on corpus_label(corpus_name, instance_id)
;

create view v_corpus_group_class
as
select distinct d.corpus_name, l.label, doc_group, class
from corpus_doc d
inner join corpus_label l 
    on d.corpus_name = l.corpus_name 
    and d.instance_id = l.instance_id
;
