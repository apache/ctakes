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

drop table if exists hotspot_sentence;
drop table if exists hotspot_instance;
drop table if exists hotspot;
drop table if exists feature_parchd;
drop table if exists feature_rank;
drop table if exists feature_eval;
drop table if exists tfidf_doclength; 
drop table if exists cv_fold;
drop table if exists cv_fold_instance;
drop table if exists cv_best_svm;
drop table if exists classifier_instance_eval_prob;
drop table if exists classifier_instance_eval;
drop table if exists classifier_eval_svm;
drop table if exists classifier_eval_semil;
drop table if exists classifier_eval_ir;
drop table if exists classifier_eval;
DROP TABLE IF EXISTS kernel_eval;
DROP TABLE IF EXISTS kernel_eval_instance;
drop table if exists corpus_label;
drop table if exists corpus_doc;
drop view if exists v_corpus_group_class;
