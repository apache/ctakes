<details>
<summary>The dictionary lookup annotator finds the entries from one or more dictionaries that match the
document text in some way.</summary>summary>
Within this annotator, these matches are called lookup hits.  
The dictionary lookup annotator is very customizable.  
It can look for matches where the words in the dictionary entries appear in the same order as
the words in the document text, or it can look for permutations of the words from the dictionary.  
It can look just for exact matches of the words, or it can also look for matches to the 
canonical forms of the words.  
Searches for a lookup hit are limited to within windows, where the window type is defined in
the LookupDescriptorFile. A window can be the words that fall within the same Sentence, the same
Chunk, the same LookupWindowAnnotation or any other annotation.  See the clinical documents 
pipeline project for an example of an analysis engine (LookupWindowAnnotator.xml) that
that creates LookupWindowAnnotations.  
Note: Dictionary entries need to have been tokenized the way the pipeline tokenizes the document text.  
The behavior of the dictionary lookup annotator is controlled by the parameters and resources 
defined in the analysis engine descriptor, and by the contents of the resource called the 
LookupDescriptorFile.  
For example, if the analysis engine descriptor ```DictionaryLookup.xml``` contains 
a resource named LookupDescriptorFile with value ```lookup/LookupDesc.xml```, then the parameter
settings and resources named within ```DictionaryLookup.xml```, together with the values within
"lookup/LookupDesc.xml" will control the actions of the dictionary lookup annotator.  
The lookupInitializer and lookupConsumer classes are specified within the LookupDescriptorFile.  
The algorithm used for looking up the terms is defined by the lookupInitializer, which
creates the lookup hits.  
The lookupConsumer adds annotations to the CAS for some or all of the lookup hits.  
An example adding only some of the lookup hits to the CAS is if you have a dictionary of RxNorm 
terms with their RxNorm codes, and a dictionary of terms from the OrangeBook, and want to create 
annotations for those terms that are in the OrangeBook that also have an RxNorm code.  
This can be done using class ```org.apache.ctakes.dictionary.lookup.ae.FirstTokenPermLookupInitializerImpl```
as the lookupInitializer, and using class OrangeBookFilterConsumerImpl as the lookupConsumer,
provided you have the RxNorm dictionary, and you configure the LookupDescriptorFile resource
to use your RxNorm dictionary.  
Note: The analysis engine descriptors for this annotator use elements of type 
configurableDataResourceSpecifier.
These cannot be modified from the Parameters or Resources tabs of the Component Descriptor 
Editor (at least not in UIMA 2.2). To view these values or edit them, use the Sources tab or 
open the descriptor with a Text Editor.  
Note: Dictionary entries need to have been tokenized the way the pipeline tokenizes the document text.  
For example, the lookup algorithm will not find a lookup hit if a dictionary entry is 
"ear, skin" but the document text contains the same text ("ear, skin") and the pipeline 
has tokenized that text as the 3 tokens "ear" "," "skin".  
To find a lookup hit for the 3 tokens, the dictionary entry should be tokenized,
with a space before the comma: "ear , skin"  
To determine the LookupDescriptorFile for an analysis engine, open the analysis engine descriptor 
(e.g. DictionaryLookupannotator.xml) and note the URL for the LookupDescriptorFile resource 
(e.g. lookup/LookupDesc.xml).  
A LookupDescriptorFile such as lookup/LookupDesc.xml, found in resources/, 
defines the dictionary(s) used, and the classes that interact with the 
dictionary(s). The implementation tag identifies the type of dictionary:
lucene index (luceneImpl), database (jdbcImpl), or delimited flatfile (csvImpl).
See class org.apache.ctakes.dictionary.lookup.ae.LookupParseUtilities.java for implementation details.  
To better understand the dictionary lookup annotator code you could start by reading the javadocs
for the classes DictionaryLookupAnnotator.java and FirstTokenPermutationImpl.java.   

This project includes two sample dictionaries.  
A sample database (a Lucene index) containing a few drug names is included
for running the examples.  
A sample database (using 2 Lucene indexes) containing a few anatomical sites, procedures, 
and disorders/diseases is included for running the examples.  
The programs used to create these Lucene indexes are  
scripts/java/edu/mayo/bmi/dictionarytools/CreateLuceneIndexForExampleDrugs.java
scripts/java/edu/mayo/bmi/dictionarytools/CreateLuceneIndexForSnomedLikeSample.java

#### Creating your own dictionaries

To create a more complete dictionary of drug concepts, you could download a copy of the UMLS 
Metathesaurus and build upon the program mentioned above to create a lucene index of the 
complete RxNorm or another source of drug concepts.  
Or you could use a different program in that same package that reads from a pipe-delimited file:
scripts/java/edu/mayo/bmi/dictionarytools/CreateLuceneIndexFromDelimitedFile  
The pipe-delimited file should contain lines in the following format.   
CUI|drug name aka description|terminology aka source|codeInThatSource|PreferredIndicator|TUI  
Where PreferredIndicator = P if the name is the preferred name for the drug.  
For example, if you want include terms from semantic type "Biomedical or Dental Material" (TUI T122), 
one line in the file you create should be:  
C1154185|Topical Spray|RXNORM|346165|P|T122  
The CreateLuceneIndexFromDelimitedFile class could then be used to create a lucene index from the 
data in the file.

To create a more complete dictionary of anatomical sites, procedures, signs/symptoms,
and/or diseases/disorders, you could download a copy of the UMLS Metathesaurus and build 
upon (add code to) the CreateLuceneIndexForSnomedLikeSample class to create 2 lucene 
indexes - one Lucene index for the concepts and their CUIs, and one that maps the codes 
from the source(s) to the CUIs. 

Alternatively you could create and populate a database with the following two tables  
  umls_ms_2005 (or whatever name you specify within LookupDesc_Db.xml)
      with columns "fword"  "cui"  "tui"  "text"
  umls_snomed_map
      with columns "cui"  "code"

#### Configuring the annotator to use your dictionary

These steps are not necessary in order to run the pipeline with the very small sample dictionaries
that are included with this project.  
If you created your own dictionary(s) as outlined above, here is how you could configure this
annotator to use your dictionary(s).  
If you created a lucene index directory called drug_index, within descriptor DictionaryLookupAnnotator.xml  
you could update the value of the IndexDirectory for external resource RxnormIndex to reference the location 
of your drug_index directory.  Recall you need to use a text editor or you need to be on tab Source 
to edit this portion of that descriptor since it is within a configurableDataResourceSpecifier.   
Alternatively, you could simply replace the contents of directory
dictionary lookup/resources/lookup/drug_index
with the contents of the lucene index directory you created.  
If you created 2 lucene index directories using CreateLuceneIndexForSnomedLikeSample.java,
you could simply replace the contents of the two directories
dictionary lookup/resources/lookup/snomed-like_sample
dictionary lookup/resources/lookup/snomed-like_codes_sample
with the contents of the lucene index directories you created.

Alternatively, if you created database tables umls_snomed_map and umls_ms_2005 as 
outlined above, you could do the following steps 

1) Replace the use of DictionaryLookupAnnotator.xml with DictionaryLookupAnnotatorDB.xml
   in your pipeline (in your aggregate flow.  e.g. in AggregatePlaintextProcessor.xml)
   The class UmlsToSnomedDbConsumerImpl.java that is used in this case is included with 
   this distribution.

2) Update some values within DictionaryLookupAnnotatorDB.xml for your environment
    2a) Username
    2b) Password
    2c) DriverClassName
    2d) URL
</details>
