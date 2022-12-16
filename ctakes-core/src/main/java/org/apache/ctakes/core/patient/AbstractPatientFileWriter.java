package org.apache.ctakes.core.patient;

import org.apache.ctakes.core.ae.NamedEngine;
import org.apache.ctakes.core.cc.AbstractFileWriter;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;


/**
 * Writes data for patient level Jcas.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/3/2018
 */
abstract public class AbstractPatientFileWriter
      extends AbstractFileWriter<Collection<JCas>>
      implements NamedEngine {

   static private final Logger LOGGER = Logger.getLogger( "AbstractPatientFileWriter" );

   static private final Object DATA_LOCK = new Object();

   private final Collection<JCas> _patientCases = new HashSet<>();

   protected AbstractPatientFileWriter() {
      PatientNoteStore.getInstance().registerEngine( getEngineName() );
   }

   /**
    * @param jCas the jcas passed to the process( jcas ) method.
    */
   @Override
   protected void createData( final JCas jCas ) {
      _patientCases.addAll( PatientNoteStore.getInstance().popPatientCases( getEngineName() ) );
   }

   /**
    * @return completed patient JCases
    */
   @Override
   protected Collection<JCas> getData() {
      return _patientCases;
   }

   /**
    * called after writing is complete
    *
    * @param data -
    */
   @Override
   protected void writeComplete( final Collection<JCas> data ) {
      _patientCases.clear();
   }

   /**
    * @param jCas       ignored
    * @param documentId ignored
    * @return the subdirectory set with the PARAM_SUBDIR parameter
    */
   @Override
   protected String getSubdirectory( final JCas jCas, final String documentId ) {
      return getSimpleSubDirectory();
   }

   /**
    * Write any remaining patient information
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      final String outputDir = getOutputDirectory( null, getRootDirectory(), "" );
      try {
         synchronized ( DATA_LOCK ) {
            createData( null );
            final Collection<JCas> data = getData();
            writeFile( data, outputDir, "", "" );
            writeComplete( data );
         }
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }

}
