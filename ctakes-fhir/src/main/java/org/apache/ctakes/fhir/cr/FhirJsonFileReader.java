package org.apache.ctakes.fhir.cr;

import org.apache.ctakes.core.cr.AbstractFileTreeReader;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.hl7.fhir.dstu3.model.Bundle;

import java.io.File;
import java.io.IOException;


/**
 * Unfinished collection reader to create ctakes annotations from fhir files.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/22/2018
 */
@PipeBitInfo(
      name = "FhirJsonFileReader",
      description = "Reads fhir information from json.", role = PipeBitInfo.Role.READER
)
public class FhirJsonFileReader extends AbstractFileTreeReader {

   static private final Logger LOGGER = Logger.getLogger( "FhirJsonFileReader" );

   /**
    * {@inheritDoc}
    */
   @Override
   protected void readFile( final JCas jCas, final File file ) throws IOException {
      jCas.reset();

      final Bundle bundle = BundleReader.readJsonBundle( file );

      BundleParser.parseBundle( jCas, bundle );
   }


}
