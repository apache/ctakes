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


CREATE TABLE  kernel_eval (
	kernel_eval_id int(11) NOT NULL AUTO_INCREMENT,
	corpus_name varchar(50) NOT NULL DEFAULT '' comment 'corpus name',
	experiment varchar(50) not null comment 'experiment - type of kernel',
	label varchar(50) not NULL default '' comment 'class label',
	cv_fold_id int not null default 0 comment 'fk cv_fold',
	param1 double not null default 0,
	param2 varchar(50) not null default '',
	PRIMARY KEY (kernel_eval_id),
	UNIQUE KEY NK_kernel_eval (corpus_name, experiment, label, cv_fold_id, param1, param2)
) ENGINE=MyISAM comment 'set of all kernel evaluations';

create table kernel_eval_instance (
	kernel_eval_instance int not null auto_increment primary key,
	kernel_eval_id int not null comment 'fk kernel_eval',
	instance_id1 bigint NOT NULL,
	instance_id2 bigint NOT NULL,
	similarity double NOT NULL,
	KEY IX_kernel_eval1 (kernel_eval_id, instance_id1),
	KEY IX_kernel_eval2 (kernel_eval_id, instance_id2),
	UNIQUE KEY NK_kernel_eval (kernel_eval_id, instance_id1, instance_id2)
) ENGINE=MyISAM comment 'kernel instance evaluation';

create table classifier_eval (
	classifier_eval_id int AUTO_INCREMENT not null primary key,
	name varchar(50) not null,
	experiment varchar(50) null default "",
	fold int null,
	run int null,
	algorithm varchar(50) null default "",
	label varchar(50) null default "",
	options varchar(1000) null default "",
	model longblob null,
	param1 double NULL,
	param2 varchar(50) NULL
) engine=myisam comment 'evaluation of a classifier on a dataset';

create table classifier_eval_svm (
	classifier_eval_id int not null comment 'fk classifier_eval' primary key,
	cost double DEFAULT '0',
  	weight varchar(50),
	degree int DEFAULT '0',
	gamma double DEFAULT '0',
	kernel int DEFAULT NULL,
	supportVectors int default null,
	vcdim double null
) engine=myisam comment 'evaluation of a libsvm classifier on a dataset';

create table classifier_eval_semil (
	classifier_eval_id int not null comment 'fk classifier_eval' primary key,
	distance varchar(50),
	degree int not null default 0,
	gamma double not null default 0,
	soft_label bit not null default 0,
	norm_laplace bit not null default 0,
	mu double not null default 0,
	lambda double not null default 0,
	pct_labeled double not null default 0
) engine=myisam comment 'evaluation of a semil classifier on a dataset';

create table classifier_eval_ir (
	classifier_eval_ir_id int not null auto_increment primary key,
	classifier_eval_id int not null comment 'fk classifier_eval',
	ir_type varchar(5) not null comment 'type of ir stats, e.g. zv' default '',
	ir_class varchar(5) not null comment 'class for ir stats' default '',
	ir_class_id int null comment 'class id for ir stats',
	tp int not null,
	tn int not null,
	fp int not null,
	fn int not null,
	ppv double not null default 0,
	npv double not null default 0,
	sens double not null default 0,
	spec double not null default 0,
	f1 double not null default 0,
	unique key NK_classifier_eval_ircls (classifier_eval_id, ir_type, ir_class),
	key IX_classifier_eval_id (classifier_eval_id)
) engine=myisam comment 'ir statistics of a classifier on a dataset';

create table classifier_instance_eval (
	classifier_instance_eval_id int not null auto_increment primary key,
	classifier_eval_id int not null comment 'fk classifier_eval',
	instance_id bigint not null,
	pred_class_id int not null,
	target_class_id int null,
	unique key nk_result (classifier_eval_id, instance_id)
) engine=myisam comment 'instance classification result';

create table classifier_instance_eval_prob (
	classifier_eval_result_prob_id int not null auto_increment primary key,
	classifier_instance_eval_id int comment 'fk classifier_instance_eval',
	class_id int not null,
	probability double not null,
	unique key nk_result_prob (classifier_instance_eval_id, class_id)
) engine=myisam comment 'probability of belonging to respective class';


create table cv_fold (
  cv_fold_id int auto_increment not null primary key,
  corpus_name varchar(50) not null comment 'corpus name',
  split_name varchar(50) not null default '' comment 'split/subset name',
  label varchar(50) not null default '' ,
  run int not null default 0,
  fold int not null default 0,
  unique index nk_cv_fold (corpus_name, split_name, label, run, fold)
)engine=myisam ;

create table cv_fold_instance (
  cv_fold_instance_id int auto_increment not null primary key,
  cv_fold_id int not null,
  instance_id bigint not null,
  train bit not null default 0,
  unique index nk_cv_fold_instance (cv_fold_id, instance_id, train)
) engine=myisam ;

create table cv_best_svm (
  corpus_name varchar(50) NOT NULL,
  label varchar(50) NOT NULL,
  experiment varchar(50) NOT NULL DEFAULT '',
  f1 double DEFAULT NULL,
  kernel int DEFAULT NULL,
  cost double DEFAULT NULL,
  weight varchar(50) DEFAULT NULL,
  param1 double DEFAULT NULL,
  param2 varchar(50) DEFAULT NULL,
  PRIMARY KEY (corpus_name,label,experiment)
) ENGINE=MyISAM comment 'best svm params based on cv';

create table feature_eval (
  feature_eval_id int auto_increment not null primary key,
  corpus_name varchar(50) not null comment 'corpus name',
  featureset_name varchar(50) not null default '' comment 'feature set name',
  label varchar(50) not null default ''  comment 'label wrt features evaluated',
  cv_fold_id int not null default 0 comment 'fold wrt features evaluated',
  param1 double not null default 0 comment 'meta-parameter for feature evaluation',
  param2 varchar(50) not null default '' comment 'meta-parameter for feature evaluation',
  type varchar(50) not null comment 'metric used to evaluate features',
  unique index nk_feature_eval(corpus_name, featureset_name, label, cv_fold_id, param1, param2, type),
  index ix_feature_eval(corpus_name, cv_fold_id, type)
) engine=myisam comment 'evaluation of a set of features in a corpus';

create table feature_rank (
  feature_rank_id int auto_increment not null primary key,
  feature_eval_id int not null comment 'fk feature_eval',
  feature_name varchar(50) not null comment 'name of feature',
  evaluation double not null default 0 comment 'measurement of feature worth',
  rank int not null default 0 comment 'rank among all features',
  unique index nk_feature_name(feature_eval_id, feature_name),
  index ix_feature_rank(feature_eval_id, rank),
  index ix_feature_evaluation(feature_eval_id, evaluation),
  index fk_feature_eval(feature_eval_id)
) engine=myisam comment 'evaluation of a feature in a corpus';

CREATE TABLE feature_parchd (
  feature_parchd_id int(11) NOT NULL AUTO_INCREMENT,
  par_feature_rank_id int(11) NOT NULL COMMENT 'fk feature_rank propagated',
  chd_feature_rank_id int(11) NOT NULL COMMENT 'fk feature_rank imputed',
  PRIMARY KEY (feature_parchd_id),
  UNIQUE KEY NK_feature_parent (par_feature_rank_id,chd_feature_rank_id)
) ENGINE=MyISAM COMMENT='link between propagated parent and raw child feature rank';

CREATE TABLE tfidf_doclength (
  tfidf_doclength_id int(11) NOT NULL AUTO_INCREMENT,
  feature_eval_id int(11) NOT NULL COMMENT 'fk feature_eval',
  instance_id bigint(20) NOT NULL,
  length int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (tfidf_doclength_id),
  UNIQUE KEY nk_instance_id (feature_eval_id,instance_id)
) ENGINE=MyISAM COMMENT='doc length for calculating tf-idf'
;

create table hotspot (
  hotspot_id int auto_increment not null primary key,
  instance_id int not null comment 'fk cv_fold_instance',
  anno_base_id int not null comment 'fk anno_base_id',
  feature_rank_id int not null comment 'fk feature_rank',
  unique index NK_hotspot (instance_id, anno_base_id, feature_rank_id)
) engine=myisam ;
ALTER TABLE `hotspot` ADD INDEX `ix_instance_id`(`instance_id`),
 ADD INDEX `ix_anno_base_id`(`anno_base_id`),
 ADD INDEX `ix_feature_rank_id`(`feature_rank_id`);

create table hotspot_instance (
    hotspot_instance_id int auto_increment primary key,
    corpus_name varchar(50) not null,
    experiment varchar(50) not null default '',
    label varchar(50) not null default '',
    instance_id int not null,
    max_evaluation double not null default 0,
    min_rank int not null default 0,
    unique index NK_hotspot_instance (corpus_name, experiment, label, instance_id)
) engine=myisam comment 'hotspot features for an instance';

create table hotspot_sentence (
    hotspot_sentence_id int auto_increment not null primary key,
    hotspot_instance_id int not null comment 'fk hotspot_instance',
    anno_base_id int not null comment 'fk anno_sentence',
    evaluation double not null default 0 comment 'max eval from hotspot',
    rank int not null default 0 comment 'min rank from hotspot',
    unique index NK_hotspot_sentence (hotspot_instance_id, anno_base_id),
    index FK_hotspot_instance_id (hotspot_instance_id),
	index FK_anno_base_id (anno_base_id),
    INDEX IX_evaluation (hotspot_instance_id, evaluation),
    INDEX IX_rank (hotspot_instance_id, rank)
) engine = myisam comment 'sentences that contain hotspots at specified threshold';

create table corpus_doc (
	corpus_name varchar(50) not null comment 'corpus name',
	instance_id bigint not null comment 'doc id',
	doc_text longtext,
	doc_group varchar(50) DEFAULT NULL COMMENT 'train/test',
	primary key (corpus_name, instance_id),
	index IX_doc_group (corpus_name, doc_group)
)  engine = myisam comment 'documents';

create table corpus_label (
	corpus_name varchar(50) not null comment 'corpus name',
	instance_id bigint not null comment 'doc id',
	label varchar(20) not null default '',
	class varchar(5) not null default '',
	primary key (corpus_name, instance_id, label),
	index FK_corpus_doc (corpus_name, instance_id)
) engine = myisam comment 'document labels'; 

create view v_corpus_group_class
as
select distinct d.corpus_name, l.label, doc_group, class
from corpus_doc d
inner join corpus_label l 
    on d.corpus_name = l.corpus_name 
    and d.instance_id = l.instance_id
;
