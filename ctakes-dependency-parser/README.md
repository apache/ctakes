Dependency parsers provide syntactic information about sentences. 
Unlike deep parsers, they do not explicitly find phrases (e.g., NP or VP); rather, they find the dependencies between words.
For example, "hormone replacement therapy" would have deep structure:  
`````(NP (NML (NN hormone) (NN replacement)) (NN therapy))`````  
but its dependency structure would show that "hormone" depends on "replacement" and "replacement" in turn depends on "therapyl". 
Below, the first column of numbers indicates the ID of the word, and the second number indicates what it is dependent on.  
```23 hormone hormone NN 24 NMOD 24 replacement replacement NN 25 NMOD 25 therapy therapy NN 22 PMOD```  
Dependency parsers can be labeled as well, e.g., we could specify that "hormone" is in a noun-modifier (i.e., NMOD) relationship with "therapy" in the example above (the last column).  
This project provides an [Apache UIMA](https://uima.apache.org/) wrapper and some utilities for [ClearParser](https://github.com/clearnlp),
a transition-based dependency parser that achieves state-of-the-art accuracy and speed.  

ClearParser is described in:  
"K-best, Locally Pruned, Transition-based Dependency Parsing Using Robust Risk Minimization."
Jinho D. Choi, Nicolas Nicolov, Collections of Recent Advances in Natural Language Processing V,
205-216, John Benjamins, Amsterdam & Philadelphia, 2009.  

The semantic role labeler assigns the predicate-argument structure of the sentence. (Who did what to whom when and where.) 