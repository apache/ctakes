/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.smokingstatus.cc;

import org.apache.ctakes.core.cc.AbstractFileWriter;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.smokingstatus.type.SmokingDocumentClassification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.util.Collection;

// TODO Rename.  Also, extend AbstractTableFileWriter.
public class RecordResolutionCasConsumer extends AbstractFileWriter<String> {

   static private final Logger LOGGER = LoggerFactory.getLogger( "RecordResolutionCasConsumer" );

   @ConfigurationParameter(
         name = "OutputFile",
         description = "Name of file to which smoking status for the corpus should be written."
   )
   private String _outputFile;

   @ConfigurationParameter(
         name = "Delimiter",
         description = "Name of the parameter that is specifies the delimiter for the output."
   )
   private String iv_delimiter;

   @ConfigurationParameter(
         name = "ProcessingCDADocument",
         description = "Specifies whether the cas should be handled as CDA (via 'plaintext' sofa view) or default "
                       + "(flat file).",
         mandatory = false
   )
   private boolean iv_useCDAProcess = false;

//   @ConfigurationParameter(
//         name = "RunPatientLevelClassification",
//         description = "Specifies whether post process should be run which provides the patient level classification.",
//         mandatory = false
//   )
//   private boolean iv_postPatientLvlProcess = false;
//
//   @ConfigurationParameter(
//         name = "FinalClassificationOutputFile",
//         description = "Optional path and file name of the output file which holds the patient level classification summary.",
//         mandatory = false
//   )
//   private String iv_patient_level_file;



   static private final String PLAIN_TEXT_VIEW = "plaintext";

   private final StringBuilder _sb = new StringBuilder();



   /**
    * System new line character
    * @throws ResourceInitializationException -
    */
   static private final String NEW_LINE = System.getProperty( "line.separator" );
//   private PatientLevelSmokingStatus patientSmokingStatus = null;



   /**
    * @param jCas the jcas passed to the process( jcas ) method.
    */
   @Override
   protected void createData( final JCas jCas ) {
      // This makes some pretty ugly output.  It would be better to use a TableWriter.
      final String documentId = DocIdUtil.getDocumentIdForFile( jCas );
      _sb.append( documentId );
      _sb.append( iv_delimiter );
      final Collection<SmokingDocumentClassification> smokeStats
            = JCasUtil.select( jCas, SmokingDocumentClassification.class );
      if ( smokeStats.isEmpty() ) {
         _sb.append( "Error in RecordResolutionCasConsumer:NO classification" );
      }
      for ( SmokingDocumentClassification smokeStat : smokeStats ) {
         //there should be just one SmokingDocumentClassification
         _sb.append( smokeStat.getClassification() );
         _sb.append( NEW_LINE );
      }
   }

   /**
    * @return completed patient JCases
    */
   @Override
   protected String getData() {
      return _sb.toString();
   }

   /**
    * Does nothing.
    * @param data -
    */
   @Override
   protected void writeComplete( final String data ) {}

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext aContext ) throws ResourceInitializationException {
      super.initialize( aContext );
//      if ( iv_postPatientLvlProcess ) {
//         if ( iv_patient_level_file != null && !iv_patient_level_file.isEmpty() ) {
//            patientSmokingStatus = new PatientLevelSmokingStatus();
//            patientSmokingStatus.setInputFile( iv_patient_level_file );
//         } else {
//            LOGGER.error( "RunPatientLevelClassification true, but no FinalClassificationOutputFile given." );
//         }
//      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      if ( iv_useCDAProcess ) {
         try {
            createData( jcas.getView( PLAIN_TEXT_VIEW ) );
         } catch ( CASException casE ) {
            LOGGER.error( casE.getMessage() );
         }
      } else {
         createData( jcas );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final String data, final String outputDir, final String documentId, final String fileName )
         throws IOException {
      final File file = new File( outputDir, fileName );
      LOGGER.info( "Writing smoking status for the corpus to " + file.getPath() + " ..." );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( getData() );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      final String outputDir = getSimpleSubDirectory().isEmpty()
                               ? getRootDirectory()
                               : getRootDirectory() + "/" + getSimpleSubDirectory();
      try {
         writeFile( getData(), outputDir, "", _outputFile );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      //  !! This did not work as advertised.  There should be 2 writers,
      //  one for corpus-level and one for patient-level

//      if ( iv_postPatientLvlProcess ) {
//         String filename = (String) getConfigParameterValue( PARAM_OUTPUT_FILE );
//
//         if ( iv_patient_level_file != null ) {
//            outFile = new File( iv_patient_level_file );
//         } else {
//            outFile = new File( filename.replace( filename, filename + "_patientLevel.txt" ) );
//         }
//         if ( !outFile.exists() ) {
//            outFile.createNewFile();
//         }
//         patientSmokingStatus.setOutputFile( outFile.getAbsolutePath() );
//         patientSmokingStatus.collectCounts( "\\" + iv_delimiter );
//         patientSmokingStatus.assignPatientLevelSmokingStatus();
//         patientSmokingStatus.printToFile();
      }


}
