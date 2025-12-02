# cTAKES CORE

Contains code and resources required by all or most other cTAKES modules.

Some of the primary functionality covered in ctakes-core and useful to beginners:
* Read files from disk: [FileTreeReader](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/cr/FileTreeReader.java)
* Find sections in a document: [SimpleSegmentAnnotator](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/ae/SimpleSegmentAnnotator.java), [BsvRegexSectionizer](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/ae/BsvRegexSectionizer.java)
* Find lists in a document: [ListAnnotator](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/ae/ListAnnotator.java)
* Find paragraphs in a document: [ParagraphAnnotator](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/ae/ParagraphAnnotator.java)
* Find sentences in a document: [SentenceDetector](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/ae/SentenceDetector.java), [SentenceDetectorAnnotatorBIO](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/ae/SentenceDetectorAnnotatorBIO.java)
* Find tokens in a document: [TokenizerAnnotator](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/ae/TokenizerAnnotator.java), [TokenizerAnnotatorPTB](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/ae/TokenizerAnnotatorPTB.java)
* Run Python scripts: [PythonRunner](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/ae/PythonRunner.java)
* Pip a Python project: [PythonPipper](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/ae/PythonPipper.java)
* Write HTML-marked document: [HtmlTextWriter](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/cc/html/HtmlTextWriter.java), [HtmlTextWriter](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/cc/pretty/html/HtmlTextWriter.java)
* Write plain-marked document: [PrettyTextWriterFit](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/cc/pretty/plaintext/PrettyTextWriterFit.java)
* Write all information to XML: [FileTreeXmiWriter](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/cc/FileTreeXmiWriter.java)
* Write annotation information to a table: [SemanticTableFileWriter](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/cc/SemanticTableFileWriter.java)
* Read and run piper files: [PiperFileReader](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/pipeline/PiperFileReader.java), [PiperFileRunner](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/pipeline/PiperFileRunner.java)
* Build pipelines from code: [PipelineBuilder](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/pipeline/PipelineBuilder.java)
* Find, create, inspect annotations: [util/annotation/](https://github.com/apache/ctakes/tree/main/ctakes-core/src/main/java/org/apache/ctakes/core/util/annotation)
* Create, inspect basic document information: [util/doc/](https://github.com/apache/ctakes/tree/main/ctakes-core/src/main/java/org/apache/ctakes/core/util/doc)
* Create, inspect basic time annotations: [CalendarUtil](https://github.com/apache/ctakes/blob/main/ctakes-core/src/main/java/org/apache/ctakes/core/util/CalendarUtil.java)

ctakes-core also contains two piper files that perform basic functions that are frequently used at the beginning of pipelines.
* [DefaultTokenizerPipeline.piper](https://github.com/apache/ctakes/blob/main/ctakes-core/src/user/resources/org/apache/ctakes/core/pipeline/DefaultTokenizerPipeline.piper)
* [FullTokenizerPipeline.piper](https://github.com/apache/ctakes/blob/main/ctakes-core/src/user/resources/org/apache/ctakes/core/pipeline/FullTokenizerPipeline.piper)

There are many more utility classes and resources in ctakes-core that are useful to cTAKES developers.

