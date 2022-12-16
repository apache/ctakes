package org.apache.ctakes.coreference.ae;

import org.apache.ctakes.core.ae.NamedEngine;
import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.ListFactory;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.coreference.ae.features.cluster.*;
import org.apache.ctakes.coreference.ae.pairing.cluster.*;
import org.apache.ctakes.coreference.util.ClusterMentionFetcher;
import org.apache.ctakes.coreference.util.MarkableCacheRelationExtractor;
import org.apache.ctakes.coreference.util.MarkableUtilities;
import org.apache.ctakes.coreference.util.ThymeCasOrderer;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation.HashableArguments;
import org.apache.ctakes.typesystem.type.refsem.*;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelationIdentifiedAnnotationRelation;
import org.apache.ctakes.typesystem.type.relation.CoreferenceRelation;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.utils.struct.CounterMap;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.*;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;
import static org.apache.ctakes.coreference.util.ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair;



@PipeBitInfo(
        name = "Coreference (Clusters)",
        description = "Coreference annotator using mention-synchronous paradigm.",
        dependencies = { BASE_TOKEN, SENTENCE, SECTION, IDENTIFIED_ANNOTATION, MARKABLE },
        products = { COREFERENCE_RELATION }
)
public class MentionClusterCoreferenceAnnotator extends CleartkAnnotator<String> implements NamedEngine {
  static private final Logger LOGGER = Logger.getLogger( MentionClusterCoreferenceAnnotator.class.getSimpleName() );

  public static final String NO_RELATION_CATEGORY = "-NONE-";
  public static final String PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE =
          "ProbabilityOfKeepingANegativeExample";
  @ConfigurationParameter(
          name = PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
          mandatory = false,
          description = "probability that a negative example should be retained for training")
  protected double probabilityOfKeepingANegativeExample = 0.5;

  public static final String PARAM_USE_EXISTING_ENCODERS="UseExistingEncoders";
  @ConfigurationParameter(name = PARAM_USE_EXISTING_ENCODERS,
          mandatory=false,
          description = "Whether to use encoders in output directory during data writing; if we are making multiple calls")
  private boolean useExistingEncoders=false;

  public static final String PARAM_SINGLE_DOCUMENT = "SingleDocument";
  @ConfigurationParameter(
          name = PARAM_SINGLE_DOCUMENT,
          mandatory = false,
          description = "Specify that coreferences should be sought for a single document.",
          defaultValue = "true" )
  private boolean singleDocument;

  protected Random coin = new Random(0);

  boolean greedyFirst = true;

  private static DataWriter<String> classDataWriter = null;

  public static AnalysisEngineDescription createDataWriterDescription(
          Class<? extends DataWriter<String>> dataWriterClass,
          File outputDirectory,
          float downsamplingRate) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
            MentionClusterCoreferenceAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING,
            true,
            MentionClusterCoreferenceAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
            downsamplingRate,
            DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
            dataWriterClass,
            DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
            outputDirectory,
            MentionClusterCoreferenceAnnotator.PARAM_SINGLE_DOCUMENT,
            false);
  }

  public static AnalysisEngineDescription createAnnotatorDescription(
          String modelPath) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
            MentionClusterCoreferenceAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING,
            false,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
            modelPath);
  }

  public static AnalysisEngineDescription createMultidocAnnotatorDescription(
          String modelPath) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
            MentionClusterCoreferenceAnnotator.class,
            CleartkAnnotator.PARAM_IS_TRAINING,
            false,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
            modelPath,
            MentionClusterCoreferenceAnnotator.PARAM_SINGLE_DOCUMENT,
            false);
  }

  private List<RelationFeaturesExtractor<CollectionTextRelation,IdentifiedAnnotation>> relationExtractors = this.getFeatureExtractors();
  private List<FeatureExtractor1<Markable>> mentionExtractors = this.getMentionExtractors();
  private List<ClusterMentionPairer_ImplBase> pairExtractors = this.getPairExtractors();

//  private Set<String> markableStrings = null;

   /**
    * @return the simple name f this class or that name with "_Training" appending if the annotator is for training.
    */
   @Override
   public String getEngineName() {
      final String simpleName = getClass().getSimpleName();
      if ( isTraining() ) {
         return simpleName + "_Training";
      }
      return simpleName;
   }

  protected List<RelationFeaturesExtractor<CollectionTextRelation,IdentifiedAnnotation>> getFeatureExtractors() {
    List<RelationFeaturesExtractor<CollectionTextRelation,IdentifiedAnnotation>> extractors = new ArrayList<>();
    extractors.add(new MentionClusterAgreementFeaturesExtractor());
    extractors.add(new MentionClusterStringFeaturesExtractor());
    extractors.add(new MentionClusterSectionFeaturesExtractor());
    extractors.add(new MentionClusterUMLSFeatureExtractor());
    extractors.add(new MentionClusterDepHeadExtractor());
    extractors.add(new MentionClusterStackFeaturesExtractor());
    extractors.add(new MentionClusterSalienceFeaturesExtractor());
    extractors.add(new MentionClusterAttributeFeaturesExtractor());
//    extractors.add(new MentionClusterAttributeVectorExtractor()); // does nothing yet

//    extractors.add(new MentionClusterDistanceFeaturesExtractor());

    try {
//      extractors.add(new MentionClusterDistSemExtractor("org/apache/ctakes/coreference/distsem/mimic_vectors.txt"));
//      extractors.add(new MentionClusterDistSemExtractor("org/apache/ctakes/coreference/distsem/deps.words"));
      extractors.add(new MentionClusterSemTypeDepPrefsFeatureExtractor());
    } catch (IOException e) {
      e.printStackTrace();
    }

    return extractors;
  }

  protected List<FeatureExtractor1<Markable>> getMentionExtractors(){
    List<FeatureExtractor1<Markable>> extractors = new ArrayList<>();
    // mention features from pairwise system:
    extractors.add(new MentionClusterAgreementFeaturesExtractor());
    extractors.add(new MentionClusterSectionFeaturesExtractor());
    extractors.add(new MentionClusterUMLSFeatureExtractor());
    extractors.add(new MentionClusterDepHeadExtractor());
    extractors.add(new MentionClusterSalienceFeaturesExtractor());

//    try{
//      extractors.add(new MentionClusterMentionFeaturesExtractor("org/apache/ctakes/coreference/distsem/ties1mil.lowercase.txt"));
//    }catch(CleartkExtractorException e){
//      e.printStackTrace();
//    }
    extractors.add(new MentionClusterAttributeFeaturesExtractor());

    return extractors;
  }

  protected List<ClusterMentionPairer_ImplBase> getPairExtractors(){
    List<ClusterMentionPairer_ImplBase> pairers = new ArrayList<>();
    int sentDist = 5;
    pairers.add(new SentenceDistancePairer(sentDist));
    pairers.add(new SectionHeaderPairer(sentDist));
    pairers.add(new ClusterPairer(Integer.MAX_VALUE));
    pairers.add(new HeadwordPairer());
    pairers.add(new PreviousDocumentPairer());
    return pairers;
  }

  protected Iterable<CollectionTextRelationIdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
          JCas jcas,
          Markable mention,
          JCas prevCas){
    LinkedHashSet<CollectionTextRelationIdentifiedAnnotationPair> pairs = new LinkedHashSet<>();
    for(ClusterMentionPairer_ImplBase pairer : this.pairExtractors){
      if(prevCas != null && pairer instanceof CrossDocumentPairer_ImplBase){
        pairs.addAll(((CrossDocumentPairer_ImplBase)pairer).getPairs(jcas, mention, prevCas));
      }else {
        pairs.addAll(pairer.getPairs(jcas, mention));
      }
    }

    return pairs;
  }

  private void resetPairers(JCas jcas, Map<Markable,ConllDependencyNode> cache){
    for(ClusterMentionPairer_ImplBase pairer : this.pairExtractors){
      pairer.reset(jcas);
      pairer.setCache(cache);
    }
  }

  @Override
  public void initialize( final UimaContext context ) throws ResourceInitializationException {
    LOGGER.info( "Initializing ..." );
    super.initialize( context );

    if ( this.useExistingEncoders && classDataWriter != null ) {
      this.dataWriter = classDataWriter;
    } else if ( this.isTraining() ) {
      classDataWriter = this.dataWriter;
    }
    LOGGER.info( "Finished." );
  }

  public void process( final JCas jCas ) throws AnalysisEngineProcessException {
    //this.dataWriter.write(new Instance<String>("#DEBUG " + ViewUriUtil.getURI(docCas)));
    LOGGER.info( "Finding Coreferences ..." );
    Map<CollectionTextRelationIdentifiedAnnotationPair, CollectionTextRelationIdentifiedAnnotationRelation>
            relationLookup;
    // It is possible that the cas for an entire patient has been passed through.  Try to process all documents.
    final Collection<JCas> views = PatientViewUtil.getDocumentViews( jCas );
    if ( views.isEmpty() ) {
      // There is only one document in the cas - the default
      if ( this.isTraining() ) {
        relationLookup = ClusterMentionFetcher.getPairRelationsForDocument( jCas );
      } else {
        relationLookup = new HashMap<>();
      }
      processDocument( jCas, null, relationLookup );
      removeSingletonClusters( jCas );
      LOGGER.info( "Finished." );
      return;
    }
    // If we get this far then we have multiple views, so we are processing a patient CAS.
    JCas prevView = null;
    if ( this.isTraining() ) {
      relationLookup = ClusterMentionFetcher.getPairRelationsForPatient( jCas );
    } else {
      relationLookup = new HashMap<>();
    }
    try ( DotLogger dotter = new DotLogger() ) {
      for ( JCas view : ThymeCasOrderer.getOrderedCases(jCas) ) {
        LOGGER.info("Processing document with view name: " + view.getViewName());
        processDocument( view, prevView, relationLookup );
        prevView = view;
      }
      for ( JCas view : ThymeCasOrderer.getOrderedCases(jCas) ) {
        removeSingletonClusters(view);
      }
    } catch ( IOException ioE ) {
      LOGGER.error( ioE.getMessage() );
    }
    LOGGER.info( "Finished." );
  }

  private void processDocument( final JCas jCas, final JCas prevCas, final Map<CollectionTextRelationIdentifiedAnnotationPair, CollectionTextRelationIdentifiedAnnotationRelation>
          relationLookup) throws AnalysisEngineProcessException {
    // lookup from pair of annotations to binary text relation
    // note: assumes that there will be at most one relation per pair

    Map<Markable,ConllDependencyNode> depHeadMap = new HashMap<>();
    for(Markable m: JCasUtil.select(jCas, Markable.class)){
      ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jCas, m);
      depHeadMap.put(m, headNode);
    }
    for(RelationFeaturesExtractor featEx : this.relationExtractors){
      if(featEx instanceof MarkableCacheRelationExtractor){
        ((MarkableCacheRelationExtractor)featEx).setCache(depHeadMap);
      }
    }
    for(FeatureExtractor1 featEx : this.mentionExtractors){
      if(featEx instanceof MarkableCacheRelationExtractor){
        ((MarkableCacheRelationExtractor)featEx).setCache(depHeadMap);
      }
    }
    this.resetPairers( jCas, depHeadMap );

    final Map<Segment, Collection<Markable>> segmentMarkables = JCasUtil.indexCovered( jCas, Segment.class, Markable.class );
    for ( Segment segment : JCasUtil.select(jCas, Segment.class) ) {
      for ( Markable mention : segmentMarkables.get(segment) ) {
//        System.out.println( "MCCA Markable: " + mention.getCoveredText() + " :" + mention.getBegin() + "," + mention.getEnd() );
        //        ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jCas, mention);
        boolean singleton = true;
        double maxScore = 0.0;
        CollectionTextRelation maxCluster = null;
        String mentionView = mention.getView().getViewName();

        for ( CollectionTextRelationIdentifiedAnnotationPair pair : this.getCandidateRelationArgumentPairs( jCas, mention, prevCas ) ) {
          CollectionTextRelation cluster = pair.getCluster();
          Markable firstElement = JCasUtil.select(cluster.getMembers(), Markable.class).iterator().next();
          String clusterHeadView = firstElement.getView().getViewName();
//          System.out.println( "   MCCA Pair Cluster: " + pair.getCluster().getCategory() );
//          System.out.println("MCCA Cluster head: " + firstElement.getCoveredText() + " :" + firstElement.getBegin() + "," + firstElement.getEnd());
          // apply all the feature extractors to extract the list of features
          List<Feature> features = new ArrayList<>();
          for ( RelationFeaturesExtractor<CollectionTextRelation, IdentifiedAnnotation> extractor : this.relationExtractors ) {
            List<Feature> feats = extractor.extract( jCas, cluster, mention );
            if ( feats != null ) {
              //              Logger.getRootLogger().info(String.format("For cluster with %d mentions, %d %s features", JCasUtil.select(cluster.getMembers(), Markable.class).size(), feats.size(), extractor.getClass().getSimpleName()));
              features.addAll( feats );
//System.out.println( "      MCCA Extract: " + extractor.getClass().getSimpleName() + "   Features:");
//feats.forEach( f -> System.out.println( "         " + f.toString() ) );
            }
          }

          for ( FeatureExtractor1<Markable> extractor : this.mentionExtractors ) {
            features.addAll( extractor.extract( jCas, mention ) );
          }

          // here is where feature conjunctions can go (dupFeatures)
          List<Feature> dupFeatures = new ArrayList<>();
          if(!mentionView.equals(clusterHeadView)){
            features.add(new Feature("IsCrossDoc", true));
            for( Feature feature : features ){
              dupFeatures.add(new Feature("CrossDoc_" + feature.getName(), feature.getValue()));
            }
          }
          features.addAll( dupFeatures );
          // sanity check on feature values
          for ( Feature feature : features ) {
            if ( feature.getValue() == null ) {
              feature.setValue( "NULL" );
              String message = String.format( "Null value found in %s from %s", feature, features );
              System.err.println( message );
            }
          }


          // during training, feed the features to the data writer
          if ( this.isTraining() ) {
            String category = this.getRelationCategory( relationLookup, cluster, mention );
            if ( category == null ) {
              continue;
            }

            // create a classification instance and write it to the training data
            this.dataWriter.write( new Instance<>( category, features ) );
            if ( !category.equals( NO_RELATION_CATEGORY ) ) {
//              LOGGER.warn("Coref training: Writing link between mention: " + mention.getCoveredText() + " and previous cluster containing mention: " + firstElement.getCoveredText());
              if(!clusterHeadView.equals(mentionView)){
                LOGGER.info("Writing positive instance linking mention [" + mention.getCoveredText() + "] to cluster with elements from previous document");
              }
              singleton = false;
              break;
            }
          }

          // during classification feed the features to the classifier and create
          // annotations
          else {
            if(!clusterHeadView.equals(mentionView)){
              LOGGER.info("Comparing new mention to cluster with elements from previous document");
            }
            String predictedCategory = this.classify( features );
//System.out.println( "      MCCA Predicted Category: " + predictedCategory + "    Scores:" );
            // TODO look at scores in classifier and try best-pair rather than first-pair?
            Map<String, Double> scores = this.classifier.score( features );
//scores.forEach( (k,v) -> System.out.println( "         " + k + " = " + v ) );
            // add a relation annotation if a true relation was predicted
            if ( !predictedCategory.equals( NO_RELATION_CATEGORY ) ) {
              //              Logger.getLogger("MCAnnotator").info(String.format("Making a pair with score %f", scores.get(predictedCategory)));
              if ( greedyFirst ) {
                createRelation( jCas, cluster, mention, predictedCategory, scores.get( predictedCategory ) );
                singleton = false;
                if(!clusterHeadView.equals(mentionView)){
                  LOGGER.info("Linking new mention to cluster with elements from previous document");
                }
                // break here for "closest-first" greedy decoding strategy (Soon et al., 2001), terminology from Lasalle and Denis (2013),
                // for "best first" need to keep track of all relations with scores and only keep the highest
                break;
              }
              if ( scores.get( predictedCategory ) > maxScore ) {
                maxScore = scores.get( predictedCategory );
                maxCluster = cluster;
              }
            }
          }
        }
        if ( !this.isTraining() && !greedyFirst && maxCluster != null ) {
          // make a link with the max cluster
          createRelation( jCas, maxCluster, mention, "CoreferenceClusterMember", maxScore );
        }

        // if we got this far and never matched up the markable then add it to list.
        // do this even during training -- adds non-chain markables to antecedent list which will be seen during testing.
        if ( singleton ) {
          // make the markable it's own cluster:
          CollectionTextRelation chain = new CollectionTextRelation( jCas );
          chain.setCategory( "Identity" );
          NonEmptyFSList list = new NonEmptyFSList( jCas );
          list.setHead( mention );
          list.setTail( new EmptyFSList( jCas ) );
          chain.setMembers( list );
          chain.addToIndexes();
          list.addToIndexes();
          list.getTail().addToIndexes();
        }
      }
    }
    createEventClusters( jCas );
  }


  /**
   * Looks up the arguments in the specified lookup table and converts the
   * relation into a label for classification
   *
   * @return If this category should not be processed for training return
   *         <i>null</i> otherwise it returns the label sent to the datawriter
   */
  protected String getRelationCategory(
          Map<CollectionTextRelationIdentifiedAnnotationPair, CollectionTextRelationIdentifiedAnnotationRelation> relationLookup,
          CollectionTextRelation cluster,
          IdentifiedAnnotation mention) {
    CollectionTextRelationIdentifiedAnnotationRelation relation =
            relationLookup.get(new CollectionTextRelationIdentifiedAnnotationPair(cluster, mention));
    String category;
    if (relation != null) {
      category = relation.getCategory();
    } else if (coin.nextDouble() <= this.probabilityOfKeepingANegativeExample) {
      category = NO_RELATION_CATEGORY;
    } else {
      category = null;
    }
    return category;
  }

  /**
   * Predict an outcome given a set of features. By default, this simply
   * delegates to the object's <code>classifier</code>. Subclasses may override
   * this method to implement more complex classification procedures.
   *
   * @param features
   *          The features to be classified.
   * @return The predicted outcome (label) for the features.
   */
  protected String classify(List<Feature> features) throws CleartkProcessingException {
    return this.classifier.classify(features);
  }

  /**
   * Create a UIMA relation type based on arguments and the relation label. This
   * allows subclasses to create/define their own types: e.g. coreference can
   * create CoreferenceRelation instead of BinaryTextRelation
   *
   * @param jCas
   *          - JCas object, needed to create new UIMA types
  //   * @param arg1
  //   *          - First argument to relation
  //   * @param arg2
  //   *          - Second argument to relation
   * @param predictedCategory
   *          - Name of relation
   */
  protected void createRelation(
          JCas jCas,
          CollectionTextRelation cluster,
          IdentifiedAnnotation mention,
          String predictedCategory,
          Double confidence) {
    // add the relation to the CAS
    CollectionTextRelationIdentifiedAnnotationRelation relation = new CollectionTextRelationIdentifiedAnnotationRelation(jCas);
    relation.setCluster(cluster);
    relation.setMention(mention);
    relation.setCategory(predictedCategory);
    relation.setConfidence(confidence);
    relation.addToIndexes();

//    RelationArgument arg = new RelationArgument(jCas);
//    arg.setArgument(mention);
    ListFactory.append(jCas, cluster.getMembers(), mention);
  }

  /**
   * Create the set of Event types for every chain we found in the document.
   * Event is a non-Annotation type (i.e., no span) that has its own attributes
   * but points to an FSArray of mentions which each have their own attributes.
   *
   * @param jCas
   *        - JCas object, needed to create UIMA types
   * @throws AnalysisEngineProcessException
   */
  private static void createEventClusters(JCas jCas) throws AnalysisEngineProcessException{
    // First, find the largest span identified annotation that shares a headword with the markable
    // do that by finding the head of the markable, then finding the identifiedannotations that cover it:

    Map<Markable, List<IdentifiedAnnotation>> markable2annotations = MarkableUtilities.indexCoveringUmlsAnnotations(jCas);
    for(CollectionTextRelation cluster : JCasUtil.select(jCas, CollectionTextRelation.class)){
      CounterMap<Class<? extends IdentifiedAnnotation>> headCounts = new CounterMap<>();
      List<Markable> memberList = new ArrayList<>(JCasUtil.select(cluster.getMembers(), Markable.class));
      for(Markable member : memberList){
        // Now find the largest covering annotation:
        IdentifiedAnnotation largest = null;
        for(IdentifiedAnnotation covering : markable2annotations.get(member)){
          if(largest == null || (covering.getEnd()-covering.getBegin() > (largest.getEnd()-largest.getBegin()))){
            largest = covering;
          }
        }
        if(largest != null){
          headCounts.add(largest.getClass());
        }
      }
      FSArray mentions = new FSArray(jCas, memberList.size());
      IntStream.range(0, memberList.size()).forEach(i -> mentions.set(i, memberList.get(i)));

      Element element = null;
      if(headCounts.size() == 0){
        element = new Event(jCas);
      }else{
        Class<? extends IdentifiedAnnotation> mostCommon = headCounts.entrySet().stream()
                .sorted(Map.Entry.<Class<? extends IdentifiedAnnotation>,Integer>comparingByValue().reversed())
                .limit(1)
                .map(f -> f.getKey())
                .collect(Collectors.toList()).get(0);
        if(mostCommon.equals(DiseaseDisorderMention.class)){
          element = new DiseaseDisorder(jCas);
        }else if(mostCommon.equals(ProcedureMention.class)){
          element = new Procedure(jCas);
        }else if(mostCommon.equals(SignSymptomMention.class)){
          element = new SignSymptom(jCas);
        }else if(mostCommon.equals(MedicationMention.class)){
          element = new Medication(jCas);
        }else if(mostCommon.equals(AnatomicalSiteMention.class)){
          element = new AnatomicalSite(jCas);
        }else{
          System.err.println("This coreference chain has an unknown type: " + mostCommon.getSimpleName());
          throw new AnalysisEngineProcessException();
        }
      }
      element.setMentions(mentions);
      element.addToIndexes();
    }
  }

  private static void removeSingletonClusters(JCas jcas){
    List<CollectionTextRelation> toRemove = new ArrayList<>();
    for(CollectionTextRelation rel : JCasUtil.select(jcas, CollectionTextRelation.class)){
      NonEmptyFSList head = (NonEmptyFSList) rel.getMembers();
      if(head.getTail() instanceof EmptyFSList){
        toRemove.add(rel);
      }
    }

    for(CollectionTextRelation rel : toRemove){
      rel.removeFromIndexes();
    }
  }


  public Map<HashableArguments, Double> getMarkablePairScores(JCas jCas){
    Map<HashableArguments, Double> scoreMap = new HashMap<>();
    for(CoreferenceRelation reln : JCasUtil.select(jCas, CoreferenceRelation.class)){
      HashableArguments pair = new HashableArguments(reln.getArg1().getArgument(), reln.getArg2().getArgument());
      scoreMap.put(pair, reln.getConfidence());
    }
    return scoreMap;
  }

}
