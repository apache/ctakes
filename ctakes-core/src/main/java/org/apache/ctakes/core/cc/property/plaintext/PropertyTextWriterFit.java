package org.apache.ctakes.core.cc.property.plaintext;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.CasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import static org.apache.ctakes.core.config.ConfigParameterConstants.DESC_OUTPUTDIR;
import static org.apache.ctakes.core.config.ConfigParameterConstants.PARAM_OUTPUTDIR;

/**
 * Writes Document event and anatomic information to file.
 * This version can be used in the UimaFit style with {@link org.apache.uima.fit.descriptor.ConfigurationParameter}
 * It cannot be used in the old Uima CPE style (e.g. the Uima CPE Gui) as the Uima CPE has problems with Fit Consumers.
 * There is a version that can be used with the CPE GUI:
 * {@link org.apache.ctakes.core.cc.property.plaintext.PropertyTextWriterUima}
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/15/2015
 */
@PipeBitInfo(
      name = "Property Text Writer",
      description = "Writes text files with lists of annotations and properties (POS, Semantic Group, CUI, Negation).",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.SENTENCE,
                       PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class PropertyTextWriterFit extends CasConsumer_ImplBase {

   @ConfigurationParameter(
         name = PARAM_OUTPUTDIR,
         mandatory = false,
         description = DESC_OUTPUTDIR,
         defaultValue = ""
   )
   private String fitOutputDirectoryPath;

//   static private final Logger LOGGER = Logger.getLogger( "PropertyTextWriterFit" );

   // delegate
   final private PropertyTextWriter _propertyTextWriter;

   public PropertyTextWriterFit() {
      super();
      _propertyTextWriter = new PropertyTextWriter();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      super.initialize( uimaContext );
      try {
         if ( fitOutputDirectoryPath != null ) {
            _propertyTextWriter.setOutputDirectory( fitOutputDirectoryPath );
         } else {
            _propertyTextWriter.setOutputDirectory( (String)uimaContext.getConfigParameterValue( PARAM_OUTPUTDIR ) );
         }
      } catch ( IllegalArgumentException | SecurityException multE ) {
         // thrown if the path specifies a File (not Dir) or by file system access methods
         throw new ResourceInitializationException( multE );
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final CAS aCAS ) throws AnalysisEngineProcessException {
      JCas jcas;
      try {
         jcas = aCAS.getJCas();
      } catch ( CASException casE ) {
         throw new AnalysisEngineProcessException( casE );
      }
      _propertyTextWriter.process( jcas );
   }

   /**
    * @return This Cas Consumer as an Analysis Engine
    * @throws org.apache.uima.resource.ResourceInitializationException if anything went wrong
    */
   static public AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
      return createAnnotatorDescription( "" );
   }

   /**
    * @param outputDirectoryPath may be empty or null, in which case the current working directory is used
    * @return This Cas Consumer as an Analysis Engine
    * @throws org.apache.uima.resource.ResourceInitializationException if anything went wrong
    */
   static public AnalysisEngineDescription createAnnotatorDescription( final String outputDirectoryPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( PropertyTextWriterFit.class,
            PARAM_OUTPUTDIR, outputDirectoryPath );
   }

}
