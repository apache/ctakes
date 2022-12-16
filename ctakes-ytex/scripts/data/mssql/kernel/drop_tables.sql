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

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[hotspot_sentence]') AND type in (N'U'))
	drop TABLE  $(db_schema).hotspot_sentence
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[hotspot_instance]') AND type in (N'U'))
	DROP TABLE $(db_schema).[hotspot_instance]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[hotspot]') AND type in (N'U'))
	drop TABLE  $(db_schema).hotspot
;


IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[feature_parchd]') AND type in (N'U'))
	drop TABLE  $(db_schema).feature_parchd
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[feature_rank]') AND type in (N'U'))
	drop TABLE  $(db_schema).feature_rank
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[tfidf_doclength]') AND type in (N'U'))
	drop TABLE  $(db_schema).tfidf_doclength
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[feature_eval]') AND type in (N'U'))
	drop TABLE  $(db_schema).feature_eval
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[classifier_eval_ir]') AND type in (N'U'))
DROP TABLE $(db_schema).[classifier_eval_ir]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[classifier_eval_svm]') AND type in (N'U'))
DROP TABLE $(db_schema).[classifier_eval_svm]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[classifier_eval_semil]') AND type in (N'U'))
DROP TABLE $(db_schema).[classifier_eval_semil]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[classifier_instance_eval_prob]') AND type in (N'U'))
DROP TABLE $(db_schema).[classifier_instance_eval_prob]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[classifier_instance_eval]') AND type in (N'U'))
DROP TABLE $(db_schema).[classifier_instance_eval]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[classifier_eval]') AND type in (N'U'))
DROP TABLE $(db_schema).[classifier_eval]
;


IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[cv_fold_instance]') AND type in (N'U'))
DROP TABLE $(db_schema).[cv_fold_instance]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[cv_fold]') AND type in (N'U'))
DROP TABLE $(db_schema).[cv_fold]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[cv_best_svm]') AND type in (N'U'))
DROP TABLE $(db_schema).[cv_best_svm]
;


IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[kernel_eval_instance]') AND type in (N'U'))
DROP TABLE $(db_schema).[kernel_eval_instance]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[kernel_eval]') AND type in (N'U'))
DROP TABLE $(db_schema).[kernel_eval]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[v_corpus_group_class]') AND type in (N'V'))
drop VIEW $(db_schema).[v_corpus_group_class]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[corpus_label]') AND type in (N'U'))
DROP TABLE $(db_schema).[corpus_label]
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[corpus_doc]') AND type in (N'U'))
DROP TABLE $(db_schema).[corpus_doc]
;




