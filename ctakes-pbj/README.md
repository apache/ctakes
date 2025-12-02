# A Python Bridge to Java (PBJ).

## Problem Statement
Solutions start with identifying the problem. 
Our problem is the lack of a standardized path to move information from cTAKES to a python program (and back again).
Having that ability is very important as most modern Machine Learning is done in Python.

## Solution
The information that we want to move is stored in an object called a CAS (Common Analysis System). 
All objects within the CAS are of a Type defined in an extensible Type System. 
For instance a discovered instance of "cancer" is stored in the CAS as an object of Type "DiseaseDisorderMention".  

The next step was for us to choose a method of delivery for our path of information.
We were looking for something that could handle multiple sub-pipelines, allow for parallel sub-pipelines, 
and a method that is fast, reusable, and easy to use.  

[Apache ActiveMQ](https://activemq.apache.org/components/artemis/) Message Broker combined with
[dkpro-cassis](https://github.com/dkpro/dkpro-cassis) became apparent as the ideal solution to our problem, 
allowing what we hoped for above and more.

## How it Works

![](https://raw.githubusercontent.com/wiki/apache/ctakes/images/step_1.png)

![](https://raw.githubusercontent.com/wiki/apache/ctakes/images/step_2.png)

![](https://raw.githubusercontent.com/wiki/apache/ctakes/images/step_3.png)

![](https://raw.githubusercontent.com/wiki/apache/ctakes/images/step_4.png)

## Other Configurations

PBJ development has focused on simple Python pipeline to cTAKES (Java) pipeline integration.
The most common configuration in the PBJ pipers that come with cTAKES is the first below: Separate before/after cTAKES processes.

![](https://raw.githubusercontent.com/wiki/apache/ctakes/images/other_configs.png)

### Introduction pipelines can be found in the ctakes-examples module.

An introductory "single-stream" pipeline such as is displayed in the section **How it Works**, is [PbjWordFinderInOne.piper](https://github.com/apache/ctakes/blob/main/ctakes-examples/src/user/resources/org/apache/ctakes/examples/pipeline/PbjWordFinderInOne.piper),
which spins up a single cTAKES Java pipeline that tokenizes a document, then sends document information to a Python *sub*-pipeline named
[word_finder_pipeline](https://github.com/apache/ctakes/blob/main/ctakes-pbj/src/user/resources/org/apache/ctakes/pbj/ctakes_pbj_py/src/ctakes_pbj/examples/word_finder_pipeline.py).
The Python pipeline will search the document for a preset list of words: 'breast', 'hernia', 'pain', 'migraines', 'allergies', 'thyroidectomy', 'exam'.
The Python pipeline will send information back to the main cTAKES pipeline, which will write the information in several different file formats.

There is an example of the same thing, but with three separate communicating pipelines, as the **Separate before/after cTAKES processes** in section **Other Configurations**: cTAKES to Python to CTAKES in [PbjWordFinder.piper](https://github.com/apache/ctakes/blob/main/ctakes-examples/src/user/resources/org/apache/ctakes/examples/pipeline/PbjWordFinder.piper).

