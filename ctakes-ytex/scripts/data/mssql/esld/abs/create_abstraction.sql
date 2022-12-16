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

CREATE TABLE [ESLD].[abs_studyid](
	[studyid] [int] NOT NULL,
	[abstractor] [nvarchar](70) NULL,
	[no_abdominal_ultrasound] [bit] NOT NULL  default 0,
	[no_abdominal_ct] [bit] NOT NULL default 0,
	[no_abdominal_mri] [bit] NOT NULL default 0,
	[notes] [nvarchar](max) NULL,
	[lab_afp_ngml] [float] NULL,
	[lab_afp_date] [datetime] NULL,
	[lab_afp_not_present] [bit] NOT NULL  default 0,
	[ammonia_not_present] [bit] NOT NULL  default 0,
	[ammonia_mmolL] [float] NULL,
	[ammonia_date] [datetime] NULL,
	[paracentesis_not_present] [bit] NOT NULL  default 0,
	[egd_report_not_present] [bit] NOT NULL default 0,
	[hepatic_enc_not_present] [bit] NOT NULL  default 0,
	[hepatic_enc_date] [datetime] NULL,
	[asterixis_not_present] [bit] NOT NULL  default 0,
	[asterixis_date] [datetime] NULL,
	[variceal_bleed_not_present] [bit] NOT NULL  default 0,
	[variceal_bleed_date] [datetime] NULL,
	[liver_biopsy_not_present] [bit] NOT NULL default 0,
	CONSTRAINT PK_abs_studyid PRIMARY KEY  
	(
		studyid ASC
	)	
)
;

CREATE TABLE [ESLD].[abs_radiology](
	[ID] [int] IDENTITY(1,1) NOT NULL,
	[studyid] [int] NOT NULL,
	[procedure_date] [datetime] NULL,
	[procedure_type] [nvarchar](20) NOT NULL,
	[ascites_reported] [bit] not NULL default 0,
	[ascites_amount] [nvarchar](20) NULL,
	[liver_masses_reported] [bit] not NULL default 0,
	[liver_masses_multiple] [bit] not NULL default 0,
	[liver_masses_count] [smallint] NULL,
	[liver_mass_dim1] [float] NULL,
	[liver_mass_dim2] [float] NULL,
	[liver_mass_dim3] [float] NULL,
	[liver_mass_arterial_enhancing] [bit] not NULL default 0,
	[varices_reported] [bit] not NULL default 0,
	[uid] [int] NULL,
	CONSTRAINT PK_abs_radiology PRIMARY KEY  
	(
		ID ASC
	),
	FOREIGN KEY (studyid) references esld.abs_studyid(studyid)
) 
;

CREATE TABLE [ESLD].[abs_biopsy](
	[ID] [int] IDENTITY(1,1) NOT NULL,
	[fibrosis_stage] [nvarchar](50) NULL,
	[inflammation_grade] [nvarchar](50) NULL,
	[cirrhosis_reported] [bit] NOT NULL default 0,
	[hepatocellular_carcinoma] [bit] NOT NULL default 0,
	[liver_biopsy_date] [datetime] NULL,
	[studyid] [int] not null,
	CONSTRAINT PK_abs_biopsy PRIMARY KEY  
	(
		ID ASC
	),
	FOREIGN KEY (studyid) references esld.abs_studyid(studyid)
)
;

CREATE TABLE [ESLD].[abs_endoscopy](
	[ID] [int] IDENTITY(1,1) NOT NULL,
	[studyid] [int] not null,
	[endoscopy_date] [datetime] NULL,
	[performed_bc_bleeding] [bit] NOT NULL default 0,
	[gastritis_reported] [bit] NOT NULL default 0,
	[peptic_ulcer_reported] [bit] NOT NULL default 0,
	[portal_gastropathy_reported] [bit] NOT NULL default 0,
	[varices_reported] [bit] NOT NULL default 0,
	[varices_active] [bit] NOT NULL default 0,
	[varices_cherry] [bit] NOT NULL default 0,
	[varices_pale] [bit] NOT NULL default 0,
	[varices_banded] [bit] NOT NULL default 0,
	[varices_esophogeal] [bit] NOT NULL default 0,
	[varices_gastric] [bit] NOT NULL default 0,
	CONSTRAINT PK_abs_endoscopy PRIMARY KEY  
	(
		ID ASC
	),
	FOREIGN KEY (studyid) references esld.abs_studyid(studyid)
);


CREATE TABLE [ESLD].[abs_paracentesis](
	[ID] [int] IDENTITY(1,1) NOT NULL,
	[studyid] [int] not NULL,
	[total_white_cells] [float] NULL,
	[percent_neutrophils] [float] NULL,
	[culture] [nvarchar](255) NULL,
	[date_recorded] [datetime] NULL,
	CONSTRAINT PK_abs_paracentesis PRIMARY KEY  
	(
		ID ASC
	),
	FOREIGN KEY (studyid) references esld.abs_studyid(studyid)
) 
;

CREATE INDEX IX_abs_endoscopy_studyid ON esld.abs_endoscopy (studyid)
;
CREATE INDEX IX_abs_biopsy_studyid ON esld.abs_biopsy (studyid)
;
CREATE INDEX IX_abs_radiology_studyid ON esld.abs_radiology (studyid)
;
CREATE INDEX IX_abs_paracentesis_studyid ON esld.abs_paracentesis (studyid)
;
