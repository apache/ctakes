package org.apache.ctakes.core.cc.pretty.plaintext;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import static org.apache.ctakes.core.config.ConfigParameterConstants.PARAM_OUTPUTDIR;
import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * Writes Document text, pos, semantic types and cuis.  Each Sentence starts a new series of pretty text lines.
 * This can be used with the old descriptor .xml files and the UIMA CPE Gui.  For a UimaFit PrettyTextWriter, use
 * {@link org.apache.ctakes.core.cc.pretty.plaintext.PrettyTextWriterFit}
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @see org.apache.ctakes.core.cc.pretty.plaintext.PrettyTextWriter
 * @since 6/24/2015
 */
@PipeBitInfo(
      name = "Pretty Text Writer (UIMA)",
      description = "Writes text files with document text and simple markups (POS, Semantic Group, CUI, Negation).",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, SENTENCE, BASE_TOKEN },
      usables = { IDENTIFIED_ANNOTATION, EVENT, TIMEX, TEMPORAL_RELATION }
)
final public class PrettyTextWriterUima extends CasConsumer_ImplBase {


   static private final Logger LOGGER = Logger.getLogger( "PrettyTextWriterUima" );

   // delegate
   final private PrettyTextWriter _prettyTextWriter;

   public PrettyTextWriterUima() {
      super();
      _prettyTextWriter = new PrettyTextWriter();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize() throws ResourceInitializationException {
      super.initialize();
      try {
         _prettyTextWriter.setOutputDirectory( (String)getConfigParameterValue( PARAM_OUTPUTDIR ) );
      } catch ( IllegalArgumentException | SecurityException multE ) {
         // thrown if the path specifies a File (not Dir) or by file system access methods
         throw new ResourceInitializationException( multE );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void processCas( final CAS aCAS ) throws AnalysisEngineProcessException {
      JCas jcas;
      try {
         jcas = aCAS.getJCas();
      } catch ( CASException casE ) {
         throw new AnalysisEngineProcessException( casE );
      }
      _prettyTextWriter.process( jcas );
   }

}
