package org.apache.ctakes.core.ae;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/5/2016
 */
@PipeBitInfo(
      name = "Regex Sectionizer",
      description = "Annotates Document Sections by detecting Section Headers using Regular Expressions provided in a Bar-Separated-Value (BSV) File.",
      products = { PipeBitInfo.TypeProduct.SECTION }
)
public class BsvRegexSectionizer extends RegexSectionizer {

   static private final Logger LOGGER = Logger.getLogger( "BsvRegexSectionizer" );


   static public final String SECTION_TYPES_PATH = "SectionsBsv";
   static public final String SECTION_TYPES_DESC
         = "path to a BSV file containing a list of regular expressions and corresponding section types.";

   @ConfigurationParameter(
         name = SECTION_TYPES_PATH,
         description = SECTION_TYPES_DESC,
         defaultValue = "org/apache/ctakes/core/sections/DefaultSectionRegex.bsv"
   )
   private String _sectionTypesPath;

   /**
    * {@inheritDoc}
    */
   @Override
   synchronized protected void loadSections() throws ResourceInitializationException {
      if ( _sectionTypesPath == null ) {
         LOGGER.error( "No " + SECTION_TYPES_DESC );
         return;
      }
      LOGGER.info( "Parsing " + _sectionTypesPath );
      try ( BufferedReader reader = new BufferedReader( new InputStreamReader( FileLocator
            .getAsStream( _sectionTypesPath ) ) ) ) {
         String line = reader.readLine();
         while ( line != null ) {
            parseBsvLine( line );
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
      LOGGER.info( "Finished Parsing" );
   }

   /**
    * @param line double-bar separated text
    */
   static private void parseBsvLine( final String line ) {
      if ( line.isEmpty() || line.startsWith( "#" ) || line.startsWith( "//" ) ) {
         // comment
         return;
      }
      final String[] splits = line.split( "\\|\\|" );
      if ( splits.length < 2 || isBoolean( splits[ 1 ] ) ) {
         LOGGER.warn( "Bad Section definition: " + line + " ; please use one of the following:\n" +
                      "NAME||HEADER_REGEX\n" +
                      "NAME||HEADER_REGEX||SHOULD_PARSE(true/false)\n" +
                      "NAME||HEADER_REGEX||FOOTER_REGEX\n" +
                      "NAME||HEADER_REGEX||FOOTER_REGEX||SHOULD_PARSE(true/false)\n" +
                      "The regex may contain \"(?<SECTION_NAME>regex_for_custom_section_name)\"" );
         return;
      }
      // Section Name is always first
      final String name = splits[ 0 ].trim();
      // Should parse flag is always last if specified, if not specified then true
      final String lastColumn = splits[ splits.length - 1 ].trim().toLowerCase();
      final boolean shouldParse = !lastColumn.equalsIgnoreCase( "false" );
      // header regex is first
      String headerRegex = splits[ 1 ].trim();
      // footer regex is after header regex, or may not be specified
      String footerRegex = null;
      if ( splits.length > 2 && !isBoolean( splits[ 2 ] ) ) {
         footerRegex = splits[ 2 ].trim();
      }
      final RegexSectionizer.SectionType sectionType
            = new RegexSectionizer.SectionType( name, headerRegex, footerRegex, shouldParse );
      addSectionType( sectionType );
   }


   static public AnalysisEngineDescription createEngineDescription( final String sectionTypesPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( BsvRegexSectionizer.class,
            SECTION_TYPES_PATH, sectionTypesPath );
   }


}
