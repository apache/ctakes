package org.apache.ctakes.core.cc.property.plaintext;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.logging.Logger;

import static org.apache.ctakes.core.config.ConfigParameterConstants.PARAM_OUTPUTDIR;

/**
 * Writes Document event and anatomic information to file.
 * This can be used with the old descriptor .xml files and the UIMA CPE Gui.  For a UimaFit PropertyTextWriter, use
 * {@link org.apache.ctakes.core.cc.property.plaintext.PropertyTextWriterFit}
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/15/2015
 */
@PipeBitInfo(
      name = "Property Text Writer (UIMA)",
      description = "Writes text files with lists of annotations and properties (POS, Semantic Group, CUI, Negation).",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.SENTENCE,
                       PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class PropertyTextWriterUima extends CasConsumer_ImplBase {


   static private final Logger LOGGER = Logger.getLogger( "PrettyTextWriterUima" );

   // delegate
   final private PropertyTextWriter _propertyTextWriter;

   public PropertyTextWriterUima() {
      super();
      _propertyTextWriter = new PropertyTextWriter();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize() throws ResourceInitializationException {
      super.initialize();
      try {
         _propertyTextWriter.setOutputDirectory( (String)getConfigParameterValue( PARAM_OUTPUTDIR ) );
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
      _propertyTextWriter.process( jcas );
   }

}
