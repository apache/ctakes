package org.apache.ctakes.gui.dictionary;


import org.apache.ctakes.gui.dictionary.umls.VocabularyStore;
import org.apache.log4j.Logger;

import java.io.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/12/2015
 */
final class DictionaryXmlWriter {

   static private final Logger LOGGER = Logger.getLogger( "DictionaryXmlWriter" );

   private DictionaryXmlWriter() {
   }

   static boolean writeXmlFile( final String databaseDir, final String databaseName ) {
      final File scriptFile = new File( databaseDir, databaseName + ".xml" );
      try ( final Writer writer = new BufferedWriter( new FileWriter( scriptFile ) ) ) {
         writer.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" );
         writer.write( "<!--\n" );
         writer.write( "Licensed to the Apache Software Foundation (ASF) under one\n" );
         writer.write( "or more contributor license agreements.  See the NOTICE file\n" );
         writer.write( "distributed with this work for additional information\n" );
         writer.write( "regarding copyright ownership.  The ASF licenses this file\n" );
         writer.write( "to you under the Apache License, Version 2.0 (the\n" );
         writer.write( "\"License\"); you may not use this file except in compliance\n" );
         writer.write( "with the License.  You may obtain a copy of the License at\n" );
         writer.write( "http://www.apache.org/licenses/LICENSE-2.0\n" );
         writer.write( "Unless required by applicable law or agreed to in writing,\n" );
         writer.write( "software distributed under the License is distributed on an\n" );
         writer.write( "\"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" );
         writer.write( "KIND, either express or implied.  See the License for the\n" );
         writer.write( "specific language governing permissions and limitations\n" );
         writer.write( "under the License.\n" );
         writer.write( "-->\n\n" );
         writer.write( "<!--    New format for the .xml lookup specification.  Uses table name and value type/class for Concept Factories.  -->\n" );
         writer.write( "<lookupSpecification>\n" );
         writer.write( "<dictionaries>\n" );
         writer.write( "   <dictionary>\n" );
         writer.write( "      <name>" + databaseName + "Terms</name>\n" );
         writer.write( "      <implementationName>org.apache.ctakes.dictionary.lookup2.dictionary.UmlsJdbcRareWordDictionary</implementationName>\n" );
         writer.write( "      <properties>\n" );
         writer.write( "<!-- urls for hsqldb memory connections must be file types in hsql 1.8.\n" );
         writer.write( "These file urls must be either absolute path or relative to current working directory.\n" );
         writer.write( "They cannot be based upon the classpath.\n" );
         writer.write( "Though JdbcConnectionFactory will attempt to \"find\" a db based upon the parent dir of the url\n" );
         writer.write( "for the sake of ide ease-of-use, the user should be aware of these hsql limitations.\n" );
         writer.write( "-->\n" );
         writer.write( createProperty( "jdbcDriver", "org.hsqldb.jdbcDriver" ) );
         writer.write( createProperty( "jdbcUrl",
               "jdbc:hsqldb:file:resources/org/apache/ctakes/dictionary/lookup/fast/" + databaseName + "/" +
               databaseName ) );
         writer.write( createProperty( "jdbcUser", "sa" ) );
         writer.write( createProperty( "jdbcPass", "" ) );
         writer.write( createProperty( "rareWordTable", "cui_terms" ) );
         writer.write( createProperty( "umlsUrl", "https://uts-ws.nlm.nih.gov/restful/isValidUMLSUser" ) );
         writer.write( createProperty( "umlsVendor", "NLM-6515182895" ) );
         writer.write( createProperty( "umlsUser", "CHANGE_ME" ) );
         writer.write( createProperty( "umlsPass", "CHANGE_ME" ) );
         writer.write( "      </properties>\n" );
         writer.write( "   </dictionary>\n" );
         writer.write( "</dictionaries>\n" );
         writer.write( "\n" );
         writer.write( "<conceptFactories>\n" );
         writer.write( "   <conceptFactory>\n" );
         writer.write( "      <name>" + databaseName + "Concepts</name>\n" );
         writer.write( "      <implementationName>org.apache.ctakes.dictionary.lookup2.concept.UmlsJdbcConceptFactory</implementationName>\n" );
         writer.write( "      <properties>\n" );
         writer.write( createProperty( "jdbcDriver", "org.hsqldb.jdbcDriver" ) );
         writer.write( createProperty( "jdbcUrl",
               "jdbc:hsqldb:file:resources/org/apache/ctakes/dictionary/lookup/fast/" + databaseName + "/" +
               databaseName ) );
         writer.write( createProperty( "jdbcUser", "sa" ) );
         writer.write( createProperty( "jdbcPass", "" ) );
         writer.write( createProperty( "umlsUrl", "https://uts-ws.nlm.nih.gov/restful/isValidUMLSUser" ) );
         writer.write( createProperty( "umlsVendor", "NLM-6515182895" ) );
         writer.write( createProperty( "umlsUser", "CHANGE_ME" ) );
         writer.write( createProperty( "umlsPass", "CHANGE_ME" ) );
         writer.write( createProperty( "tuiTable", "tui" ) );
         writer.write( createProperty( "prefTermTable", "prefTerm" ) );
         writer.write( "<!-- Optional tables for optional term info.\n" );
         writer.write( "Uncommenting these lines alone may not persist term information;\n" );
         writer.write( "persistence depends upon the TermConsumer.  -->\n" );
         for ( String vocabulary : VocabularyStore.getInstance().getAllVocabularies() ) {
            writer.write( createProperty( vocabulary.toLowerCase().replace( '.', '_' ).replace( '-', '_' )
                                          + "Table", VocabularyStore.getInstance().getCtakesClass( vocabulary ) ) );
         }
         writer.write( "      </properties>\n" );
         writer.write( "   </conceptFactory>\n" );
         writer.write( "</conceptFactories>\n" );
         writer.write( "\n" );
         writer.write( "<!--  Defines what terms and concepts will be used  -->\n" );
         writer.write( "<dictionaryConceptPairs>\n" );
         writer.write( "   <dictionaryConceptPair>\n" );
         writer.write( "      <name>" + databaseName + "Pair</name>\n" );
         writer.write( "      <dictionaryName>" + databaseName + "Terms</dictionaryName>\n" );
         writer.write( "      <conceptFactoryName>" + databaseName + "Concepts</conceptFactoryName>\n" );
         writer.write( "   </dictionaryConceptPair>\n" );
         writer.write( "</dictionaryConceptPairs>\n" );
         writer.write( "\n" );
         writer.write( "<!-- DefaultTermConsumer will persist all spans.\n" );
         writer.write( "PrecisionTermConsumer will only persist only the longest overlapping span of any semantic group.\n" );
         writer.write( "SemanticCleanupTermConsumer works as Precision** but also removes signs/sympoms contained within disease/disorder,\n" );
         writer.write( "and (just in case) removes any s/s and d/d that are also (exactly) anatomical sites. -->\n" );
         writer.write( "<rareWordConsumer>\n" );
         writer.write( "   <name>Term Consumer</name>\n" );
         writer.write( "   <implementationName>org.apache.ctakes.dictionary.lookup2.consumer.DefaultTermConsumer</implementationName>\n" );
         writer.write( "   <!--<implementationName>org.apache.ctakes.dictionary.lookup2.consumer.PrecisionTermConsumer</implementationName>-->\n" );
         writer.write( "   <!--<implementationName>org.apache.ctakes.dictionary.lookup2.consumer.SemanticCleanupTermConsumer</implementationName>-->\n" );
         writer.write( "   <properties>\n" );
         writer.write( "<!-- Depending upon the consumer, the value of codingScheme may or may not be used.  With the packaged consumers,\n" );
         writer.write( "codingScheme is a default value used only for cuis that do not have secondary codes (snomed, rxnorm, etc.)  -->\n" );
         writer.write( createProperty( "codingScheme", databaseName ) );
         writer.write( "   </properties>\n" );
         writer.write( "</rareWordConsumer>\n" );
         writer.write( "\n" );
         writer.write( "</lookupSpecification>\n" );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return false;
      }
      return true;
   }

   static private String createProperty( final String name, final String value ) {
      return "         <property key=\"" + name + "\" value=\"" + value + "\"/>\n";
   }

}
