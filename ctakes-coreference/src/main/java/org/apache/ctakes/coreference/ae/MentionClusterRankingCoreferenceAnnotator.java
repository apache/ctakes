package org.apache.ctakes.coreference.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.ListFactory;
import org.apache.ctakes.coreference.ae.features.cluster.*;
import org.apache.ctakes.coreference.util.ClusterMentionFetcher;
import org.apache.ctakes.coreference.util.ClusterUtils;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation.HashableArguments;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelationIdentifiedAnnotationRelation;
import org.apache.ctakes.typesystem.type.relation.CoreferenceRelation;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textsem.MedicationEventMention;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.svmlight.rank.QidInstance;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;
import static org.apache.ctakes.coreference.util.ClusterMentionFetcher.CollectionTextRelationIdentifiedAnnotationPair;

// TODO Consolidate all of the duplicate code in the coref module

@PipeBitInfo(
      name = "Coreference (Cluster Rank)",
      description = "Coreference annotator using mention-synchronous paradigm.",
      dependencies = { BASE_TOKEN, SENTENCE, SECTION, PARAGRAPH, IDENTIFIED_ANNOTATION, MARKABLE },
      products = { COREFERENCE_RELATION }
)
public class MentionClusterRankingCoreferenceAnnotator extends CleartkAnnotator<Double> {
  public static final String NO_RELATION_CATEGORY = "-NONE-";
  public static final String CLUSTER_RELATION_CATEGORY = "CoreferenceClusterMember";
  
  public static final String PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE =
      "ProbabilityOfKeepingANegativeExample";
  @ConfigurationParameter(
      name = PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
      mandatory = false,
      description = "probability that a negative example should be retained for training")
  protected double probabilityOfKeepingANegativeExample = 0.5;

  protected Random coin = new Random(0);

  boolean greedyFirst = true;
  
  private int qid = 0;
  
  public static AnalysisEngineDescription createDataWriterDescription(
      Class<? extends DataWriter<?>> dataWriterClass,
      File outputDirectory,
      float downsamplingRate) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        MentionClusterRankingCoreferenceAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        true,
        MentionClusterRankingCoreferenceAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
        downsamplingRate,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        dataWriterClass,
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outputDirectory);
  }

  public static AnalysisEngineDescription createAnnotatorDescription(
      String modelPath) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        MentionClusterRankingCoreferenceAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        false,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        modelPath);
  }

  private List<RelationFeaturesExtractor<CollectionTextRelation,IdentifiedAnnotation>> relationExtractors = this.getFeatureExtractors();
  private List<FeatureExtractor1<Markable>> mentionExtractors = this.getMentionExtractors();
  
  private Set<String> markableStrings = null;
  private Map<ConllDependencyNode,Collection<IdentifiedAnnotation>> nodeEntMap = null;
  private Map<String,Set<Markable>> headWordMarkables = null;
  private Map<HashableArguments,Double> pairScores = null;
  
  protected List<RelationFeaturesExtractor<CollectionTextRelation,IdentifiedAnnotation>> getFeatureExtractors() {
    List<RelationFeaturesExtractor<CollectionTextRelation,IdentifiedAnnotation>> extractors = new ArrayList<>();
    extractors.add(new MentionClusterAgreementFeaturesExtractor());
    extractors.add(new MentionClusterStringFeaturesExtractor());
    extractors.add(new MentionClusterSectionFeaturesExtractor());
    extractors.add(new MentionClusterUMLSFeatureExtractor());
    extractors.add(new MentionClusterDepHeadExtractor());
    extractors.add(new MentionClusterStackFeaturesExtractor());
    extractors.add(new MentionClusterSalienceFeaturesExtractor());
//    extractors.add(new MentionClusterDistanceFeaturesExtractor());
    extractors.add(new MentionClusterAttributeFeaturesExtractor());
    
    try {
      extractors.add(new MentionClusterDistSemExtractor());
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

    try {
      extractors.add(new MentionClusterMentionFeaturesExtractor());
    } catch (CleartkExtractorException e) {
      e.printStackTrace();
    }
    extractors.add(new MentionClusterAttributeFeaturesExtractor());

    return extractors;
  }
  
  protected Iterable<CollectionTextRelationIdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
      JCas jcas,
      IdentifiedAnnotation mention){
    int sentDist = 5;
    // using linked hash set ensures no duplicates:
    LinkedHashSet<CollectionTextRelationIdentifiedAnnotationPair> pairs = new LinkedHashSet<>();
    pairs.addAll(getSentenceDistancePairs(jcas, mention, sentDist));
    pairs.addAll(getSectionHeaderPairs(jcas, mention, sentDist));
    pairs.addAll(getClusterPairs(jcas, mention, Integer.MAX_VALUE));
    pairs.addAll(getHeadwordMatchPairs(jcas, mention, sentDist));
    
    return pairs;
  }
  
  /*
   * getExactStringMatchPairs()
   * For mentions that have the exact string repeated elsewhere in the document we want to
   * allow matching across any distance. We don't use the sentence distance parameter here.
   * We make use of a global variable markableStrings that is a HashSet containig all the markable
   * strings from this document.
   */
  private List<CollectionTextRelationIdentifiedAnnotationPair> getExactStringMatchPairs(
      JCas jcas, IdentifiedAnnotation mention, int sentDist) {
    List<CollectionTextRelationIdentifiedAnnotationPair> pairs = new ArrayList<>();
    
    if(markableStrings.contains(mention.getCoveredText().toLowerCase())){
      for(CollectionTextRelation cluster : JCasUtil.select(jcas, CollectionTextRelation.class)){
        Annotation mostRecent = ClusterUtils.getMostRecent((NonEmptyFSList)cluster.getMembers(), mention);
        if(mostRecent == null) continue;

        for(Markable m : JCasUtil.select(cluster.getMembers(), Markable.class)){
          if(m == mostRecent) break;
          // see if any of the members of the cluster have the exact same string as this 
          if(m.getCoveredText().toLowerCase().equals(mention.getCoveredText().toLowerCase())){
            pairs.add(new CollectionTextRelationIdentifiedAnnotationPair(cluster, mention));
            break;
          }
        }
      }
    }
    return pairs;
  }
  
  /*
   * getClusterPairs()
   * In this method we allow to link to clusters containing more than one mention even if they
   * are beyond a sentence distance. First we check whether the most recent mention in the cluster
   * is within the specified sentence distance (presumably longer than the sentence distance passed into
   * the method that constrains by distance). The wrinkle is that during training many clusters will have multiple
   * members but only one before the focus mention. So we need to count the members of a cluster until we 
   * get to the most recent one in the cluster. If that value is > 1 then we allow the pairing.
   */
  private List<CollectionTextRelationIdentifiedAnnotationPair> getClusterPairs(
      JCas jcas, IdentifiedAnnotation mention, int sentDist) {
    List<CollectionTextRelationIdentifiedAnnotationPair> pairs = new ArrayList<>();
    for(CollectionTextRelation cluster : JCasUtil.select(jcas, CollectionTextRelation.class)){
      NonEmptyFSList members = ((NonEmptyFSList)cluster.getMembers());
      Annotation first = (Annotation) members.getHead();
      if(first == null || mention.getBegin() <= first.getEnd()){
        continue;
      }

      IdentifiedAnnotation mostRecent = (IdentifiedAnnotation) ClusterUtils.getMostRecent((NonEmptyFSList)cluster.getMembers(), mention);
      if(mostRecent == null || EventCoreferenceAnnotator.sentDist(jcas, mostRecent, mention) > sentDist){
        continue;
      }
      int numMembers=0;
      for(Markable m : JCasUtil.select(cluster.getMembers(), Markable.class)){
        numMembers++;
        if(m == mostRecent) break;
      }
      if(numMembers > 1){
        pairs.add(new CollectionTextRelationIdentifiedAnnotationPair(cluster, mention));
      }
    }
    
    return pairs;
  }

  /*
   * Here we want to add only things that are nearby. First we check the semantic types
   * of the cluster we're comparing against. If any member is an Anatomical Site or Medication,
   * we add the cluster no matter what. Otherwise we check how many sentences are in between
   * the mention and the latest element of the cluster.
   */
  protected List<CollectionTextRelationIdentifiedAnnotationPair> getSentenceDistancePairs(JCas jcas, IdentifiedAnnotation mention, int sentDist){
    List<CollectionTextRelationIdentifiedAnnotationPair> pairs = new ArrayList<>();
    Set<String> bestAnaTypes = getBestEnt(jcas, (Markable) mention);
    
    for(CollectionTextRelation cluster : JCasUtil.select(jcas, CollectionTextRelation.class)){
      NonEmptyFSList members = ((NonEmptyFSList)cluster.getMembers());
      Annotation first = (Annotation) members.getHead();
      if(first == null || mention.getBegin() <= first.getEnd()) continue;
      
      // check for distance if they are not anatomical site or medication
      if(!(bestAnaTypes.contains(AnatomicalSiteMention.class.getSimpleName()) ||
          bestAnaTypes.contains(MedicationEventMention.class.getSimpleName()))){

        IdentifiedAnnotation mostRecent = (IdentifiedAnnotation) ClusterUtils.getMostRecent(members, mention);
        if(mostRecent == null || EventCoreferenceAnnotator.sentDist(jcas, mostRecent, mention) > sentDist) continue;
      }

      // check for types of cluster
      Set<String> bestClusterTypes = getBestEnt(jcas, cluster);
      if(bestAnaTypes.size() > 0 && bestClusterTypes.size() > 0){
        boolean overlap = false;
        for(String semType : bestAnaTypes){
          if(bestClusterTypes.contains(semType)){
            overlap = true;
          }
        }
        // they both correspond to named entities but no overlap in which category of named entity.
        if(!overlap){
          continue;
        }
      }
      pairs.add(new CollectionTextRelationIdentifiedAnnotationPair(cluster, mention));      
    }
    return pairs;
  }

  /*
   * getSectionHeaderPairs()
   * Here we want to add clusters where one of the members is on a line all by itself (a section header)
   * To do this we leverage the annotatino of Paragraphs, roughly the areas between newlines. If such a 
   * span only contains one sentence then we consider it a "header" (or also as important a list item).
   * If it is a header we add it. Here we use sentDist to not bother adding things that will be added by
   * the "sentence distance" method.
   */
  protected List<CollectionTextRelationIdentifiedAnnotationPair> getSectionHeaderPairs(JCas jcas, IdentifiedAnnotation mention, int sentDist){
    List<CollectionTextRelationIdentifiedAnnotationPair> pairs = new ArrayList<>();
    for(CollectionTextRelation cluster : JCasUtil.select(jcas, CollectionTextRelation.class)){
      NonEmptyFSList members = ((NonEmptyFSList)cluster.getMembers());
      Annotation first = (Annotation) members.getHead();
      if(first == null || mention.getBegin() <= first.getEnd()){
        continue;
      }

      // first check if it is sentence distance range -- if so we can ignore because it will be include by other pair generator
      IdentifiedAnnotation mostRecent = (IdentifiedAnnotation) ClusterUtils.getMostRecent(members, mention);
      if(mostRecent == null || EventCoreferenceAnnotator.sentDist(jcas, mostRecent, mention) <= sentDist){
        continue;
      }
      
      // now check if any of the mentions are in a section header
      List<Paragraph> pars = JCasUtil.selectCovered(jcas, Paragraph.class, 0, mention.getBegin());
      for(int j = 0; j < pars.size(); j++){
        boolean match = false;
        Paragraph par = pars.get(j); // pars.get(pars.size()-j-1);
        List<Sentence> coveredSents = JCasUtil.selectCovered(jcas, Sentence.class, par);
        if(coveredSents != null && coveredSents.size() == 1){
          // this is sentences that are the same span as paragraphs -- how we model section headers
          // see if any of the cluster mentions are in the section header
          for(Markable m : JCasUtil.select(members, Markable.class)){
            if(dominates(par, m)){
              pairs.add(new CollectionTextRelationIdentifiedAnnotationPair(cluster, mention));
              match = true;
              break;
            }
          }
        }
        if(match) break;
      }
    }
    return pairs;
  }
  
  protected List<CollectionTextRelationIdentifiedAnnotationPair> getHeadwordMatchPairs(JCas jcas, IdentifiedAnnotation mention, int sentDist){
    List<CollectionTextRelationIdentifiedAnnotationPair> pairs = new ArrayList<>();

    ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jcas, mention);
    if(headNode == null){
      Logger.getLogger(MentionClusterRankingCoreferenceAnnotator.class).warn("There is a markable with no dependency node covering it.");
      return pairs;
    }
    String head = headNode.getCoveredText().toLowerCase();
    if(headWordMarkables.containsKey(head)){
      final Set<Markable> headSet = headWordMarkables.get( head );

      ClusterMentionFetcher.populatePairs( jcas, mention, headSet, pairs );
    }
    
    return pairs;
  }
  
  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    // lookup from pair of annotations to binary text relation
    // note: assumes that there will be at most one relation per pair
    markableStrings = new HashSet<>();
    nodeEntMap = JCasUtil.indexCovering(jCas, ConllDependencyNode.class, IdentifiedAnnotation.class);
    headWordMarkables = new HashMap<>();
//    pairScores = getMarkablePairScores(jCas);
    
    Map<CollectionTextRelationIdentifiedAnnotationPair, CollectionTextRelationIdentifiedAnnotationRelation> relationLookup;
    if (this.isTraining()) {
      relationLookup = ClusterMentionFetcher.getPairRelationsForDocument( jCas );
    } else {
      relationLookup = new HashMap<>();
    }
    final Map<Segment, Collection<Markable>> segmentMarkables = JCasUtil.indexCovered( jCas, Segment.class, Markable.class );

//    for(Segment segment : JCasUtil.select(jCas, Segment.class)){
//      for(Markable mention : JCasUtil.selectCovered(jCas, Markable.class, segment)){
    for ( Collection<Markable> markables : segmentMarkables.values() ) {
      for ( Markable mention : markables ) {
        ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jCas, mention);
        String mentionText = mention.getCoveredText().toLowerCase();
        boolean singleton = true;
        double maxScore = Double.NEGATIVE_INFINITY;
        CollectionTextRelation maxCluster = null;
        List<Feature> mentionFeatures = new ArrayList<>();
        for(FeatureExtractor1<Markable> extractor : this.mentionExtractors){
          mentionFeatures.addAll(extractor.extract(jCas, mention));
        }

        for(CollectionTextRelationIdentifiedAnnotationPair pair : this.getCandidateRelationArgumentPairs(jCas, mention)){
          CollectionTextRelation cluster = pair.getCluster();
          // apply all the feature extractors to extract the list of features
          List<Feature> features = new ArrayList<>();
          features.addAll(mentionFeatures);
          
          for (RelationFeaturesExtractor<CollectionTextRelation,IdentifiedAnnotation> extractor : this.relationExtractors) {
            List<Feature> feats = extractor.extract(jCas, cluster, mention);
            if (feats != null){
//              Logger.getRootLogger().info(String.format("For cluster with %d mentions, %d %s features", JCasUtil.select(cluster.getMembers(), Markable.class).size(), feats.size(), extractor.getClass().getSimpleName()));
              features.addAll(feats);
            }
          }
          
          
          // here is where feature conjunctions can go (dupFeatures)
          List<Feature> dupFeatures = new ArrayList<>();
          // sanity check on feature values
          for (Feature feature : features) {
            if (feature.getValue() == null) {
              feature.setValue("NULL");
              String message = String.format("Null value found in %s from %s", feature, features);
              System.err.println(message);
              //            throw new IllegalArgumentException(String.format(message, feature, features));
            }else{
              String prefix = null;
//              if(mentionText.equals("it") || mentionText.equals("this") || mentionText.equals("that")){
//                prefix = "PRO_"+mentionText;
//              }else if(headNode != null && headNode.getPostag() != null){
//                prefix = headNode.getPostag();                
//              }else{
//                prefix = "UNK";
//              }
              if(prefix != null){
                dupFeatures.add(new Feature(prefix+"_"+feature.getName(), feature.getValue()));
              }
            }
          }
          features.addAll(dupFeatures);    

          // during training, feed the features to the data writer
          // create a classification instance and write it to the training data

          if (this.isTraining()) {
            String category = this.getRelationCategory(relationLookup, cluster, mention);
            if (category == null) {
              continue;
            }
            double outVal = 1.0;
            if(category.equals(NO_RELATION_CATEGORY)){
              outVal = 0.0;
            }

            QidInstance<Double> inst = new QidInstance<>();
            inst.setQid(String.valueOf(qid));
            inst.addAll(features);
            inst.setOutcome(outVal);
            this.dataWriter.write(inst);
            if(!category.equals(NO_RELATION_CATEGORY)){
              singleton = false;
              break;
            }
          }

          // during classification feed the features to the classifier and create
          // annotations
          else {
            Double prediction = this.classify(features);
            if(prediction > maxScore){
              maxScore = prediction;
              maxCluster = cluster;
            }
          }
        }
        
        markableStrings.add(mention.getCoveredText().toLowerCase());
        
        if(headNode != null){
          String head = headNode.getCoveredText().toLowerCase();
          if(!headWordMarkables.containsKey(head)){
            headWordMarkables.put(head, new HashSet<Markable>());
          }
          headWordMarkables.get(head).add(mention);
        }
        
        if(this.isTraining()){
          // write a dummy link with only mention features:
          QidInstance<Double> inst = new QidInstance<>();
          inst.setQid(String.valueOf(qid));
          for(Feature feat : mentionFeatures){
            if(feat.getName() != null){
              feat.setName("DUMMYLINK_" + feat.getName());
            }
          }
          inst.addAll(mentionFeatures);
          if(singleton){
            inst.setOutcome(1.0);
          }else{
            inst.setOutcome(0.0);
          }
          this.dataWriter.write(inst);
        }else{
          Double nullPrediction = this.classify(mentionFeatures);
          if(nullPrediction > maxScore){
            // make the markable it's own cluster:
            CollectionTextRelation chain = new CollectionTextRelation(jCas);
            NonEmptyFSList list = new NonEmptyFSList(jCas);
            list.setHead(mention);
            list.setTail(new EmptyFSList(jCas));
            chain.setMembers(list);
            chain.addToIndexes();
            list.addToIndexes();
            list.getTail().addToIndexes();
          }else{
            createRelation(jCas, maxCluster, mention, CLUSTER_RELATION_CATEGORY);
          }
        }
        qid++;
      }
    }
    
    removeSingletonClusters(jCas);
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
  protected Double classify(List<Feature> features) throws CleartkProcessingException {
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
      String predictedCategory) {
    // add the relation to the CAS
    CollectionTextRelationIdentifiedAnnotationRelation relation = new CollectionTextRelationIdentifiedAnnotationRelation(jCas);
    relation.setCluster(cluster);
    relation.setMention(mention);
    relation.setCategory(predictedCategory);
    relation.addToIndexes();
    
//    RelationArgument arg = new RelationArgument(jCas);
//    arg.setArgument(mention);
    ListFactory.append(jCas, cluster.getMembers(), mention);    
  }


  private void removeSingletonClusters(JCas jcas){
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
  
  private static final boolean dominates(Annotation arg1, Annotation arg2) {
    return (arg1.getBegin() <= arg2.getBegin() && arg1.getEnd() >= arg2.getEnd());
  }

  public Set<String> getBestEnt(JCas jcas, CollectionTextRelation cluster){
    Set<String> semTypes = new HashSet<>();
    for(Markable member : JCasUtil.select(cluster.getMembers(), Markable.class)){
      semTypes.addAll(getBestEnt(jcas, member));
    }
    return semTypes;
  }
  
  public Set<String> getBestEnt(JCas jcas, Markable markable){
    Set<String> bestEnts = new HashSet<>();
    IdentifiedAnnotation bestEnt = null;
    Set<IdentifiedAnnotation> otherBestEnts = new HashSet<>();
    ConllDependencyNode head = DependencyUtility.getNominalHeadNode(jcas, markable);
    Collection<IdentifiedAnnotation> coveringEnts = nodeEntMap.get(head);
    for(IdentifiedAnnotation ent : coveringEnts){
      if(ent.getOntologyConceptArr() == null) continue; // skip non-umls entities.
      ConllDependencyNode entHead = DependencyUtility.getNominalHeadNode(jcas, ent);
      if(entHead == head){
        if(bestEnt == null){
          bestEnt = ent;
        }else if((ent.getEnd()-ent.getBegin()) > (bestEnt.getEnd() - bestEnt.getBegin())){
          // if the span of this entity is bigger than the biggest existing one:
          bestEnt = ent;
          otherBestEnts = new HashSet<>();
        }else if((ent.getEnd()-ent.getBegin()) == (bestEnt.getEnd() - bestEnt.getBegin())){
          // there is another one with the exact same span and possibly different type!
          otherBestEnts.add(ent);
        }
      }
    }

    if(bestEnt!=null){
      bestEnts.add(bestEnt.getClass().getSimpleName());
      for(IdentifiedAnnotation other : otherBestEnts){
        bestEnts.add(other.getClass().getSimpleName());
      }
    }
    return bestEnts;
  }
  
  
  public Map<HashableArguments, Double> getMarkablePairScores(JCas jCas){
    Map<HashableArguments, Double> scoreMap = new HashMap<>();
    for(CoreferenceRelation reln : JCasUtil.select(jCas, CoreferenceRelation.class)){
      HashableArguments pair = new HashableArguments((IdentifiedAnnotation)reln.getArg1().getArgument(), (IdentifiedAnnotation)reln.getArg2().getArgument());
      scoreMap.put(pair, reln.getConfidence());
    }
    return scoreMap;
  }

//  public static class CollectionTextRelationIdentifiedAnnotationPair {
//    private final CollectionTextRelation cluster;
//    private final IdentifiedAnnotation mention;
//
//    public CollectionTextRelationIdentifiedAnnotationPair(CollectionTextRelation cluster, IdentifiedAnnotation mention){
//      this.cluster = cluster;
//      this.mention = mention;
//    }
//
//    public final CollectionTextRelation getCluster(){
//      return this.cluster;
//    }
//
//    public final IdentifiedAnnotation getMention(){
//      return this.mention;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//      CollectionTextRelationIdentifiedAnnotationPair other = (CollectionTextRelationIdentifiedAnnotationPair) obj;
//      return (this.cluster == other.cluster &&
//          this.mention == other.mention);
//    }
//
//    @Override
//    public int hashCode() {
//      return 31*cluster.hashCode() + (mention==null ? 0 : mention.hashCode());
//    }
//  }

}
