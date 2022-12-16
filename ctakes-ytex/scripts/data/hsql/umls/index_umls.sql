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

CREATE INDEX X_MRCONSO_CUI ON MRCONSO(CUI);

CREATE INDEX X_MRCONSO_SUI ON MRCONSO(SUI);

CREATE INDEX X_MRCONSO_LUI ON MRCONSO(LUI);

CREATE INDEX X_MRCONSO_CODE ON MRCONSO(CODE);

CREATE INDEX X_MRCONSO_SAB_TTY ON MRCONSO(SAB,TTY);

CREATE INDEX X_MRCONSO_SCUI ON MRCONSO(SCUI);

CREATE INDEX X_MRCONSO_SDUI ON MRCONSO(SDUI);

CREATE INDEX X_MRCONSO_STR ON MRCONSO(STR);

CREATE INDEX X_MRSTY_CUI ON MRSTY(CUI);

CREATE INDEX X_MRSTY_STY ON MRSTY(STY);

