# Apache cTAKES™

## Introduction


The Apache™ clinical Text Analysis and Knowledge Extraction System (cTAKES™) focuses on extracting knowledge
from clinical text through Natural Language Processing (NLP) techniques.

cTAKES is engineered in a modular fashion and employs leading-edge rule-based and machine learning methods.

cTAKES has standard features for biomedical text processing software,
including the ability to extract concepts such as symptoms, procedures, diagnoses, medications and anatomy
with attributes and standard codes.

More powerful components can perform tasks as complex as identifying temporal events,
dates and times – resulting in placement of events in a patient timeline.

Components are trained on gold standards from the biomedical as well as the general domain.
This affords usability across different types of clinical narrative (e.g. radiology reports,
clinical notes, discharge summaries) in various institution formats as well as other types of
health-related narrative (e.g. twitter feeds), using multiple data standards (e.g. Health Level 7 (HL7),
Clinical Document Architecture (CDA), Fast Healthcare Interoperability Resources (FHIR), SNOMED-CT, RxNORM).

cTAKES is the NLP platform for many initiatives across the world covering a variety of research purposes
and large datasets.
Contributors include professionals at medical and commercial institutions, NLP and Machine Learning researchers,
Medical Doctors, and students of many disciplines and levels.
We encourage people from all backgrounds to get involved! (link)


<br>

## Supported Environments
1. **Java 1.8** is required to run cTAKES versions 5.x and older. Versions 6+ require java 17.  Run this command to check your Java version:
```
$ java -version
```
2. **Maven 3** is required to build cTAKES. Run this to command to check your Maven version:
```
$ mvn -version
```
3. A license for the [Unified Medical Language System (UMLS)](https://www.nlm.nih.gov/research/umls/index.html)
   is required to use the named entity recognition module (dictionary lookup) with the default dictionary.
4. **Python 3** is required to use cTAKES [Python Bridge to Java (PBJ)](https://github.com/apache/ctakes/wiki/pbj_intro). 
Run this to command to check your Python version:
```
$ python -V
```


<br/>


## Getting Started

### New Users

The easiest way for new users to get a jump start running cTAKES is to use the [Standard Pipeline Installation Facility](artifacts).
The Standard Pipeline Installation Facility is a tool that can install cTAKES configured to run the most popular cTAKES pre-built pipelines. 
You can then use the [Piper File Submitter](https://github.com/apache/ctakes/wiki/Piper+File+Submitter) GUI to submit jobs or submit them from the command line.

For access to all cTAKES capabilities, download a [zip]() or [tar.z]() file containing a fully-built installation of the most recent cTAKES release.
Then, after obtaining a UMLS license, use the [UMLS Package Fetcher](https://github.com/apache/ctakes/wiki/cTAKES+UMLS+Package+Fetcher) GUI to install a copy of the 
default dictionary for Named Entity Recognition (NER) using cTAKES Fast Dictionary Lookup.

### New Developers

__Notice:__ cTAKES 6.0.0-SNAPSHOT requires jdk 17 to build and run.

All source code for cTAKES versions 5+ is available from the [cTAKES GitHub repository](https://github.com/apache/ctakes).
1. Clone this repository
```
$ git clone https://github.com/apache/ctakes.git
```
2. Open your local copy of the repository in an IDE of your choice.
3. Run directly from the code (link).  
   or
4. Build a binary installation (link), and
5. Run a binary installation (link). 


## More information

Much more information can be found on the [cTAKES wiki](https://github.com/apache/ctakes/wiki).

You can also write to the cTAKES user and developer mailing lists: user at ctakes.apache.org and dev at apache.ctakes.org
and find answers to previously asked questions by searching the [user](https://lists.apache.org/list.html?user@ctakes.apache.org)
and [developer](https://lists.apache.org/list.html?dev@ctakes.apache.org) mail archives.