@rem export snomed, rxnorm subset using this:
rmdir /s /q E:\projects\ytex-umls\mssql
mkdir E:\projects\ytex-umls\mssql
cd /d E:\projects\ytex-umls\mssql
bcp "select * from umls..MRCONSO where SAB in ('SNOMEDCT', 'SNOMEDCT_US', 'RXNORM', 'SRC') and LAT = 'ENG'" queryout MRCONSO.bcp -S localhost -T -n
bcp "select * from umls..MRSTY where CUI in (select distinct CUI from umls..MRCONSO where SAB in ('SNOMEDCT', 'SNOMEDCT_US', 'RXNORM', 'SRC'))" queryout MRSTY.bcp -S localhost -T -n
bcp "select * from YTEX_TEST.dbo.umls_aui_fword where aui in (select distinct AUI from umls..MRCONSO where SAB in ('SNOMEDCT', 'SNOMEDCT_US', 'RXNORM', 'SRC'))" queryout umls_aui_fword.bcp -S localhost -T -n

@rem export examples using this:
cd E:\projects\ytex\data\mssql\umls
bcp "select * from umls..MRCONSO where CUI in (select distinct code from YTEX_TEST.dbo.anno_ontology_concept) and SAB in ('SNOMEDCT', 'SNOMEDCT_US', 'RXNORM', 'SRC') and LAT = 'ENG'" queryout MRCONSO.bcp -S localhost -T -n
bcp "select * from umls..MRSTY where CUI in (select distinct code from YTEX_TEST.dbo.anno_ontology_concept)" queryout MRSTY.bcp -S localhost -T -n
bcp "select * from YTEX_TEST.dbo.umls_aui_fword where aui in (select AUI from umls..MRCONSO m inner join (select distinct code from YTEX_TEST.dbo.anno_ontology_concept) c on m.cui = c.code where SAB in ('SNOMEDCT', 'SNOMEDCT_US'))" queryout umls_aui_fword.bcp -S localhost -T -n
