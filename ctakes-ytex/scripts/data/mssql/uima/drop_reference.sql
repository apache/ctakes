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

-- drop 'reference' data
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[hibernate_sequences]') AND type in (N'U'))
	drop table $(db_schema).hibernate_sequences
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[anno_base_sequence]') AND type in (N'U'))
	drop table $(db_schema).anno_base_sequence
;




IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[ref_uima_type]') AND type in (N'U'))
	drop table $(db_schema).ref_uima_type
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[ref_named_entity_regex]') AND type in (N'U'))
	drop table $(db_schema).ref_named_entity_regex
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[ref_segment_regex]') AND type in (N'U'))
	drop table $(db_schema).ref_segment_regex
;

IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'$(db_schema).[ref_stopword]') AND type in (N'U'))
	drop table $(db_schema).ref_stopword
;
