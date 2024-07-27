package org.apache.ctakes.gui.dictionary.cased;


import org.apache.ctakes.gui.dictionary.umls.VocabularyStore;
import org.apache.ctakes.gui.dictionary.util.HsqlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/28/2020
 */
final public class CasedPiperWriter {

   static private final Logger LOGGER = LoggerFactory.getLogger( "CasedPiperWriter" );


   private CasedPiperWriter() {
   }


   static public boolean writePiper( final String hsqlPath,
                                     final String dictionaryName,
                                     final Collection<String> writtenSchema ) {
//      final String url = HsqlUtil.URL_PREFIX + hsqlPath.replace( '\\', '/' ) + "/" + dictionaryName + "/" +
//                         dictionaryName;
      final String url = HsqlUtil.URL_PREFIX + "resources/org/apache/ctakes/dictionary/lookup/cased/"
                         + dictionaryName + "/"
                         + dictionaryName;
      final List<String> schemaList = new ArrayList<>( writtenSchema );
      Collections.sort( schemaList );
      schemaList.add( "TUI" );
      schemaList.add( "PREFERRED_TEXT" );
      final String schemas = String.join( ",", schemaList );
      final File piperFile = new File( hsqlPath, dictionaryName + ".piper" );
      try ( final Writer writer = new BufferedWriter( new FileWriter( piperFile ) ) ) {
         writer.write(
               "// This piper file contains instructions to set up your custom dictionary and encoders for "
               + "Case-sensitive Dictionary Lookup.\n" );
         writer.write( "// To use your new dictionary, load this piper in your main piper:\n" );
         writer.write( "// load " + hsqlPath + "\n" );
         writer.write( "\n" );
         writer.write( "//             ===  Setup common to all Dictionaries  ===\n" );
         writer.write( "//               =  Trigger Part of Speech  =\n" );
         writer.write( "//    Use Verbs as lookup tokens.  Default = yes.\n" );
         writer.write( "// set lookupVerbs=yes\n" );
         writer.write( "//    Use Nouns as lookup tokens.  Default = yes.\n" );
         writer.write( "// set lookupNouns=yes\n" );
         writer.write( "//    Use Adjectives as lookup tokens.  Default = yes.\n" );
         writer.write( "// set lookupAdjectives=yes\n" );
         writer.write( "//    Use Adverbs as lookup tokens.  Default = yes.\n" );
         writer.write( "// set lookupAdverbs=yes\n" );
         writer.write( "//    Comma delimited array of other parts of speech to use for lookup.  Default is empty.\n" );
         writer.write( "// set otherLookups=\n" );
         writer.write( "//               =  Trigger Word Length  =\n" );
         writer.write( "//    Minimum character span to use for lookup.  Default is 3.\n" );
         writer.write( "// set minimumSpan=3\n" );
         writer.write( "//               =  Text Loose Matching  =\n" );
         writer.write( "//    Allow words to be skipped in lookup.  Default is no.\n" );
         writer.write( "// set allowWordSkips=no\n" );
         writer.write( "//    Number of words that can be skipped consecutively in lookup.  Default is 2.\n" );
         writer.write( "// set consecutiveSkips=2\n" );
         writer.write( "//    Number of words that can be skipped in total in lookup.  Default is 4.\n" );
         writer.write( "// set totalSkips=4\n" );
         writer.write( "//               =  Subsumption  =\n" );
         writer.write( "//    Subsume small terms by larger enclosing terms in the same semantic group.  Default is yes.\n" );
         writer.write( "//      This is not the default behavior of the default dictionary lookup, but that of the PrecisionTermConsumer.\n" );
         writer.write( "// set subsume=yes\n" );
         writer.write( "//    Subsume contained terms of the same and certain other semantic groups.  Default is yes.\n" );
         writer.write( "//      This is not the default behavior of the default dictionary lookup, but that of the SemanticCleanupTermConsumer.\n" );
         writer.write( "// set subsumeSemantics=yes\n" );
         writer.write( "//    Comma delimited array of semantic types to group reassignment key:value pairs.  Default is empty.\n" );
         writer.write( "//      Within the comma delimited array types and groups are separated by a colon.\n" );
         writer.write( "//      Semantic Type can be indicated by name or TUI.  Semantic Group must be indicated by name.\n" );
         writer.write( "//      Example:     set reassignSemantics=Cell:Finding,T065:Event\n" );
         writer.write( "// set reassignSemantics=\n" );
         writer.write( "\n" );
         writer.write( "//             ===  Dictionaries Setup  ===\n" );
         writer.write( "//               =  Dictionary Names  =\n" );
         writer.write( "//    Comma delimited array of dictionary names.\n" );
         writer.write( "set dictionaries=" + dictionaryName + "\n" );
         writer.write( "\n" );
         writer.write( "//             ===  Individual Dictionary Setup  ===\n" );
         writer.write( "//    Individual Dictionary setup parameters are named {dictionaryName}_{parameterName}.\n" );
         writer.write( "//               =  Dictionary Type  =\n" );
         writer.write( "//    Declare the source type the Dictionary.  {dictionaryName}_type\n" );
         writer.write( "set " + dictionaryName + "_type=JDBC\n" );
         writer.write( "\n" );
         writer.write( "//               =  JDBC Database  =\n" );
         writer.write( "//    JDBC Driver for the Dictionary.  {dictionaryName}_driver\n" );
         writer.write( "set " + dictionaryName + "_driver=org.hsqldb.jdbcDriver\n" );
         writer.write( "//    Url for the Database.  {dictionaryName}_url\n" );
         writer.write( "set " + dictionaryName + "_url=" + url + "\n" );
         writer.write( "//    User for the Database.  {dictionaryName}_user.\n" );
         writer.write( "// set " + dictionaryName + "_user=sa\n" );
         writer.write( "//    Password for the Database.  {dictionaryName}_pass\n" );
         writer.write( "// set " + dictionaryName + "_pass=\n" );
         writer.write( "//               =  JDBC Term Tables  =\n" );
         writer.write( "//    Upper case Term Table in the Database.  {dictionaryName}_upper\n" );
         writer.write( "// set " + dictionaryName + "_upper=UPPER\n" );
         writer.write( "//    Mixed case Term Table in the Database.  {dictionaryName}_mixed\n" );
         writer.write( "// set " + dictionaryName + "_mixed=MIXED\n" );
         writer.write( "//    Lower case Term Table in the Database.  {dictionaryName}_lower\n" );
         writer.write( "// set " + dictionaryName + "_lower=LOWER\n" );
         writer.write( "\n" );
         writer.write( "//             ===  Encoders Setup  ===\n" );
         writer.write( "//    Comma delimited array of encoder names.  Note that these names also indicate a Code Schema name.\n" );
         writer.write( "set encoders=" + schemas + "\n" );
         writer.write( "\n" );
         writer.write( "//             ===  Individual Encoder Setup  ===\n" );
         writer.write( "//    Individual Encoder setup parameters are named {encoderName}_{parameterName}.\n" );
         writer.write( "//               =  Encoder Type  =\n" );
         writer.write( "//    Declare the source type the Encoder.  {encoderName}_type\n" );
         for ( String schema : schemaList ) {
            writer.write( "set " + schema + "_type=JDBC\n" );
         }
         writer.write( "\n" );
         writer.write( "//               =  JDBC Database  =\n" );
         writer.write( "//    JDBC Driver for the Encoder.  {encoderName}_driver\n" );
         writer.write( "//    The default JDBC driver is org.hsqldb.jdbcDriver\n\n" );
         writer.write( "//    Url for the Database.  {encoderName}_url\n" );
         for ( String schema : schemaList ) {
            writer.write( "set " + schema + "_url=" + url + "\n" );
         }
         writer.write( "//    Most of the following settings are left empty to exemplify brevity.\n\n" );
         writer.write( "//    User for the Database.  {encoderName}_user   Default user is sa\n\n" );
         writer.write( "//    Password for the Database.  {encoderName}_pass   Default password is empty.\n\n" );
         writer.write( "//               =  JDBC Encoder Tables  =\n" );
         writer.write( "//    Encoding Table in the Database.  {encoderName}_table   Default table is the schema name.\n\n" );
         writer.write( "//    Encoding Table Class Type.  {encoderName}_class\n" );
         for ( String schema : schemaList ) {
            if ( schema.equals( "TUI" ) ) {
               writer.write( "set TUI_class=tui\n" );
               continue;
            } else if ( schema.equals( "PREFERRED_TEXT" ) ) {
               writer.write( "set PREFERRED_TEXT_class=pref_text\n" );
               continue;
            }
            writer.write( "set " + schema + "_class="
                          + VocabularyStore.getInstance().getCtakesClass( schema ) + "\n" );
         }
         writer.write( "\n" );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return false;
      }
      return true;
   }


}
