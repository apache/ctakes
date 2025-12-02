# Fast Dictionary Lookup

The fast dictionary lookup annotator identifies terms in text and normalizes them to codes in an ontology:
UMLS CUI, Snomed-CT, RxNorm, etc.
The fast dictionary lookup module comes with multiple possible pre-packaged configurations and is also customizable and extendable.  

## Process Overview
The Fast Dictionary Lookup module has six basic processes performed by three components, as well as a parser that can configure the actual Dictionaries.

- A Parse Dictionary Descriptor file
- B Create Dictionaries and Concept Factories

1.	Get _Lookup Windows_ from **CAS**
2.	For each _Lookup window_, get candidate _Lookup Tokens_
3.	For each _Lookup Token_, get matches in Dictionary Index
4.	For each _Token_ match, check _Lookup Window_ for _Full Text_ match
5.	For each _Full Text_ match, create _Concepts_
6.	Store appropriate _Concepts_ in **CAS** as **Annotations**

![Structure Diagram](https://raw.githubusercontent.com/wiki/apache/ctakes/images/EnumeratedFlow2.png)

## Configuration
There are options available to change the type of term matching used as well as the persistence of terms.
Changes in configuration are made in two places:  

1.	The main descriptor ...```-fast/desc/analysis_engine/UmlsLookupAnnotator.xml```
2.	The resource (dictionary) configuration file ```resources/.../dictionary/lookup/fast/sno_rx_16ab.xml``` 
(The file name might be different if you created your own custom dictionary)

## Text Exact Match
Because the UMLS dictionary contains rows with different combinations of lexical elements per term, 
using a direct string match of text in note to text of term is a valid candidate for term matching.  
This is different from the complex mechanism in the current (first word) lookup, and makes for simpler code and greater accuracy. 
This precise specification (and improved lookup speed) enables the use of an entire sentence as a lookup window rather than just a noun phrase.
Usage of Sentence as a lookup window allows all possible tokens to be used for not only lookup keys, but also for term matching.  
For proper accuracy, custom dictionaries should also contain multiple entries for variations of term syntax. 
Note that term matching is attempted using the actual text in the note and also per-token cTAKES-generated lexical variants 
of the text in the note.  
This is the behavior of the ```DefaultJCasTermAnnotator``` class, which is the one used in the ```UmlsLookupAnnotator.xml``` descriptor.

## Text Overlap Match
To better approximate the original lookup annotator, one lookup method finds overlapping terms in addition to exact matching terms.
This allows matches on discontiguous spans.
For instance, for the text “blood, urine test” the exact match will find only one procedure: “urine test”. 
The overlap match will find both “urine test” and “blood     test”. 
This is the behavior of the OverlapJCasTermAnnotator class, which is the one used in the ```UmlsOverlapLookupAnnotator.xml``` descriptor.

## All Terms Persistence
All terms discovered by the matchers can be stored in the CAS by a consumer, regardless of any property of the term. 
This means that for the text “lung cancer” the specific disease term “lung cancer” and broader term “cancer”. 
This can be useful for future searches on general concepts, 
e.g. searching via the CUI for “cancer” and getting all instances of “cancer” found in texts “lung cancer”, “skin cancer”, “stomach cancer”, etc. 
This is the behavior of the ```DefaultTermConsumer``` class.

## Most Precise Terms Persistence
Matched terms can be stored only by the longest overlapping span discovered for a semantic group.
This keeps, for instance, the disease “lung cancer” but not “cancer”. 
Using semantic groups means that both the disease “lung cancer” and the anatomical site “lung” are persisted even though the spans overlap. 
When using the overlap matching method, any discontiguous spans are accounted for. 
So, for “blood, urine test” both the discontiguous spanned term “blood   test” and the contiguous spanned term “urine test” are valid. 
To persist only the most precise terms, edit the xml configuration file for your dictionary (default is ```sno_rx_16ab.xml```),
specifically within the section rareWordConsumer change the selected implementation. 
By default it is ```DefaultTermConsumer```, but you will want to use the commented-out ```PrecisionTermConsumer```.

## Dictionary Stores
The default configuration uses a dictionary that contains a subset of the UMLS in an hsql database. 
Custom dictionaries can be added using another hsql database, or using a bar-separated value (BSV) (a.k.a. pipe-separated) flat file. 
If you use a BSV file you do not need to tokenize the terms. 
Tokenization will be done automatically at runtime.

## Lookup Window
By default the new lookup uses Sentence as the lookup window. 
The primary reasons for this are:

1. Not all terms are within Noun Phrases
2. Some Noun Phrases overlapped, causing repeated lookups (in my 3.0 candidate trials)
3. Not all cTakes Noun Phrases are accurate. 

Because the lookup is fast, using a full Sentence for lookup doesn't seem to hurt much. 
However, you can always switch it back to see if precision is increased enough to warrant the decrease in recall. 
This is changed in ```UmlsLookupAnnotator.xml```.
