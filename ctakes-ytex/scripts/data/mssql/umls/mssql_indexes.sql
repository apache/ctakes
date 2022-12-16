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

/* adapted from oracle/mysql umls load scripts */

CREATE INDEX X_MRCOC_CUI1 ON $(db_schema).MRCOC(CUI1);

CREATE INDEX X_MRCOC_AUI1 ON $(db_schema).MRCOC(AUI1);

CREATE INDEX X_MRCOC_CUI2 ON $(db_schema).MRCOC(CUI2);

CREATE INDEX X_MRCOC_AUI2 ON $(db_schema).MRCOC(AUI2);

CREATE INDEX X_MRCOC_SAB ON $(db_schema).MRCOC(SAB);

CREATE INDEX X_MRCONSO_CUI ON $(db_schema).MRCONSO(CUI);

ALTER TABLE $(db_schema).MRCONSO ADD CONSTRAINT X_MRCONSO_PK  PRIMARY KEY (AUI);

CREATE INDEX X_MRCONSO_SUI ON $(db_schema).MRCONSO(SUI);

CREATE INDEX X_MRCONSO_LUI ON $(db_schema).MRCONSO(LUI);

CREATE INDEX X_MRCONSO_CODE ON $(db_schema).MRCONSO(CODE);

CREATE INDEX X_MRCONSO_SAB_TTY ON $(db_schema).MRCONSO(SAB,TTY);

CREATE INDEX X_MRCONSO_SCUI ON $(db_schema).MRCONSO(SCUI);

CREATE INDEX X_MRCONSO_SDUI ON $(db_schema).MRCONSO(SDUI);

-- CREATE INDEX X_MRCONSO_STR ON $(db_schema).MRCONSO(STR);

CREATE INDEX X_MRCXT_CUI ON $(db_schema).MRCXT(CUI);

CREATE INDEX X_MRCXT_AUI ON $(db_schema).MRCXT(AUI);

CREATE INDEX X_MRCXT_SAB ON $(db_schema).MRCXT(SAB);

CREATE INDEX X_MRDEF_CUI ON $(db_schema).MRDEF(CUI);

CREATE INDEX X_MRDEF_AUI ON $(db_schema).MRDEF(AUI);

ALTER TABLE $(db_schema).MRDEF ADD CONSTRAINT X_MRDEF_PK  PRIMARY KEY (ATUI);

CREATE INDEX X_MRDEF_SAB ON $(db_schema).MRDEF(SAB);

CREATE INDEX X_MRHIER_CUI ON $(db_schema).MRHIER(CUI);

CREATE INDEX X_MRHIER_AUI ON $(db_schema).MRHIER(AUI);

CREATE INDEX X_MRHIER_SAB ON $(db_schema).MRHIER(SAB);

-- field to big to index
-- CREATE INDEX X_MRHIER_PTR ON $(db_schema).MRHIER(PTR);

CREATE INDEX X_MRHIER_PAUI ON $(db_schema).MRHIER(PAUI);

CREATE INDEX X_MRHIST_CUI ON $(db_schema).MRHIST(CUI);

CREATE INDEX X_MRHIST_SOURCEUI ON $(db_schema).MRHIST(SOURCEUI);

CREATE INDEX X_MRHIST_SAB ON $(db_schema).MRHIST(SAB);

ALTER TABLE $(db_schema).MRRANK ADD CONSTRAINT X_MRRANK_PK  PRIMARY KEY (SAB,TTY);

CREATE INDEX X_MRREL_CUI1 ON $(db_schema).MRREL(CUI1);

CREATE INDEX X_MRREL_AUI1 ON $(db_schema).MRREL(AUI1);

CREATE INDEX X_MRREL_CUI2 ON $(db_schema).MRREL(CUI2);

CREATE INDEX X_MRREL_AUI2 ON $(db_schema).MRREL(AUI2);

ALTER TABLE $(db_schema).MRREL ADD CONSTRAINT X_MRREL_PK  PRIMARY KEY (RUI);

CREATE INDEX X_MRREL_SAB ON $(db_schema).MRREL(SAB);

ALTER TABLE $(db_schema).MRSAB ADD CONSTRAINT X_MRSAB_PK  PRIMARY KEY (VSAB);

CREATE INDEX X_MRSAB_RSAB ON $(db_schema).MRSAB(RSAB);

CREATE INDEX X_MRSAT_CUI ON $(db_schema).MRSAT(CUI);

CREATE INDEX X_MRSAT_METAUI ON $(db_schema).MRSAT(METAUI);

ALTER TABLE $(db_schema).MRSAT ADD CONSTRAINT X_MRSAT_PK  PRIMARY KEY (ATUI);

CREATE INDEX X_MRSAT_SAB ON $(db_schema).MRSAT(SAB);

CREATE INDEX X_MRSAT_ATN ON $(db_schema).MRSAT(ATN);

CREATE INDEX X_MRSTY_CUI ON $(db_schema).MRSTY(CUI);

ALTER TABLE $(db_schema).MRSTY ADD CONSTRAINT X_MRSTY_PK  PRIMARY KEY (ATUI);

CREATE INDEX X_MRSTY_STY ON $(db_schema).MRSTY(STY);

-- field to large to index
-- CREATE INDEX X_MRXNS_ENG_NSTR ON $(db_schema).MRXNS_ENG(NSTR);

CREATE INDEX X_MRXNW_ENG_NWD ON $(db_schema).MRXNW_ENG(NWD);

CREATE INDEX X_MRXW_BAQ_WD ON $(db_schema).MRXW_BAQ(WD);

CREATE INDEX X_MRXW_CZE_WD ON $(db_schema).MRXW_CZE(WD);

CREATE INDEX X_MRXW_DAN_WD ON $(db_schema).MRXW_DAN(WD);

CREATE INDEX X_MRXW_DUT_WD ON $(db_schema).MRXW_DUT(WD);

CREATE INDEX X_MRXW_ENG_WD ON $(db_schema).MRXW_ENG(WD);

CREATE INDEX X_MRXW_FIN_WD ON $(db_schema).MRXW_FIN(WD);

CREATE INDEX X_MRXW_FRE_WD ON $(db_schema).MRXW_FRE(WD);

CREATE INDEX X_MRXW_GER_WD ON $(db_schema).MRXW_GER(WD);

CREATE INDEX X_MRXW_HEB_WD ON $(db_schema).MRXW_HEB(WD);

CREATE INDEX X_MRXW_HUN_WD ON $(db_schema).MRXW_HUN(WD);

CREATE INDEX X_MRXW_ITA_WD ON $(db_schema).MRXW_ITA(WD);

CREATE INDEX X_MRXW_JPN_WD ON $(db_schema).MRXW_JPN(WD);

CREATE INDEX X_MRXW_KOR_WD ON $(db_schema).MRXW_KOR(WD);

CREATE INDEX X_MRXW_LAV_WD ON $(db_schema).MRXW_LAV(WD);

CREATE INDEX X_MRXW_NOR_WD ON $(db_schema).MRXW_NOR(WD);

CREATE INDEX X_MRXW_POR_WD ON $(db_schema).MRXW_POR(WD);

CREATE INDEX X_MRXW_RUS_WD ON $(db_schema).MRXW_RUS(WD);

CREATE INDEX X_MRXW_SCR_WD ON $(db_schema).MRXW_SCR(WD);

CREATE INDEX X_MRXW_SPA_WD ON $(db_schema).MRXW_SPA(WD);

CREATE INDEX X_MRXW_SWE_WD ON $(db_schema).MRXW_SWE(WD);

CREATE INDEX X_AMBIGSUI_SUI ON $(db_schema).AMBIGSUI(SUI);

CREATE INDEX X_AMBIGLUI_LUI ON $(db_schema).AMBIGLUI(LUI);

CREATE INDEX X_MRAUI_CUI2 ON $(db_schema).MRAUI(CUI2);

CREATE INDEX X_MRCUI_CUI2 ON $(db_schema).MRCUI(CUI2);

CREATE INDEX X_MRMAP_MAPSETCUI ON $(db_schema).MRMAP(MAPSETCUI);
