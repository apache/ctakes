The context annotator provides a mechanism for examining the context of existing annotations, finding 
events of interest in the context, and acting on those events in some way.  The negation and status 
annotators both take advantage of this infrastructure by examining the context of named entities 
(e.g. disorders and findings) to see if they should be considered as negated (e.g. "no chest pain") 
or if their status should be modified (e.g. "myocardial infarction" should have status "history of").
In fact, the "negation annotator" is really just the context annotator configured to deal with negations.
Similarly, the "status annotator" is the context annotator configured to identify the status of named entities.  
To better understand the context annotator code you should start by reading the javadocs for the class 
```org.apache.ctakes.necontexts.ContextAnnotator.java```.  It provides a nice conceptual overview of how the code works.  
