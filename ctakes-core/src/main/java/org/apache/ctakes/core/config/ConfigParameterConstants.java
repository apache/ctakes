package org.apache.ctakes.core.config;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/28/2015
 */
final public class ConfigParameterConstants {

   private ConfigParameterConstants() {
   }

   /**
    * Name of configuration parameter that can be set to the path of
    * a Piper file.
    */
   static public final String PARAM_PIPER = "Piper";
   static public final String OPTION_PIPER = "-p";
   static public final String DESC_PIPER = "Location of a Piper File.";


   /**
    * Name of configuration parameter that can be set to the path of
    * a directory containing input files.
    */
   static public final String PARAM_INPUTDIR = "InputDirectory";
   static public final String OPTION_INPUTDIR = "-i";
   static public final String DESC_INPUTDIR = "Directory for all input files.";

   /**
    * Name of configuration parameter that can be set to the path of
    * a directory containing output files.
    */
   static public final String PARAM_OUTPUTDIR = "OutputDirectory";
   static public final String OPTION_OUTPUTDIR = "-o";
   static public final String DESC_OUTPUTDIR = "Directory for all output files.";

   /**
    * Name of configuration parameter that can be set to the path of
    * a subdirectory for input or output files.
    */
   static public final String PARAM_SUBDIR = "SubDirectory";
   static public final String OPTION_SUBDIR = "-s";
   static public final String DESC_SUBDIR = "SubDirectory for files.";

   /**
    * Name of configuration parameter that can be set to the path of
    * a file containing dictionary lookup configuration.
    */
   static public final String PARAM_LOOKUP_XML = "LookupXml";
   static public final String OPTION_LOOKUP_XML = "-l";
   static public final String DESC_LOOKUP_XML = "Path to the xml file containing information for dictionary lookup configuration.";

}
