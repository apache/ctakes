package org.apache.ctakes.core.cc.pretty.plaintext;

//import org.apache.log4j.Logger;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;

import static org.apache.ctakes.core.config.ConfigParameterConstants.PARAM_OUTPUTDIR;
import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * Writes Document text, pos, semantic types and cuis.  Each Sentence starts a new series of pretty text lines.
 * This version can be used in the UimaFit style with {@link org.apache.uima.fit.descriptor.ConfigurationParameter}
 * It cannot be used in the old Uima CPE style (e.g. the Uima CPE Gui) as the Uima CPE has problems with Fit Consumers.
 * There is a version that can be used with the CPE GUI:
 * {@link org.apache.ctakes.core.cc.pretty.plaintext.PrettyTextWriterUima}
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @see org.apache.ctakes.core.cc.pretty.plaintext.PrettyTextWriter
 * @since 7/8/2015
 */
@PipeBitInfo(
      name = "Pretty Text Writer",
      description = "Writes text files with document text and simple markups (POS, Semantic Group, CUI, Negation).",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, SENTENCE, BASE_TOKEN },
      usables = { DOCUMENT_ID_PREFIX, IDENTIFIED_ANNOTATION, EVENT, TIMEX, TEMPORAL_RELATION }
)
final public class PrettyTextWriterFit extends AbstractJCasFileWriter {


   // delegate
   final private PrettyTextWriter _prettyTextWriter;

   public PrettyTextWriterFit() {
      super();
      _prettyTextWriter = new PrettyTextWriter();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      _prettyTextWriter.writeFile( jCas, outputDir + "/" + documentId + ".pretty.txt" );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      super.initialize( uimaContext );
      try {
         _prettyTextWriter.setOutputDirectory( (String)uimaContext.getConfigParameterValue( PARAM_OUTPUTDIR ) );
      } catch ( IllegalArgumentException | SecurityException multE ) {
         // thrown if the path specifies a File (not Dir) or by file system access methods
         throw new ResourceInitializationException( multE );
      }
   }


//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public void process( final CAS aCAS ) throws AnalysisEngineProcessException {
//      JCas jcas;
//      try {
//         jcas = aCAS.getJCas();
//      } catch ( CASException casE ) {
//         throw new AnalysisEngineProcessException( casE );
//      }
//      _prettyTextWriter.process( jcas );
//   }
//
//   /**
//    * @return This Cas Consumer as an Analysis Engine
//    * @throws org.apache.uima.resource.ResourceInitializationException if anything went wrong
//    */
//   static public AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
//      return createAnnotatorDescription( "" );
//   }
//
//   /**
//    * @param outputDirectoryPath may be empty or null, in which case the current working directory is used
//    * @return This Cas Consumer as an Analysis Engine
//    * @throws org.apache.uima.resource.ResourceInitializationException if anything went wrong
//    */
//   static public AnalysisEngineDescription createAnnotatorDescription( final String outputDirectoryPath )
//         throws ResourceInitializationException {
//      return AnalysisEngineFactory.createEngineDescription( PrettyTextWriterFit.class,
//            PARAM_OUTPUTDIR, outputDirectoryPath );
//   }

}
