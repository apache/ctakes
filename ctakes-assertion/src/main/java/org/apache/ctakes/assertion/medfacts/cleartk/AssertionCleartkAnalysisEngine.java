/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.assertion.medfacts.cleartk;

import org.apache.commons.io.FilenameUtils;
import org.apache.ctakes.assertion.attributes.features.selection.FeatureSelection;
import org.apache.ctakes.assertion.medfacts.cleartk.extractors.FedaFeatureFunction;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.temporary.assertion.AssertionCuePhraseAnnotation;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CoveredTextExtractor;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.TypePathExtractor;
import org.cleartk.ml.feature.function.FeatureFunctionExtractor;
import org.cleartk.ml.tksvmlight.TreeFeature;

import java.io.File;
import java.net.URI;
import java.util.*;
//import org.chboston.cnlp.ctakes.relationextractor.ae.ModifierExtractorAnnotator;

/**
 * @author swu
 */
public abstract class AssertionCleartkAnalysisEngine extends
                                                     CleartkAnnotator<String> {
   Logger logger = Logger.getLogger( AssertionCleartkAnalysisEngine.class );

   public static final String PARAM_GOLD_VIEW_NAME = "GoldViewName";

   public enum FEATURE_CONFIG {
      NO_SEM, NO_SYN, STK, STK_FRAGS, PTK, PTK_FRAGS, DEP_REGEX, DEP_REGEX_FRAGS, ALL_SYN, VECTORS, NO_TOK
   }

   public static int relationId; // counter for error logging

   // additional parameter for domain adaptation
   public static final String FILE_TO_DOMAIN_MAP = "mapTrainFileToDomain";


   @ConfigurationParameter(
         name = PARAM_GOLD_VIEW_NAME,
         mandatory = false,
         description = "view containing the manual identified annotations (especially EntityMention and EventMention annotations); needed for training" )
   protected String goldViewName;

   public static final String PARAM_PRINT_ERRORS = "PrintErrors";

   @ConfigurationParameter(
         name = PARAM_PRINT_ERRORS,
         mandatory = false,
         description = "Print errors true/false",
         defaultValue = "false" )
   boolean printErrors;

   public static final String PARAM_PROBABILITY_OF_KEEPING_DEFAULT_EXAMPLE = "ProbabilityOfKeepingADefaultExample";

   @ConfigurationParameter(
         name = PARAM_PROBABILITY_OF_KEEPING_DEFAULT_EXAMPLE,
         mandatory = false,
         description = "probability that a default example should be retained for training" )
   protected double probabilityOfKeepingADefaultExample = 1.0;

   public static final String PARAM_PORTION_OF_DATA_TO_USE = "PortionOfDataToUse";
   @ConfigurationParameter(
         name = PARAM_PORTION_OF_DATA_TO_USE,
         mandatory = false,
         description = "How much data to actually use during training (e.g. for building learning curves)"
   )
   protected double portionOfDataToUse = 1.0;

   public static final String PARAM_FEATURE_SELECTION_THRESHOLD = "WhetherToDoFeatureSelection";
   // Accurate name? Actually uses the threshold, right?

   @ConfigurationParameter(
         name = PARAM_FEATURE_SELECTION_THRESHOLD,
         mandatory = false,
         description = "the Chi-squared threshold at which features should be removed" )
   protected Float featureSelectionThreshold = 0f;

   public static final String PARAM_FEATURE_CONFIG = "FEATURE_CONFIG";
   @ConfigurationParameter(
         name = PARAM_FEATURE_CONFIG,
         description = "Feature configuration to use (for experiments)",
         mandatory = false
   )
   protected FEATURE_CONFIG featConfig = FEATURE_CONFIG.ALL_SYN;

   public static final String PARAM_FEATURE_SELECTION_URI = "FeatureSelectionURI";

   @ConfigurationParameter(
         mandatory = false,
         name = PARAM_FEATURE_SELECTION_URI,
         description = "provides a URI where the feature selection data will be written" )
   protected URI featureSelectionURI;

   protected static Random coin = new Random( 0 );

   protected static final String FEATURE_SELECTION_NAME = "SelectNeighborFeatures";

   @ConfigurationParameter(
         name = FILE_TO_DOMAIN_MAP,
         mandatory = false,
         description = "a map of filenames to their respective domains (i.e., directories that contain them)" )
   protected String fileDomainMap;
   protected Map<String, String> fileToDomain = new HashMap<>();

   protected String lastLabel;


   /* DEPRECATED: STW 2013/03/28.  Use DependencyUtility:getNominalHeadNode(jCas,annotation) instead */
//  public ConllDependencyNode findAnnotationHead(JCas jcas, Annotation annotation) {
//		
//	    for (ConllDependencyNode depNode : JCasUtil.selectCovered(jcas, ConllDependencyNode.class, annotation)) {
//	    	
//	    	ConllDependencyNode head = depNode.getHead();
//	    	if (head == null || head.getEnd() <= annotation.getBegin() || head.getBegin() > annotation.getEnd()) {
//	    		// The head is outside the bounds of the annotation, so this node must be the annotation's head
//	    		return depNode;
//	    	}
//	    }
//	    // Can this happen?
//	    return null;
//	}


   //private FeatureExtractor1 tokenFeatureExtractor;
//  protected List<ContextExtractor<IdentifiedAnnotation>> contextFeatureExtractors;
//  protected List<ContextExtractor<BaseToken>> tokenContextFeatureExtractors;
   protected List<CleartkExtractor<IdentifiedAnnotation, BaseToken>> contextFeatureExtractors;
   protected List<CleartkExtractor<IdentifiedAnnotation, BaseToken>> tokenContextFeatureExtractors;
   protected List<CleartkExtractor<IdentifiedAnnotation, BaseToken>> tokenCleartkExtractors;
   protected List<FeatureExtractor1<IdentifiedAnnotation>> entityFeatureExtractors;
   protected List<FeatureExtractor1<IdentifiedAnnotation>> entityTreeExtractors;
   protected CleartkExtractor<IdentifiedAnnotation, BaseToken> cuePhraseInWindowExtractor;


   protected List<FeatureFunctionExtractor<IdentifiedAnnotation>> featureFunctionExtractors = new ArrayList<>();
   protected FedaFeatureFunction ffDomainAdaptor = null;

   protected FeatureSelection<String> featureSelection;

   public abstract void setClassLabel( IdentifiedAnnotation entityMention, Instance<String> instance )
         throws AnalysisEngineProcessException;

   protected abstract void initializeFeatureSelection() throws ResourceInitializationException;
//  public abstract FeatureSelection<String> createFeatureSelection(double threshold);
//  public abstract URI createFeatureSelectionURI(File outputDirectoryName);

   private JCas getAnnotationView( final JCas jCas ) throws AnalysisEngineProcessException {
      if ( this.isTraining() ) {
         try {
            return jCas.getView( this.goldViewName );
         } catch ( CASException e ) {
            throw new AnalysisEngineProcessException( e );
         }
      }
      return jCas;
   }

   @Override
   @SuppressWarnings( "deprecation" )
   public void initialize( UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );

      // Re-process the "directory" string for domains that were used in the data
      if ( null != fileDomainMap ) {
         String[] dirs = fileDomainMap.split( "[;:]" );
         for ( String dir : dirs ) {

            // TODO: normalize dir to real domainId
            String domainId = normalizeToDomain( dir );

            File dataDir = new File( dir );
            if ( dataDir.listFiles() != null ) {
               for ( File f : dataDir.listFiles() ) {
                  fileToDomain.put( FilenameUtils.removeExtension( f.getName() ), domainId );
               }
               //    	System.out.println(trainFiles.toString());
            }
         }
      }

      if ( this.isTraining() && this.goldViewName == null ) {
         throw new IllegalArgumentException( PARAM_GOLD_VIEW_NAME + " must be defined during training" );
      }

      // alias for NGram feature parameters
//    int fromRight = CharacterNGramProliferator.RIGHT_TO_LEFT;

      // a list of feature extractors that require only the token:
      // the stem of the word, the text of the word itself, plus
      // features created from the word text like character ngrams
      this.entityFeatureExtractors = new ArrayList<>();

      // a list of feature extractors that require the token and the sentence
//    this.contextFeatureExtractors = new ArrayList<CleartkExtractor>();

      this.tokenCleartkExtractors = new ArrayList<>();

      CleartkExtractor<IdentifiedAnnotation, BaseToken> tokenExtraction1 =
            new CleartkExtractor<>(
                  BaseToken.class,
//    				new FeatureFunctionExtractor(new CoveredTextExtractor(), new LowerCaseFeatureFunction()),
//            new FeatureFunctionExtractor(new CoveredTextExtractor(), new BrownClusterFeatureFunction()),
                  new CoveredTextExtractor<BaseToken>(),
                  //new CleartkExtractor.Covered(),
                  new CleartkExtractor.LastCovered( 2 ),
                  new CleartkExtractor.Preceding( 5 ),
                  new CleartkExtractor.Following( 4 ),
                  new CleartkExtractor.Bag( new CleartkExtractor.Preceding( 3 ) ),
                  new CleartkExtractor.Bag( new CleartkExtractor.Following( 3 ) ),
                  new CleartkExtractor.Bag( new CleartkExtractor.Preceding( 5 ) ),
                  new CleartkExtractor.Bag( new CleartkExtractor.Following( 5 ) ),
                  new CleartkExtractor.Bag( new CleartkExtractor.Preceding( 10 ) ),
                  new CleartkExtractor.Bag( new CleartkExtractor.Following( 10 ) )
            );

      CleartkExtractor<IdentifiedAnnotation, BaseToken> posExtraction1 =
            new CleartkExtractor<>(
                  BaseToken.class,
                  new TypePathExtractor<>( BaseToken.class, "partOfSpeech" ),
                  new CleartkExtractor.LastCovered( 2 ),
                  new CleartkExtractor.Preceding( 3 ),
                  new CleartkExtractor.Following( 2 )
            );

      this.tokenCleartkExtractors.add( tokenExtraction1 );
//    this.tokenCleartkExtractors.add(posExtraction1);

//    this.contextFeatureExtractors.add(new CleartkExtractor(IdentifiedAnnotation.class,
//        new CoveredTextExtractor(),
//        //new TypePathExtractor(IdentifiedAnnotation.class, "stem"),
//        new Preceding(2),
//        new Following(2)));

      // stab at dependency-based features
      //List<Feature> features = new ArrayList<Feature>();
      //ConllDependencyNode node1 = findAnnotationHead(jCas, arg1);

//    CombinedExtractor1 baseExtractorCuePhraseCategory =
//        new CombinedExtractor1
//          (
//           new CoveredTextExtractor<BaseToken>(),
//           new TypePathExtractor(AssertionCuePhraseAnnotation.class, "cuePhrase"),
//           new TypePathExtractor(AssertionCuePhraseAnnotation.class, "cuePhraseCategory"),
//           new TypePathExtractor(AssertionCuePhraseAnnotation.class, "cuePhraseAssertionFamily")
//          );

      // Commented out by TM because it is never actually used:
/*    
    cuePhraseInWindowExtractor = new CleartkExtractor<>(
        BaseToken.class,
        new CoveredTextExtractor<BaseToken>(),
        new CleartkExtractor.Bag(new CleartkExtractor.Covered())
//          AssertionCuePhraseAnnotation.class,
//          baseExtractorCuePhraseCategory,
//          new CleartkExtractor.Bag(new CleartkExtractor.Preceding(3)),
//          new CleartkExtractor.Bag(new CleartkExtractor.Following(3)),
//          new CleartkExtractor.Bag(new CleartkExtractor.Preceding(5)),
//          new CleartkExtractor.Bag(new CleartkExtractor.Following(5)),
//          new CleartkExtractor.Bag(new CleartkExtractor.Preceding(10)),
//          new CleartkExtractor.Bag(new CleartkExtractor.Following(10))
          );
*/
      if ( !fileToDomain.isEmpty() ) {
         // set up FeatureFunction for all the laggard, non-Extractor features
         ffDomainAdaptor = new FedaFeatureFunction( new ArrayList<>( new HashSet<>( fileToDomain.values() ) ) );
      }
      entityTreeExtractors = new ArrayList<>();
   }

   @Override
   public void process( JCas jCas ) throws AnalysisEngineProcessException {
      logger.info( "Processing ..." );
      String documentId = DocIdUtil.getDocumentID( jCas );
      String domainId = "";
      String domainFeature = null;

      if ( this.featureFunctionExtractors.size() <= 0 ) {
         this.ffDomainAdaptor = null;
      }

      if ( documentId != null ) {
         logger.debug( "processing next doc: " + documentId );
         // set the domain to be FeatureFunction'ed into all extractors
         if ( !fileToDomain.isEmpty() && ffDomainAdaptor != null ) {
            domainId = fileToDomain.get( documentId );
            // if domain is not found, no warning -- just considers general domain
            ffDomainAdaptor.setDomain( domainId );
         } else if ( !fileToDomain.isEmpty() ) {
            domainFeature = fileToDomain.get( documentId );
         }
      } else {
         logger.debug( "processing next doc (doc id is null)" );
      }

      this.lastLabel = "<BEGIN>";

//    // get gold standard relation instances during testing for error analysis
//    if (! this.isTraining() && printErrors) {
//      JCas goldView;
//      try {
//        goldView = jCas.getView("GoldView");
//      } catch(CASException e) {
//        throw new AnalysisEngineProcessException(e);
//      }
//      
//      //categoryLookup = createCategoryLookup(goldView); 
//    }

      final JCas annotationView = getAnnotationView( jCas );

//    Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentenceMap = JCasUtil.indexCovering(annotationView, IdentifiedAnnotation.class, Sentence.class);
//    Map<Sentence, Collection<BaseToken>> tokensCoveredInSentenceMap = JCasUtil.indexCovered(annotationView, Sentence.class, BaseToken.class);

//    Map<IdentifiedAnnotation, Collection<Zone>> coveringZoneMap =
//        JCasUtil.indexCovering(jCas, IdentifiedAnnotation.class, Zone.class);
//    Map<IdentifiedAnnotation, Collection<Sentence>> coveringSents =
//        JCasUtil.indexCovering(jCas, IdentifiedAnnotation.class, Sentence.class);

//    List<Instance<String>> instances = new ArrayList<Instance<String>>();
      // generate a list of training instances for each sentence in the document
      // Use an indexed map.  This is faster than calling select and then selectCovering within a loop.
      final Map<Sentence, List<Annotation>> sentenceAnnotationMap
            = JCasUtil.indexCovered( annotationView, Sentence.class, Annotation.class );
      // Faster than calling JCasUtil methods for each which has to iterate through the full cas each time.
      final Collection<IdentifiedAnnotation> entities = new ArrayList<>();
      final Collection<AssertionCuePhraseAnnotation> cues = new ArrayList<>();
      final Collection<BaseToken> baseTokens = new ArrayList<>();
      for(Sentence coveringSent : JCasUtil.select(annotationView, Sentence.class)){
         Collection<Annotation> coveredAnnotations = sentenceAnnotationMap.get(coveringSent);
         // Sort Annotations into *Mention, assertion cues and BaseTokens in one loop.
         // Faster than calling JCasUtil methods for each which has to iterate through the full cas each time.
         entities.clear();
         cues.clear();
         baseTokens.clear();
         for ( Annotation annotation : coveredAnnotations ) {
            if ( annotation instanceof EventMention || annotation instanceof EntityMention ) {
               entities.add( (IdentifiedAnnotation)annotation );
            } else if ( annotation instanceof AssertionCuePhraseAnnotation ) {
               cues.add( (AssertionCuePhraseAnnotation)annotation );
            } else if ( annotation instanceof BaseToken ) {
               baseTokens.add( (BaseToken)annotation );
            }
         }

         for ( IdentifiedAnnotation identifiedAnnotation : entities ) {
            if ( identifiedAnnotation.getPolarity() == -1 ) {
               logger.debug( String.format( " - identified annotation: [%d-%d] polarity %d (%s)",
                     identifiedAnnotation.getBegin(),
                     identifiedAnnotation.getEnd(),
                     identifiedAnnotation.getPolarity(),
                     identifiedAnnotation.getClass().getName() ) );
            }
            Instance<String> instance = new Instance<>();

            if ( domainFeature != null ) {
               instance.add( new Feature( "Domain", domainFeature ) );
            }
//      // extract all features that require only the entity mention annotation
//      instance.addAll(tokenFeatureExtractor.extract(jCas, entityMention));

            // extract all features that require the token and sentence annotations

            //Sentence sentence = sentenceList.iterator().next();
      
      /*
      if (sentence != null)
      {
        for (ContextExtractor<IdentifiedAnnotation> extractor : this.contextFeatureExtractors) {
          instance.addAll(extractor.extractWithin(annotationView, entityMention, sentence));
        }
      } else
      {
        // TODO extract context features for annotations that don't fall within a sentence
        logger.log(Level.WARN, "FIXME/TODO: generate context features for entities that don't fall within a sentence");
      }
      */
      
      /*
      for (ContextExtractor<BaseToken> extractor : this.tokenContextFeatureExtractors) {
          instance.addAll(extractor.extract(annotationView, entityMention));
        }
        */

            // only use extract this version if not doing domain adaptation
            if ( ffDomainAdaptor == null ) {
               for ( CleartkExtractor<IdentifiedAnnotation, BaseToken> extractor : this.tokenCleartkExtractors ) {
//    		  instance.addAll(extractor.extractWithin(annotationView, entityMention, sentence));
//                  if ( coveringSent != null ) {
                  instance.addAll( extractor
                        .extractWithin( annotationView, identifiedAnnotation, coveringSent ) );
//                  } else {
//                     instance.addAll( extractor.extract( annotationView, identifiedAnnotation ) );
//                  }
               }
            }

            int closest = Integer.MAX_VALUE;
            AssertionCuePhraseAnnotation closestCue = null;
            for ( AssertionCuePhraseAnnotation cue : cues ) {
               // It is much faster to count between BaseTokens already isolated within the same sentence.
               final int betweenCount = countBetween( cue, identifiedAnnotation, baseTokens );
               if ( betweenCount < closest ) {
                  closestCue = cue;
                  closest = betweenCount;
               }

//          instance.addAll(cuePhraseInWindowExtractor.extractBetween(jCas, cue, entityOrEventMention));
            }
            if ( closestCue != null && closest < 21 ) {
               instance.add( new Feature( "ClosestCue_Word", closestCue.getCoveredText() ) );
//          instance.add(new Feature("ClosestCue_Phrase", closestCue.getCuePhrase()));
               instance.add( new Feature( "ClosestCue_PhraseFamily", closestCue.getCuePhraseAssertionFamily() ) );
               instance.add( new Feature( "ClosestCue_PhraseCategory", closestCue.getCuePhraseCategory() ) );

               // add hack-ey domain adaptation to these hacked-in features
               if ( !fileToDomain.isEmpty() && ffDomainAdaptor != null ) {
                  instance.addAll( ffDomainAdaptor
                        .apply( new Feature( "ClosestCue_Word", closestCue.getCoveredText() ) ) );
                  instance.addAll( ffDomainAdaptor
                        .apply( new Feature( "ClosestCue_PhraseFamily", closestCue
                              .getCuePhraseAssertionFamily() ) ) );
                  instance.addAll( ffDomainAdaptor
                        .apply( new Feature( "ClosestCue_PhraseCategory", closestCue.getCuePhraseCategory() ) ) );
               }

            }
//            }
//      if (cuePhraseFeatures != null && !cuePhraseFeatures.isEmpty())
//      {
//        instance.addAll(cuePhraseFeatures);
//      }


            // 7/9/13 SRH trying to make it work just for anatomical site
            int eemTypeId = identifiedAnnotation.getTypeID();
            if ( eemTypeId == CONST.NE_TYPE_ID_ANATOMICAL_SITE ) {
               // 7/9/13 srh modified per tmiller so it's binary but not numeric feature
               //instance.add(new Feature("ENTITY_TYPE_" + entityOrEventMention.getTypeID()));
               instance.add( new Feature( "ENTITY_TYPE_ANAT_SITE" ) );
               // add hack-ey domain adaptation to these hacked-in features
               if ( !fileToDomain.isEmpty() && ffDomainAdaptor != null ) {
                  instance.addAll( ffDomainAdaptor.apply( new Feature( "ENTITY_TYPE_ANAT_SITE" ) ) );
               }
            }
      /* This hurts recall more than it helps precision
      else if (eemTypeId == CONST.NE_TYPE_ID_DRUG) {
    	  // 7/10 adding drug
    	  instance.add(new Feature("ENTITY_TYPE_DRUG"));
      }
      */

            // only extract these features if not doing domain adaptation
            if ( ffDomainAdaptor == null ) {
               for ( FeatureExtractor1<IdentifiedAnnotation> extractor : this.entityFeatureExtractors ) {
                  instance.addAll( extractor.extract( jCas, identifiedAnnotation ) );
               }
            }

            for ( FeatureExtractor1<IdentifiedAnnotation> extractor : this.entityTreeExtractors ) {
               instance.addAll( extractor.extract( jCas, identifiedAnnotation ) );
            }

//      List<Feature> zoneFeatures = extractZoneFeatures(coveringZoneMap, entityOrEventMention);
//      if (zoneFeatures != null && !zoneFeatures.isEmpty())
//      {
//        instance.addAll(zoneFeatures);
//      }

            List<Feature> feats = instance.getFeatures();
//      List<Feature> lcFeats = new ArrayList<Feature>();

            for ( Feature feat : feats ) {
               if ( feat instanceof TreeFeature ||
                    (feat.getName() != null && (feat.getName().startsWith( "TreeFrag" ) ||
                                                feat.getName().startsWith( "WORD" ) ||
                                                feat.getName().startsWith( "NEG" ))) ) {
                  continue;
               }
               if ( feat.getName() != null &&
                    (feat.getName().contains( "_TreeFrag" ) || feat.getName().contains( "_WORD" ) ||
                     feat.getName().contains( "_NEG" )) ) {
                  continue;
               }
               if ( feat.getValue() instanceof String ) {
                  feat.setValue( ((String)feat.getValue()).toLowerCase() );
               }
            }

            if ( !fileToDomain.isEmpty() && ffDomainAdaptor != null ) {
               for ( FeatureFunctionExtractor<IdentifiedAnnotation> extractor : this.featureFunctionExtractors ) {
                  // TODO: extend to the case where the extractors take a different argument besides entityOrEventMention
                  instance.addAll( extractor.extract( jCas, identifiedAnnotation ) );
               }
            }


            // grab the output label
            setClassLabel( identifiedAnnotation, instance );

            if ( this.isTraining() ) {
               // apply feature selection, if necessary
               if ( this.featureSelection != null ) {
                  feats = this.featureSelection.transform( feats );
               }

               // ensures that the (possibly) transformed feats are used
               if ( instance.getOutcome() != null ) {
                  if ( coin.nextDouble() < this.portionOfDataToUse ) {
                     this.dataWriter.write( new Instance<>( instance.getOutcome(), feats ) );
                  }
               }
            }
         }
      }
   }

  /*
  public List<Feature> extractZoneFeatures(Map<IdentifiedAnnotation, Collection<Zone>> coveringZoneMap, IdentifiedAnnotation entityOrEventMention)
  {
    final Collection<Zone> zoneList = coveringZoneMap.get(entityOrEventMention);
    
    if (zoneList == null || zoneList.isEmpty())
    {
      //logger.info("AssertionCleartkAnalysisEngine.extractZoneFeatures() early END (no zones)");
      return new ArrayList<Feature>();
    } else
    {
      logger.debug("AssertionCleartkAnalysisEngine.extractZoneFeatures() found zones and adding zone features");
    }
    
    ArrayList<Feature> featureList = new ArrayList<Feature>();
    for (Zone zone : zoneList)
    {
      Feature currentFeature = new Feature("zone", zone.getLabel());
      logger.debug(String.format("zone: %s", zone.getLabel()));
      logger.debug(String.format("zone feature: %s", currentFeature.toString()));
      featureList.add(currentFeature);
    }
    
    return featureList;
  }
  */

   public static AnalysisEngineDescription getDescription( Object... additionalConfiguration )
         throws ResourceInitializationException {
      AnalysisEngineDescription desc = AnalysisEngineFactory
            .createEngineDescription( AssertionCleartkAnalysisEngine.class );
      if ( additionalConfiguration.length > 0 ) {
         ConfigurationParameterFactory.addConfigurationParameters( desc, additionalConfiguration );
      }
      return desc;
   }

   public Map<String, String> getTrainFileToDomain() {
      return fileToDomain;
   }

   public void setTrainFileToDomain( Map<String, String> trainFileToDomain ) {
      this.fileToDomain = trainFileToDomain;
   }

   /**
    * Looks in the domain string (path) for meaningful corpus names
    *
    * @param dir
    * @return
    */
   public static String normalizeToDomain( String dir ) {
      // TODO: real normalization
      String[] p = dir.split( "/" );
      List<String> parts = new ArrayList<>();
      Collections.addAll( parts, p );
      Collections.reverse( parts );
      for ( String part : parts ) {
         if ( part.toLowerCase().startsWith( "test" ) || part.toLowerCase().startsWith( "train" ) ||
              part.toLowerCase().startsWith( "dev" ) ) {
            continue;
         }
         return part;
      }
      return dir;
   }


   /**
    * @param annotation1 -
    * @param annotation2 -
    * @param baseTokens  baseTokens within window
    * @return number of basetokens that lie between annotation1 and annotation2
    */
   static private int countBetween( final Annotation annotation1,
                                    final Annotation annotation2,
                                    final Collection<BaseToken> baseTokens ) {
      final int lowEnd = Math.min( annotation1.getEnd(), annotation2.getEnd() );
      final int highBegin = Math.max( annotation1.getBegin(), annotation2.getBegin() );
      int between = 0;
      for ( BaseToken baseToken : baseTokens ) {
         if ( lowEnd < baseToken.getBegin() && baseToken.getEnd() < highBegin ) {
            between++;
         }
      }
      return between;
   }

  /*
  public static AnalysisEngineDescription getClassifierDescription(String modelFileName)
      throws ResourceInitializationException {
    return CleartkAnnotatorDescriptionFactory.createCleartkAnnotator(
        AssertionCleartkAnalysisEngine.class,
        AssertionComponents.TYPE_SYSTEM_DESCRIPTION,
        modelFileName);
  }

  public static AnalysisEngineDescription getWriterDescription(String outputDirectory)
      throws ResourceInitializationException {
    AnalysisEngineDescription aed = CleartkAnnotatorDescriptionFactory.createViterbiAnnotator(
        AssertionCleartkAnalysisEngine.class,
        AssertionComponents.TYPE_SYSTEM_DESCRIPTION,
        DefaultMaxentDataWriterFactory.class,
        outputDirectory);
    ConfigurationParameterFactory.addConfigurationParameter(
        aed,
        MaxentDataWriterFactory_ImplBase.PARAM_COMPRESS,
        true);
    return aed;
  }
  */

   // Object.finalize() was deprecated in jdk 9.  Given the manner of this code, this is a -reasonable- replacement.
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      if ( classifier instanceof AutoCloseable ) {
         try {
            ((AutoCloseable)classifier).close();
         } catch ( Exception e ) {
            throw new AnalysisEngineProcessException( e );
         }
      }
   }


}
