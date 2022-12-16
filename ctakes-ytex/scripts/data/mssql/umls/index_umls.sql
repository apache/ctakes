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

CREATE INDEX X_MRCONSO_CUI ON $(db_schema).MRCONSO(CUI);

CREATE INDEX X_MRCONSO_SUI ON $(db_schema).MRCONSO(SUI);

CREATE INDEX X_MRCONSO_LUI ON $(db_schema).MRCONSO(LUI);

CREATE INDEX X_MRCONSO_CODE ON $(db_schema).MRCONSO(CODE);

CREATE INDEX X_MRCONSO_SAB_TTY ON $(db_schema).MRCONSO(SAB,TTY);

CREATE INDEX X_MRCONSO_SCUI ON $(db_schema).MRCONSO(SCUI);

CREATE INDEX X_MRCONSO_SDUI ON $(db_schema).MRCONSO(SDUI);

CREATE INDEX X_MRSTY_CUI ON $(db_schema).MRSTY(CUI);

CREATE INDEX X_MRSTY_STY ON $(db_schema).MRSTY(STY);
