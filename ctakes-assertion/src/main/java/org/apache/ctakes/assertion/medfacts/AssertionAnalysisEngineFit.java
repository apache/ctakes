package org.apache.ctakes.assertion.medfacts;

import org.apache.ctakes.assertion.medfacts.i2b2.api.CharacterOffsetToLineTokenConverterCtakesImpl;
import org.apache.ctakes.assertion.medfacts.i2b2.api.SingleDocumentProcessorCtakes;
import org.apache.ctakes.assertion.medfacts.types.Concept;
import org.apache.ctakes.assertion.stub.*;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static org.apache.ctakes.typesystem.type.constants.CONST.ATTR_SUBJECT_FAMILY_MEMBER;
import static org.apache.ctakes.typesystem.type.constants.CONST.ATTR_SUBJECT_PATIENT;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/19/2016
 */
@PipeBitInfo(
      name = "Assertion Engine (FIT)",
      description = "Adds Negation, Uncertainty, Conditional and Subject to annotations.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class AssertionAnalysisEngineFit extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "AssertionAnalysisEngineFit" );

   static public final String ASSERTION_MODEL_PARAM = "assertionModelResource";
   static public final String SCOPE_MODEL_PARAM = "scopeModelResource";
   static public final String CUE_MODEL_PARAM = "cueModelResource";
   static public final String POS_MODEL_PARAM = "posModelResource";
   static public final String ENABLED_FEATURES_PARAM = "enabledFeaturesResource";


   @ConfigurationParameter(
         name = ASSERTION_MODEL_PARAM,
         defaultValue = "org/apache/ctakes/assertion/models/i2b2.model" )
   private String _assertionModelPath;

   @ConfigurationParameter(
         name = SCOPE_MODEL_PARAM,
         defaultValue = "org/apache/ctakes/assertion/models/scope.model" )
   private String _scopeModelPath;

   @ConfigurationParameter(
         name = CUE_MODEL_PARAM,
         defaultValue = "org/apache/ctakes/assertion/models/cue.model" )
   private String _cueModelPath;

   @ConfigurationParameter(
         name = POS_MODEL_PARAM,
         defaultValue = "org/apache/ctakes/assertion/models/pos.model" )
   private String _posModelPath;

   @ConfigurationParameter(
         name = ENABLED_FEATURES_PARAM,
         defaultValue = "org/apache/ctakes/assertion/models/featureFile11b" )
   private String _enabledFeaturesPath;

   private AssertionDecoderConfiguration _assertionDecoderConfiguration;

   @Override
   public void initialize( final UimaContext uimaContext ) throws ResourceInitializationException {
      super.initialize( uimaContext );
      // byte assertionModelContents[];
      File assertionModelFile;
      String scopeModelFilePath;
      String cueModelFilePath;
      String posModelFilePath;
      File enabledFeaturesFile;
      try {
         assertionModelFile = FileLocator.getFile( _assertionModelPath );
         // assertionModelContents = StringHandling.readEntireContentsBinary(assertionModelFile);
         scopeModelFilePath = FileLocator.getFile( _scopeModelPath ).getAbsolutePath();
         cueModelFilePath = FileLocator.getFile( _cueModelPath ).getAbsolutePath();
         posModelFilePath = FileLocator.getFile( _posModelPath ).getAbsolutePath();
         enabledFeaturesFile = FileLocator.getFile( _enabledFeaturesPath );
      } catch ( FileNotFoundException fnfE ) {
         throw new ResourceInitializationException( fnfE );
      }
      LOGGER.info( "scope model file: " + scopeModelFilePath );
      LOGGER.info( "cue model file: " + cueModelFilePath );
      LOGGER.info( "pos model file: " + posModelFilePath );
      final AssertionDecoderConfiguration assertionDecoderConfiguration = new AssertionDecoderConfiguration();
      final ScopeParser scopeParser = new ScopeParser( scopeModelFilePath, cueModelFilePath );
      assertionDecoderConfiguration.setScopeParser( scopeParser );
      final PartOfSpeechTagger posTagger = new PartOfSpeechTagger( posModelFilePath );
      assertionDecoderConfiguration.setPosTagger( posTagger );
      final Set<String> enabledFeatureIdSet = BatchRunner.loadEnabledFeaturesFromFile( enabledFeaturesFile );
      assertionDecoderConfiguration.setEnabledFeatureIdSet( enabledFeatureIdSet );
      final JarafeMEDecoder assertionDecoder = new JarafeMEDecoder( assertionModelFile );
      assertionDecoderConfiguration.setAssertionDecoder( assertionDecoder );
      _assertionDecoderConfiguration = assertionDecoderConfiguration;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Starting processing" );
      final String documentText = jcas.getDocumentText();
      final List<ApiConcept> apiConceptList = new ArrayList<>();
      final Collection<Concept> concepts = JCasUtil.select( jcas, Concept.class );
      for ( Concept concept : concepts ) {
         final int begin = concept.getBegin();
         final int end = concept.getEnd();
         final String conceptText = documentText.substring( begin, end );
         final ApiConcept apiConcept = new ApiConcept();
         apiConcept.setBegin( begin );
         apiConcept.setEnd( end );
         apiConcept.setText( conceptText );
         apiConcept.setType( concept.getConceptType() );
         apiConcept.setExternalId( concept.getAddress() );
         apiConceptList.add( apiConcept );
      }
      final SingleDocumentProcessorCtakes processor = new SingleDocumentProcessorCtakes();
      processor.setJcas( jcas );
      processor.setAssertionDecoderConfiguration( _assertionDecoderConfiguration );
      processor.setContents( documentText );
      final CharacterOffsetToLineTokenConverter converter = new CharacterOffsetToLineTokenConverterCtakesImpl( jcas );
      processor.setConverter2( converter );
      apiConceptList.forEach( processor::addConcept );
      LOGGER.debug( "BEFORE CALLING processor.processSingleDocument()" );
      processor.processSingleDocument();
      LOGGER.debug( "AFTER CALLING processor.processSingleDocument()" );
      final Map<Integer, String> assertionTypeMap = processor.getAssertionTypeMap();
      final CasIndexer<Annotation> indexer = new CasIndexer<>( jcas, null );
      for ( Map.Entry<Integer, String> current : assertionTypeMap.entrySet() ) {
         final Integer currentIndex = current.getKey();
         final String currentAssertionType = current.getValue();
         final ApiConcept originalConcept = apiConceptList.get( currentIndex );
         final Concept associatedConcept = (Concept)indexer.lookupByAddress( originalConcept.getExternalId() );
         final int entityAddress = associatedConcept.getOriginalEntityExternalId();
         final IdentifiedAnnotation annotation = (IdentifiedAnnotation)indexer.lookupByAddress( entityAddress );
         mapI2B2AssertionValueToCtakes( currentAssertionType, annotation );
      }
      LOGGER.info( "Processing Finished" );
   }


   static private void fillProperties( final IdentifiedAnnotation annotation,
                                       final int polarity, final int uncertainty,
                                       final boolean generic, final boolean conditional,
                                       final String subject, final float confidence ) {
      annotation.setPolarity( polarity );
      annotation.setUncertainty( uncertainty );
      annotation.setGeneric( generic );
      annotation.setConditional( conditional );
      annotation.setSubject( subject );
      annotation.setConfidence( confidence );
   }


   // possible values for currentAssertionType:
   // present
   // absent
   // associated_with_someone_else
   // conditional
   // hypothetical
   // possible
   // Changed from original implementation by information in https://www.mitre.org/sites/default/files/pdf/10_4676.pdf
   static private void mapI2B2AssertionValueToCtakes( final String assertionType,
                                                      final IdentifiedAnnotation annotation )
         throws AnalysisEngineProcessException {
      if ( assertionType == null ) {
         LOGGER.error( "current assertion type is null" );
         fillProperties( annotation, -2, -2, false, false, "skipped", -2.0f );
         return;
      }
      switch ( assertionType ) {
         case "present":
            fillProperties( annotation, 1, 0, false, false, ATTR_SUBJECT_PATIENT, 1.0f );
            break;
         case "absent":
            fillProperties( annotation, -1, 0, false, false, ATTR_SUBJECT_PATIENT, 1.0f );
            break;
         case "associated_with_someone_else":
            // OLD:   annotation.setSubject( "CONST.ATTR_SUBJECT_FAMILY_MEMBER" );
            fillProperties( annotation, 1, 0, false, false, ATTR_SUBJECT_FAMILY_MEMBER, 1.0f );
            break;
         case "conditional":
            // OLD:   currently no mapping to sharp type...all sharp properties are defaults!
            // OLD:   annotation.setConditional( false );
            fillProperties( annotation, 1, 0, false, true, ATTR_SUBJECT_PATIENT, 1.0f );
            break;
         case "hypothetical":
            // OLD:   annotation.setConditional( true ); annotation.setGeneric( false );
            fillProperties( annotation, 1, 0, true, false, ATTR_SUBJECT_PATIENT, 1.0f );
            break;
         case "possible":
            fillProperties( annotation, 1, 1, false, false, ATTR_SUBJECT_PATIENT, 1.0f );
            break;
         default:
            LOGGER.error( "unexpected assertion value returned: " + assertionType );
            fillProperties( annotation, -2, -2, false, false, "skipped", -2.0f );
            break;
      }
   }

}
