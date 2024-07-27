package org.apache.ctakes.examples.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.core.cc.XMISerializer;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Collection;

/**
 * Write xmi files in a directory tree mimicking that of the input files
 *
 * @author SPF , chip-nlp
 * @since 5/24/2021
 */
@PipeBitInfo(
      name = "XMI Writer (Dir Tree, Views)",
      description = "Writes XMI files with full representation of input text and all extracted information per View.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID },
      usables = { PipeBitInfo.TypeProduct.DOCUMENT_ID_PREFIX }
)
final public class FileTreeViewXmiWriter extends AbstractJCasFileWriter {

   static private final Logger LOGGER = LoggerFactory.getLogger( "FileTreeViewXmiWriter" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas, final String outputDir,
                          final String documentId, final String fileName ) throws IOException {

      final Collection<JCas> views = PatientViewUtil.getAllViews( jCas );
      for ( JCas view : views ) {
         final File xmiFile = new File( outputDir, fileName + "_" + view.getViewName() + ".xmi" );
         LOGGER.info( "Writing XMI to " + xmiFile.getPath() + " ..." );
         try {
            writeXmi( jCas.getCas(), xmiFile );
         } catch ( IOException | SAXException multE ) {
            throw new IOException( multE );
         }
         LOGGER.info( "Finished Writing" );
      }

   }

   /**
    * Serialize a CAS to a file in XMI format
    *
    * @param cas  CAS to serialize
    * @param file output file
    * @throws IOException  -
    * @throws SAXException -
    */
   static private void writeXmi( final CAS cas, final File file ) throws IOException, SAXException {
      try ( OutputStream outputStream = new BufferedOutputStream( new FileOutputStream( file ) ) ) {
         XmiCasSerializer casSerializer = new XmiCasSerializer( cas.getTypeSystem() );
         XMISerializer xmiSerializer = new XMISerializer( outputStream );
         casSerializer.serialize( cas, xmiSerializer.getContentHandler() );
      }
   }

   public static AnalysisEngine createEngine( final String outputDirectory ) throws ResourceInitializationException {
      return AnalysisEngineFactory
            .createEngine( org.apache.ctakes.core.cc.FileTreeXmiWriter.class, ConfigParameterConstants.PARAM_OUTPUTDIR,
                           outputDirectory );
   }


}