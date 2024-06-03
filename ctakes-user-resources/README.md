This is not a normal module.  There should not be main code or main resources in this module.  
This module creates a ctakes-user-resources jar that can be a dependency for other projects.  
 
1.  We want to place user resources ( pipers, regex bsvs, stop words, etc.) in separate modules.  
2.  For editable 'runtime' dev flexibility we want said resources in the resources/ directory upon compile. 
3.  We want user resources to be available in binary installations under a common resources/ directory.  
4.  Given #2 and #3 above, we do not want multiple copies of user resources in jars and resources/ dirs.  
5.  There are external projects that depend upon user resources, so they must be available for 'getting'.  

Note: External projects include Apache cTAKES projects such as ctakes-web-rest.  
