package org.apache.ctakes.fhir.cr;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.*;
import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/1/2020
 */
final public class BundleReader {

   private BundleReader() {
   }

   static private final Logger LOGGER = Logger.getLogger( "BundleReader" );


   static public Bundle readJsonBundle( final File file ) throws IOException {
      final FhirContext fhirContext = FhirContext.forDstu3();
      return readFileBundle( fhirContext.newJsonParser(), file );
   }

   static public Bundle readJsonBundle( final String text ) throws IOException {
      IBaseResource baseResource;
      final FhirContext fhirContext = FhirContext.forDstu3();
      final IParser jsonParser = fhirContext.newJsonParser();
      try {
         baseResource = jsonParser.parseResource( text );
      } catch ( ConfigurationException | DataFormatException multE ) {
         throw new IOException( multE );
      }
      if ( baseResource == null ) {
         throw new IOException( "Null Bundle" );
      }
      if ( !Bundle.class.isInstance( baseResource ) ) {
         throw new IOException( "Resource is not a Bundle" );
      }
      return (Bundle)baseResource;
   }

   static public Bundle readXmlBundle( final File file ) throws IOException {
      final FhirContext fhirContext = FhirContext.forDstu3();
      return readFileBundle( fhirContext.newXmlParser(), file );
   }

   static private Bundle readFileBundle( final IParser iParser, final File file ) throws IOException {
      IBaseResource baseResource;
      final FhirContext fhirContext = FhirContext.forDstu3();
      try ( Reader reader = new BufferedReader( new FileReader( file ) ) ) {
         baseResource = iParser.parseResource( reader );
      } catch ( IOException | ConfigurationException | DataFormatException multE ) {
         throw new IOException( "Could not read fhir from " + file.getAbsolutePath(), multE );
      }
      if ( baseResource == null ) {
         throw new IOException( "Null Bundle for file " + file.getAbsolutePath() );
      }
      if ( !Bundle.class.isInstance( baseResource ) ) {
         throw new IOException( "Resource is not a Bundle for file " + file.getAbsolutePath() );
      }
      return (Bundle)baseResource;
   }


}
