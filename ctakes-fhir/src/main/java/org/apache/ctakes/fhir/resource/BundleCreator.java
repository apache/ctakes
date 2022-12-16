package org.apache.ctakes.fhir.resource;

import org.apache.ctakes.fhir.element.FhirElementFactory;
import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.hl7.fhir.dstu3.model.Bundle;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/25/2017
 */
final public class BundleCreator implements FhirResourceCreator<TOP, Bundle> {

   static private final Logger LOGGER = Logger.getLogger( "BundleCreator" );

   static private final String CTAKES_BUNDLE_ID = "ctakes_bundle";
   static private final DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyyMMddhhmm" );

   /**
    * {@inheritDoc}
    */
   @Override
   public Bundle createResource( final JCas jCas, final TOP nullified, final FhirPractitioner practitioner,
                                 final FhirNoteSpecs noteSpecs ) {
      final Bundle bundle = new Bundle();
      final String noteTime = DATE_FORMAT.format( new Date() );
      bundle.setId( FhirElementFactory.createId( jCas, CTAKES_BUNDLE_ID, noteTime ) );
      // The bundle is a collection; created for ease of distribution.
      bundle.setType( Bundle.BundleType.COLLECTION );
      return bundle;
   }

}
