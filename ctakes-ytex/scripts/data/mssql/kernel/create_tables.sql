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


CREATE TABLE  $(db_schema).kernel_eval (
	kernel_eval_id int identity NOT NULL,
	corpus_name varchar(50) NOT NULL DEFAULT '' ,
	experiment varchar(50) not null ,
	label varchar(50) not NULL default '' ,
	cv_fold_id int not null default 0 ,
	param1 float not null default 0,
	param2 varchar(50) not null default '',
	PRIMARY KEY (kernel_eval_id)
);
create UNIQUE index NK_kernel_eval on  $(db_schema).kernel_eval (corpus_name, experiment, label, cv_fold_id, param1, param2);

create table  $(db_schema).kernel_eval_instance (
	kernel_eval_instance int not null identity primary key,
	kernel_eval_id int not null,
	instance_id1 bigint NOT NULL,
	instance_id2 bigint NOT NULL,
	similarity float NOT NULL,
	foreign key (kernel_eval_id) references $(db_schema).kernel_eval (kernel_eval_id) ON DELETE CASCADE
);
create index IX_kernel_eval1 on  $(db_schema).kernel_eval_instance (kernel_eval_id, instance_id1);
create index IX_kernel_eval2 on   $(db_schema).kernel_eval_instance (kernel_eval_id, instance_id2);
create UNIQUE index NK_kernel_eval on  $(db_schema).kernel_eval_instance(kernel_eval_id, instance_id1, instance_id2);

create table $(db_schema).classifier_eval (
	classifier_eval_id int identity not null primary key,
	name varchar(50) not null,
	experiment varchar(50) null default '',
	fold int null,
	run int null,
	algorithm varchar(50) null default '',
	label varchar(50) null default '',
	options varchar(1000) null default '',
	model varbinary(max) null,
	param1 float NULL,
	param2 varchar(50) NULL
);

create table $(db_schema).classifier_eval_svm (
	classifier_eval_id int not null primary key ,
	cost float DEFAULT '0',
  	weight varchar(50),
	degree int DEFAULT '0',
	gamma float DEFAULT '0',
	kernel int DEFAULT NULL,
	supportVectors int default null,
	vcdim float null,
	foreign key (classifier_eval_id) references $(db_schema).classifier_eval (classifier_eval_id) ON DELETE CASCADE
);

create table $(db_schema).classifier_eval_semil (
	classifier_eval_id int not null primary key,
	distance varchar(50),
	degree int not null default 0,
	gamma float not null default 0,
	soft_label bit not null default 0,
	norm_laplace bit not null default 0,
	mu float not null default 0,
	lambda float not null default 0,
	pct_labeled float not null default 0,
	foreign key (classifier_eval_id) references $(db_schema).classifier_eval (classifier_eval_id) ON DELETE CASCADE
) ;

create table $(db_schema).classifier_eval_ir (
	classifier_eval_ir_id int identity not null primary key,
	classifier_eval_id int not null ,
	ir_type varchar(5) not null ,
	ir_class varchar(5) not null ,
	ir_class_id int null ,
	tp int not null,
	tn int not null,
	fp int not null,
	fn int not null,
	ppv float not null default 0,
	npv float not null default 0,
	sens float not null default 0,
	spec float not null default 0,
	f1 float not null default 0,
	foreign key (classifier_eval_id) references $(db_schema).classifier_eval (classifier_eval_id) ON DELETE CASCADE
);
create unique index NK_classifier_eval_ircls on  $(db_schema).classifier_eval_ir(classifier_eval_id, ir_type, ir_class);


create table $(db_schema).classifier_instance_eval (
	classifier_instance_eval_id int identity not null primary key,
	classifier_eval_id int not null ,
	instance_id bigint not null,
	pred_class_id int not null,
	target_class_id int null,
	foreign key (classifier_eval_id) references $(db_schema).classifier_eval (classifier_eval_id) ON DELETE CASCADE
);
create unique index nk_result on  $(db_schema).classifier_instance_eval(classifier_eval_id, instance_id);

create table $(db_schema).classifier_instance_eval_prob (
	classifier_eval_result_prob_id int identity not null primary key,
	classifier_instance_eval_id int ,
	class_id int not null,
	probability float not null,
	foreign key (classifier_instance_eval_id) references $(db_schema).classifier_instance_eval (classifier_instance_eval_id) ON DELETE CASCADE
);
create unique index nk_result_prob on  $(db_schema).classifier_instance_eval_prob(classifier_instance_eval_id, class_id);

create table $(db_schema).cv_fold (
  cv_fold_id int identity not null primary key,
  corpus_name varchar(50) not null ,
  split_name varchar(50) not null default '' ,
  label varchar(50) not null default '' ,
  run int not null default 0,
  fold int not null default 0
);
create unique index nk_cv_fold on  $(db_schema).cv_fold (corpus_name, split_name, label, run, fold);

create table $(db_schema).cv_fold_instance (
  cv_fold_instance_id int identity not null primary key,
  cv_fold_id int not null,
  instance_id bigint not null,
  train bit not null default 0,
  foreign key (cv_fold_id) references $(db_schema).cv_fold (cv_fold_id) ON DELETE CASCADE
);
create unique index nk_cv_fold_instance on $(db_schema).cv_fold_instance (cv_fold_id, instance_id, train);

create table $(db_schema).cv_best_svm (
  corpus_name varchar(50) NOT NULL,
  label varchar(50) NOT NULL,
  experiment varchar(50) NOT NULL DEFAULT '',
  f1 float DEFAULT NULL,
  kernel int DEFAULT NULL,
  cost float DEFAULT NULL,
  weight varchar(50) DEFAULT NULL,
  param1 float DEFAULT NULL,
  param2 varchar(50) DEFAULT NULL,
  PRIMARY KEY (corpus_name,label,experiment)
) ;

create table $(db_schema).feature_eval (
  feature_eval_id int identity not null primary key,
  corpus_name varchar(50) not null ,
  featureset_name varchar(50) not null default '' ,
  label varchar(50) not null default ''  ,
  cv_fold_id int not null default 0 ,
  param1 float not null default 0 ,
  param2 varchar(50) not null default '' ,
  type varchar(50) not null,
);
create unique index nk_feature_eval on $(db_schema).feature_eval(corpus_name, featureset_name, label, cv_fold_id, param1, param2, type);
create index ix_feature_eval on $(db_schema).feature_eval (corpus_name, cv_fold_id, type);

create table  $(db_schema).feature_rank (
  feature_rank_id int identity not null primary key,
  feature_eval_id int not null ,
  feature_name varchar(50) not null ,
  evaluation float not null default 0 ,
  rank int not null default 0,
  foreign key (feature_eval_id) references $(db_schema).feature_eval (feature_eval_id) ON DELETE CASCADE
) ;
create unique index nk_feature_name on  $(db_schema).feature_rank(feature_eval_id, feature_name);
create index ix_feature_rank  on  $(db_schema).feature_rank(feature_eval_id, rank);
create index ix_feature_evaluation  on  $(db_schema).feature_rank(feature_eval_id, evaluation);

CREATE TABLE $(db_schema).feature_parchd (
  feature_parchd_id int identity NOT NULL primary key,
  par_feature_rank_id int NOT NULL ,
  chd_feature_rank_id int NOT NULL
);
create UNIQUE index NK_feature_parent on $(db_schema).feature_parchd(par_feature_rank_id,chd_feature_rank_id);

CREATE TABLE $(db_schema).tfidf_doclength (
  tfidf_doclength_id int NOT NULL identity primary key,
  feature_eval_id int NOT NULL ,
  instance_id bigint NOT NULL,
  length int NOT NULL DEFAULT '0',
  foreign key (feature_eval_id) references $(db_schema).feature_eval (feature_eval_id) ON DELETE CASCADE
)
;
create UNIQUE index nk_instance_id  on $(db_schema).tfidf_doclength(feature_eval_id,instance_id)

create table $(db_schema).hotspot (
  hotspot_id int identity not null primary key,
  instance_id int not null ,
  anno_base_id int not null ,
  feature_rank_id int not null
);
create unique index NK_hotspot on $(db_schema).hotspot (instance_id, anno_base_id, feature_rank_id);
create index ix_instance_id on $(db_schema).hotspot (instance_id);
create index ix_anno_base_id on $(db_schema).hotspot (anno_base_id);
create index ix_feature_rank_id on $(db_schema).hotspot (feature_rank_id);

create table $(db_schema).hotspot_instance (
    hotspot_instance_id int identity primary key,
    corpus_name varchar(50) not null,
    experiment varchar(50) not null default '',
    label varchar(50) not null default '',
    instance_id int not null,
    max_evaluation float not null default 0,
    min_rank int not null default 0
);
create unique index NK_hotspot_instance on $(db_schema).hotspot_instance (corpus_name, experiment, label, instance_id);
    
create table $(db_schema).hotspot_sentence (
    hotspot_sentence_id int identity not null primary key,
    hotspot_instance_id int not null ,
    anno_base_id int not null ,
    evaluation float not null default 0 ,
    rank int not null default 0 ,
	foreign key (hotspot_instance_id) references $(db_schema).hotspot_instance (hotspot_instance_id) ON DELETE CASCADE
) ;
create unique index NK_hotspot_sentence on $(db_schema).hotspot_sentence (hotspot_instance_id, anno_base_id);
create index FK_anno_base_id on $(db_schema).hotspot_sentence (anno_base_id);
create INDEX IX_evaluation on $(db_schema).hotspot_sentence (hotspot_instance_id, evaluation);
create INDEX IX_rank on $(db_schema).hotspot_sentence (hotspot_instance_id, rank);

create table $(db_schema).corpus_doc (
	corpus_name varchar(50) not null ,
	instance_id bigint not null ,
	doc_text nvarchar(max),
	doc_group varchar(50) DEFAULT NULL ,
	primary key (corpus_name, instance_id)
) ;
create index IX_doc_group on $(db_schema).corpus_doc(corpus_name, doc_group);

create table $(db_schema).corpus_label (
	corpus_name varchar(50) not null ,
	instance_id bigint not null ,
	label varchar(20) not null default '',
	class varchar(5) not null default '',
	primary key (corpus_name, instance_id, label)
); 
create index FK_corpus_doc on $(db_schema).corpus_label(corpus_name, instance_id);

go

create view $(db_schema).v_corpus_group_class
as
select distinct d.corpus_name, l.label, doc_group, class
from $(db_schema).corpus_doc d
inner join $(db_schema).corpus_label l 
    on d.corpus_name = l.corpus_name 
    and d.instance_id = l.instance_id
;
