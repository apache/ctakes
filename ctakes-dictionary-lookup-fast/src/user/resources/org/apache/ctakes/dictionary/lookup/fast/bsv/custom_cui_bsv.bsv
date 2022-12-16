1|ctakes
1|ctakes application
// line comments can start with double-slash
2|new dictionary lookup
2|dictionary lookup, new
2|fast dictionary lookup
2|dictionary-lookup-fast
# line comments can start with hash
3|text tokenization is automatic, for bsv
3|for .bsv files, text-tokenization is done upon loading
3|There's no need to WORRY about matching the cTakes token scheme
4|the entire bsv dictionary is loaded, then rare words are determined
4|the rare-word indexing is used for custom bsv just like umls hsql
5|CUIs must be 1-7 digits characters.  First-character "C" is optional
C5|Note that C5, C0000005 5, 05, and 005 are translated as the same cui
6|The bsv loader will detect 2 or 3 or 4 columns
6|2 columns are parsed as CUI , Text if set as a dictionary
6|2 columns are parsed as CUI , TUI if set as a concept factory
7|3 columns are parsed as CUI , TUI , Text if set as a dictionary and/or concept factory
8|4 columns are parsed as CUI , TUI , Text , Preferred Term if set as a dictionary and/or concept factory


