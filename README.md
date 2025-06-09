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
1. **Java 17** is required to run cTAKES 6.0.0 and higher.  **Java 8 or Java 11** is required to run cTAKES 5.
 Run this command to check your Java version:
```
java -version
```
2. A license for the National Library of Medicine's [Unified Medical Language System (UMLS)](https://www.nlm.nih.gov/research/umls/index.html)
   is required to use the named entity recognition module (dictionary lookup) with the default dictionary.
3. **Python 3** is required to use cTAKES [Python Bridge to Java (PBJ)](https://github.com/apache/ctakes/wiki/pbj_intro).
   Run this to command to check your Python version:
```
python -V
```
> [!NOTE]
> If you are using an integrated development environment (IDE), please see its documentation on using git, Java, Python, and Apache Maven.
> You should be able to use features in your IDE instead of running commands in a terminal.

<br/>
### For developers:

1. Apache **Maven 3** is required to build cTAKES. Run this to command to check your Maven version:
```
mvn -version
```

<br/>


## Getting Started

### New Users (Non-Developers)

For access to all cTAKES capabilities, download a pre-built copy of a cTAKES installation from the [release area](https://github.com/apache/ctakes/releases).  
The names of pre-built installations follow the format `apache-ctakes-#.#.#-bin.zip`.
After unzipping the release file and obtaining a UMLS license, use the [UMLS Package Fetcher](https://github.com/apache/ctakes/wiki/cTAKES+UMLS+Package+Fetcher) GUI to install a copy of the
default dictionary for Named Entity Recognition (NER) using cTAKES Fast Dictionary Lookup.
You can then use the [Piper File Submitter](https://github.com/apache/ctakes/wiki/Piper+File+Submitter) GUI to submit jobs, 
or run any of the scripts in the `bin/` directory.


### New Developers

All source code for cTAKES versions 5+ is available from the [cTAKES GitHub repository](https://github.com/apache/ctakes).
1. Clone the cTAKES code repository using git.
```
git clone https://github.com/apache/ctakes.git
```
2. Compile the cTAKES code using Apache Maven.  In your cTAKES root directory, run this command:
```
mvn clean compile
```
3. [Download](https://sourceforge.net/projects/ctakesresources/files/sno_rx_16ab.zip) the default cTAKES dictionary zip file.
4. Copy the contents of the zip file to the `resources/org/apache/ctakes/dictionary/lookup/fast` directory.

> [!TIP]
> As an alternative to steps 3 and 4, you can use the [UMLS Package Fetcher](https://github.com/apache/ctakes/wiki/cTAKES+UMLS+Package+Fetcher) GUI.
> Run the class `DictionaryDownloader.java` to launch that tool, or use the `getUmlsDictionary` script if using a full build of cTAKES.

5. Run the cTAKES default pipeline using the Java class `PiperFileRunner.java`. To use the [Piper File Submitter](https://github.com/apache/ctakes/wiki/Piper+File+Submitter) GUI, run the `PiperRunnerGui.java` class.

> [!NOTE]
> To run the cTAKES Java classes, the full Java classpath must be configured. Setting up a classpath is beyond the scope of this document.  
> An integrated development environment (IDE) should set up the classpath for you, please see its documentation.

> [!IMPORTANT]
> You cannot run scripts in the `bin/` directory within a development environment.
> Within a cTAKES development environment you can run Java classes and Maven profiles, but no scripts in the `bin` directory.

> [!TIP]
> You can build your own cTAKES installation from a development environment using Apache Maven. 
> A cTAKES installation is required to run scripts in the `bin/` directory.

6. Build using Apache Maven:
```
mvn clean compile package
```

> [!NOTE]
> If you are using an integrated development environment (IDE), please see its documentation on using Apache Maven.

After packaging, there should be tar and zip files for `apache-ctakes-#.#.#.-bin` and ` apache-ctakes-#.#.#.-src` in your `ctakes-distribution/target/` directory.
7. Unzip the `apache-ctakes-#.#.#.-bin` into a directory *outside* your cTAKES development area.


## More information

You can write to the cTAKES user and developer mailing lists: **user** at `ctakes.apache.org` and **dev** at `apache.ctakes.org`
and find answers to previously asked questions by searching the [user](https://lists.apache.org/list.html?user@ctakes.apache.org)
and [developer](https://lists.apache.org/list.html?dev@ctakes.apache.org) mail archives.