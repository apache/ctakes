# get all the umls_aui_fword data
mysql --skip-column-names --user=ytex --password=ytex --database=ytex -e "select * from umls_aui_fword" > umls_aui_fword.txt


# get the demo data
mysql --skip-column-names --user=ytex --password=ytex --database=ytex -e "select concat(CUI,'|',TUI,'|',STN,'|',STY,'|',ATUI,'|',coalesce(CVF,'')) from sctrxnorm2011ab.MRSTY mst inner join (select distinct code from anno_ontology_concept) c on mst.cui = c.code" > E:\projects\ytex\data\umls\MRSTY.RRF 
mysql --skip-column-names --user=ytex --password=ytex --database=ytex -e "select concat(CUI,'|',LAT,'|',TS,'|',LUI,'|',STT,'|',SUI,'|',ISPREF,'|',AUI,'|',coalesce(SAUI,''),'|',coalesce(SCUI,''),'|',coalesce(SDUI,''),'|',SAB,'|',TTY,'|',mrc.CODE,'|',STR,'|',SRL,'|',SUPPRESS,'|',coalesce(CVF,'')) from sctrxnorm2011ab.MRCONSO mrc inner join ( select distinct code from anno_ontology_concept ) c on mrc.cui = c.code and lat = 'ENG'" > E:\projects\ytex\data\umls\MRCONSO.RRF 
mysql --skip-column-names --user=ytex --password=ytex --database=ytex -e "select fw.* from umls_aui_fword fw inner join sctrxnorm2011ab.mrconso mrc on fw.aui = mrc.aui inner join (select distinct code from anno_ontology_concept) c on c.code = mrc.cui" > E:\projects\ytex\data\umls\umls_aui_fword.txt 

demo contains only umls concepts for fracture demo

mysql demo data:
select
CUI,
TUI,
STN,
STY,
ATUI,
coalesce(CVF,'')
into outfile 'E:/projects/ytex/data/mysql/umls/MRSTY.RRF'
fields terminated by '|' ESCAPED BY '' lines terminated by '\r\n'
from umls.MRSTY mst
inner join (select distinct code from anno_ontology_concept) c on mst.cui = c.code
;


select CUI,
LAT,
TS,
LUI,
STT,
SUI,
ISPREF,
AUI,
coalesce(SAUI,''),
coalesce(SCUI,''),
coalesce(SDUI,''),
SAB,
TTY,
mrc.CODE,
STR,
SRL,
SUPPRESS,
coalesce(CVF,'')
into outfile 'E:/projects/ytex/data/mysql/umls/MRCONSO.RRF'
fields terminated by '|' ESCAPED BY '' lines terminated by '\r\n'
from umls.MRCONSO mrc
inner join
(
select distinct code from anno_ontology_concept
) c on mrc.cui = c.code
where mrc.sab in ( 'SNOMEDCT', 'SNOMEDCT_US', 'RXNORM', 'SRC')
and lat = 'ENG'
;

select fw.*
into outfile 'E:/projects/ytex/data/mysql/umls/umls_aui_fword.txt'
from umls_aui_fword fw
inner join umls.mrconso mrc on fw.aui = mrc.aui and mrc.sab in ( 'SNOMEDCT', 'SNOMEDCT_US', 'RXNORM', 'SRC')
inner join
(
select distinct code from anno_ontology_concept
) c on c.code = mrc.cui;


mysql real data:
select CUI,
LAT,
TS,
LUI,
STT,
SUI,
ISPREF,
AUI,
coalesce(SAUI,''),
coalesce(SCUI,''),
coalesce(SDUI,''),
SAB,
TTY,
mrc.CODE,
STR,
SRL,
SUPPRESS,
coalesce(CVF,'')
into outfile 'E:/projects/ytex-umls/mysql/MRCONSO.RRF'
fields terminated by '|' ESCAPED BY '' lines terminated by '\r\n'
from umls.MRCONSO mrc
where SAB in ('SNOMEDCT', 'SNOMEDCT_US', 'RXNORM', 'SRC')
and LAT = 'ENG'
;


select
sty.CUI,
TUI,
STN,
STY,
ATUI,
coalesce(CVF,'')
into outfile 'E:/projects/ytex-umls/mysql/MRSTY.RRF'
fields terminated by '|' ESCAPED BY '' lines terminated by '\r\n'
from umls.MRSTY sty
inner join
	(
	select distinct cui
	from umls.MRCONSO
	where SAB in ('SNOMEDCT', 'SNOMEDCT_US', 'RXNORM', 'SRC')
	) c on sty.cui = c.cui
;

select fw.*
into outfile 'E:/projects/ytex-umls/mysql/umls_aui_fword.txt'
from umls_aui_fword fw
inner join umls.mrconso mrc on fw.aui = mrc.aui and mrc.sab in ('SNOMEDCT', 'SNOMEDCT_US', 'RXNORM', 'SRC')
;

