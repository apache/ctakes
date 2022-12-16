package org.apache.ctakes.core.pipeline;

import com.lexicalscope.jewel.cli.Option;

/**
 * Standard Options for ctakes.  For instance; -p PiperFile, -i InputDir, -o OutputDir.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/7/2017
 */
public interface StandardCliOptions {

   static public final String UMLS_KEY = "key";
   static public final String UMLS_USER = "user";
   static public final String UMLS_PASS = "pass";
   static public final String LOOKUP_XML = "lookupXml";
   static public final String PIPER_FILE = "piper";
   static public final String INPUT_DIR = "inputDir";
   static public final String OUTPUT_DIR = "outputDir";
   static public final String SUB_DIR = "subDir";
   static public final String XMI_OUT_DIR = "xmiOut";
   static public final String HTML_OUT_DIR = "htmlOut";


   @Option(
         shortName = "p",
         longName = PIPER_FILE,
         description = "path to the piper file containing commands and parameters for pipeline configuration.",
         defaultValue = "" )
   String getPiperPath();

   @Option(
         shortName = "i",
         longName = INPUT_DIR,
         description = "path to the directory containing the clinical notes to be processed.",
         defaultValue = "" )
   String getInputDirectory();

   @Option(
         shortName = "o",
         longName = OUTPUT_DIR,
         description = "path to the directory where the output files are to be written.",
         defaultValue = "" )
   String getOutputDirectory();

   @Option(
         shortName = "s",
         longName = SUB_DIR,
         description = "path to a subdirectory for output files.",
         defaultValue = "" )
   String getSubDirectory();

   @Option(
         longName = XMI_OUT_DIR,
         description = "path to the directory where xmi files are to be written.  Adds XmiWriter to pipeline.",
         defaultValue = "" )
   String getXmiOutDirectory();

   @Option(
         longName = HTML_OUT_DIR,
         description = "path to the directory where html files are to be written.  Adds HtmlWriter to pipeline.",
         defaultValue = "" )
   String getHtmlOutDirectory();

   @Option(
         shortName = "l",
         longName = LOOKUP_XML,
         description = "path to the xml file containing information for dictionary lookup configuration.",
         defaultValue = "" )
   String getLookupXml();

   @Option(
         longName = UMLS_USER,
         description = "UMLS username.",
         defaultValue = "" )
   String getUmlsUserName();

   @Option(
         longName = UMLS_PASS,
         description = "UMLS user password.",
         defaultValue = "" )
   String getUmlsPassword();


   @Option(
         longName = UMLS_KEY,
         description = "UMLS API Key.",
         defaultValue = "" )
   String getUmlsApiKey();


   @Option(
         shortName = "?",
         longName = "help",
         description = "print usage.",
         helpRequest = true )
   boolean isHelpWanted();

}
