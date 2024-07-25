package org.apache.ctakes.coreference.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.coreference.ae.features.*;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.relationextractor.ae.features.*;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CoreferenceRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textsem.MedicationEventMention;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.*;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.Feature;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

@PipeBitInfo(
      name = "Event Coreference Annotator",
      description = "Annotates Event Coreferences.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION,
            PipeBitInfo.TypeProduct.DEPENDENCY_NODE, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION,
            PipeBitInfo.TypeProduct.MARKABLE },
      products = { PipeBitInfo.TypeProduct.COREFERENCE_RELATION }
)
public class EventCoreferenceAnnotator extends RelationExtractorAnnotator {

  public static final String IDENTITY_RELATION = "Identity";

  public static final int DEFAULT_SENT_DIST = 5;
  public static final String PARAM_SENT_DIST = "SentenceDistance";
  @ConfigurationParameter(name = PARAM_SENT_DIST, mandatory = false, description = "Number of sentences allowed between coreferent mentions")
  private int maxSentDist = DEFAULT_SENT_DIST;
  
  public static final double DEFAULT_PAR_SIM = 0.5;
  public static final String PARAM_PAR_SIM = "PararaphSimilarity";
  @ConfigurationParameter(name = PARAM_PAR_SIM, mandatory = false, description = "Similarity required to pair paragraphs for coreference")
  private double simThreshold = DEFAULT_PAR_SIM;

  public static final boolean DEFAULT_SCORE_ALL = false;
  public static final String PARAM_SCORE_ALL = "ScoreAllPairs";
  @ConfigurationParameter(name = PARAM_SCORE_ALL, mandatory = false, description = "Whether to score all pairs (as in a feature detector")
  private boolean scoreAll = DEFAULT_SCORE_ALL;
  
  private Map<ConllDependencyNode,List<IdentifiedAnnotation>> nodeEntMap = null;
  private Map<Markable,Set<String>> markableEnts = null;
  private List<Markable> markablesByConfidence = null;
  private Map<Annotation,NonEmptyFSList> chains = null;
  private double lastScore;
  
  
  private Logger LOGGER = LogManager.getLogger(EventCoreferenceAnnotator.class);
  
  public static AnalysisEngineDescription createDataWriterDescription(
      Class<? extends DataWriter<String>> dataWriterClass,
      File outputDirectory,
      float downsamplingRate) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        EventCoreferenceAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        true,
        RelationExtractorAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
        downsamplingRate,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        dataWriterClass,
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outputDirectory);
  }
  
  public static AnalysisEngineDescription createAnnotatorDescription(String modelPath)
      throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        EventCoreferenceAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        false,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        modelPath);
  }
  
  public static AnalysisEngineDescription createScoringAnnotatorDescription(String modelPath)
      throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        EventCoreferenceAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        false,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        modelPath,
        EventCoreferenceAnnotator.PARAM_SCORE_ALL,
        true);
  }
  
  @Override
  protected List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> getFeatureExtractors()
        throws ResourceInitializationException {
    List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> featureExtractorList = new ArrayList<>();
    
    // pick and choose from base class:
    featureExtractorList.add(new TokenFeaturesExtractor());
    featureExtractorList.add(new PartOfSpeechFeaturesExtractor());
    featureExtractorList.add(new PhraseChunkingExtractor());
//    featureExtractorList.add(new NamedEntityFeaturesExtractor()); // same features in UMLSFeatureExtractor
    featureExtractorList.add(new DependencyTreeFeaturesExtractor());
//    featureExtractorList.add(new DependencyPathFeaturesExtractor()); // not in mention-cluster version
    
//    featureList.add(new DistanceFeatureExtractor());
    featureExtractorList.add(new StringMatchingFeatureExtractor());
    featureExtractorList.add(new TokenFeatureExtractor()); // agreement features
    featureExtractorList.add(new SectionFeatureExtractor());
    featureExtractorList.add(new UMLSFeatureExtractor());
    featureExtractorList.add(new CorefSyntaxFeatureExtractor()); // dep head feature
    featureExtractorList.add(new TemporalFeatureExtractor());
    
    // added for feature parity with cluster version:
    featureExtractorList.add(new SalienceFeatureExtractor());
    featureExtractorList.add(new AttributeFeatureExtractor());
    
//    featureExtractorList.add(new ChainStackFeatureExtractor());
    
//    featureExtractorList.add(new DocumentStructureTreeExtractor());
    try{
      featureExtractorList.add(new DistSemFeatureExtractor());
    }catch(IOException e){
      e.printStackTrace();
    }
    
    return featureExtractorList;
  }
  
  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    if(this.isTraining() && JCasUtil.select(jCas, CoreferenceRelation.class).size() == 0){
      LOGGER.debug("Skipping document with no gold standard coreference relations.");
      return;
    }
    numClassifications = 0;
    nodeEntMap = JCasUtil.indexCovering(jCas, ConllDependencyNode.class, IdentifiedAnnotation.class);
    markableEnts = new HashMap<>();
    chains = new HashMap<>();
    markablesByConfidence = new ArrayList<>(JCasUtil.select(jCas, Markable.class));
    Collections.sort(markablesByConfidence, new MarkableConfidenceComparator());
    for(Markable m : markablesByConfidence){
      markableEnts.put(m, getBestEnt(jCas, m));
    }
    super.process(jCas);
    if(!this.isTraining() && !this.scoreAll){
      for(NonEmptyFSList chainHead : new HashSet<>(chains.values())){
        CollectionTextRelation chain = new CollectionTextRelation(jCas);
        chain.setMembers(chainHead);
        
        NonEmptyFSList cur = chainHead;
        while(cur.getTail() != null){
          cur = (NonEmptyFSList) cur.getTail();
        }
        EmptyFSList tail = new EmptyFSList(jCas);
        tail.addToIndexes();
        cur.setTail(tail);
                
        chain.addToIndexes();
      }
    }
    LOGGER.debug("This document had : " + numClassifications + " pair classifications");
    foundAnaphors.clear();
    chains.clear();
  }

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

  @Override
  protected Iterable<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
      JCas jcas, Annotation segment) {
    
    return new PairIterable(jcas, segment);
  }

  public List<IdentifiedAnnotationPair> getClosePairs(JCas jcas, Annotation segment, double confidence){
    List<Markable> markables = new ArrayList<>(JCasUtil.select(jcas, Markable.class));
    List<IdentifiedAnnotationPair> pairs = new ArrayList<>();
    for(int i = 1; i < markables.size(); i++){
      Markable ana = markables.get(i);
      // only look at anaphors w/in this segment:
      if(!dominates(segment, ana)){
        continue;
      }
      Set<String> bestAnaTypes = getBestEnt(jcas, ana);

      for(int j = i-1; j >= 0; j--){
        Markable ante = markables.get(j);  
        if(ante.getConfidence() < confidence){
          continue;
        }
        
        // check sentence distance unless this is an anatomical site or medication
        if(!(bestAnaTypes.contains(AnatomicalSiteMention.class.getSimpleName()) ||
            bestAnaTypes.contains(MedicationEventMention.class.getSimpleName()))){
          int sentdist = sentDist(jcas, ante, ana);
          if(sentdist > maxSentDist) break;
        }
        
        Set<String> bestAnteTypes = getBestEnt(jcas, ante);
        
        // if they both have entity types we need to make sure they match
        // -- if neither has a sem type or only one is tagged we can let them
        // try to match.
        if(bestAnaTypes.size() > 0 && bestAnteTypes.size() > 0){
          boolean overlap = false;
          for(String semType : bestAnaTypes){
            if(bestAnteTypes.contains(semType)){
              overlap = true;
            }
          }
          // they both correspond to named entities but no overlap in which category of named entity.
          if(!overlap){
            continue;
          }
        }
        pairs.add(new IdentifiedAnnotationPair(ante, ana));
      }
    }
    return pairs;
  }

  public Set<String> getBestEnt(JCas jcas, Markable markable){
    if(markableEnts.containsKey(markable)) return markableEnts.get(markable);
//    markableEnts.put(markable, new HashSet<String>());
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
//      markableEnts.get(markable).add(bestEnt.getClass().getSimpleName());
      for(IdentifiedAnnotation other : otherBestEnts){
        bestEnts.add(other.getClass().getSimpleName());
//        markableEnts.get(markable).add(other.getClass().getSimpleName());
      }
    }
    return bestEnts;
//    return markableEnts.get(markable);
  }
  public static boolean dominates(Annotation arg1, Annotation arg2) {
    return (arg1.getBegin() <= arg2.getBegin() && arg1.getEnd() >= arg2.getEnd());
  }

  public List<IdentifiedAnnotationPair> getParagraphPairs(JCas jcas, Annotation segment){
    List<IdentifiedAnnotationPair> pairs = new ArrayList<>();
    
    // CODE FOR PARAGRAPH-BASED MATCHING
    List<Paragraph> pars = new ArrayList<>(JCasUtil.select(jcas, Paragraph.class));
    double[][] sims = new double[pars.size()][pars.size()];
    for(int i = 0; i < sims.length; i++){
      Arrays.fill(sims[i], 0.0);
    }
    
    for(int i = 0; i < pars.size(); i++){
      // get all pairs within this paragraph
      List<Markable> curParMarkables = JCasUtil.selectCovered(Markable.class, pars.get(i));
      for(int anaId = 1; anaId < curParMarkables.size(); anaId++){
        for(int anteId = anaId-1; anteId >= 0; anteId--){
          Markable ana = curParMarkables.get(anaId);
          Markable ante = curParMarkables.get(anteId);
          int sentdist = sentDist(jcas, ante, ana);
          if(sentdist > maxSentDist) break;
          pairs.add(new IdentifiedAnnotationPair(ante, ana));
        }
      }
    }
    return pairs; 
  }
  
  public List<IdentifiedAnnotationPair> getSimilarPairs(JCas jcas, Annotation segment){
    List<IdentifiedAnnotationPair> pairs = new ArrayList<>();
    FSArray parVecs = JCasUtil.selectSingle(jcas, FSArray.class);

    List<Paragraph> pars = new ArrayList<>(JCasUtil.select(jcas, Paragraph.class));
    double[][] sims = new double[pars.size()][pars.size()];
    for(int i = 0; i < sims.length; i++){
      Arrays.fill(sims[i], 0.0);
    }

    for(int i = 0; i < pars.size(); i++){
      List<Markable> curParMarkables = JCasUtil.selectCovered(Markable.class, pars.get(i));
      FloatArray parVec = (FloatArray) parVecs.get(i);
      for(int j = i-1; j >= 0; j--){
        if(sims[i][j] == 0.0){
          // compute the sim explicitly
          FloatArray prevParVec = (FloatArray) parVecs.get(j);
          sims[i][j] = calculateSimilarity(parVec, prevParVec);
        }

        if(sims[i][j] > simThreshold){
          // pair up all markables in each paragraph
          List<Markable> prevParMarkables = JCasUtil.selectCovered(Markable.class, pars.get(j));
          for(int anaId = 0; anaId < curParMarkables.size(); anaId++){
            for(int anteId = prevParMarkables.size()-1; anteId >= 0; anteId--){
              Markable ana = curParMarkables.get(anaId);
              Markable ante = prevParMarkables.get(anteId);
              int sentdist = sentDist(jcas, ante, ana);
              if(sentdist > maxSentDist) break;
              pairs.add(new IdentifiedAnnotationPair(ante, ana));
            }
          }
        }
      }
    }
    return pairs;
  }
  
  public List<IdentifiedAnnotationPair> getConfidentPairs(JCas jcas, Annotation segment, double threshold){
    List<IdentifiedAnnotationPair> pairs = new ArrayList<>();
    List<Markable> anas = JCasUtil.selectCovered(Markable.class, segment);
    
    for(Markable ana : anas){
      for(Markable ante : markablesByConfidence){
        // if we are into the unconfident 
        if(ante.getConfidence() < threshold){
          break;
        }
        
        // if the candidate antecedent is after the anafor skip it.
        if(ante.getBegin() > ana.getBegin() && ante.getEnd() > ante.getEnd()){
          continue;
        }
        
        // if the anaphor has a sem type make sure the ante matches it
        boolean match = false;
        if(markableEnts.get(ana).size() > 0){
          if(markableEnts.get(ante).size() == 0){
            match = true;
          }else{
            for(String semType : markableEnts.get(ana)){
              if(markableEnts.get(ante).contains(semType)){
                match = true;
                break;
              }
            }
          }
        }else{
          match = true;
        }
        
        if(match){
          pairs.add(new IdentifiedAnnotationPair(ante, ana));
        }
      }
    }
    return pairs;
  }
  /*
   * Markables that are in a section header are highly salient and prime candidates
   * as antecedents in coreference. We detect headers as sentences that are the only sentence in a paragraph.
   * This is probably high recall with some precision hits but thats ok for now. 
   */
  public List<IdentifiedAnnotationPair> getSectionHeaderPairs(JCas jcas, Annotation segment, double confidence){
    List<IdentifiedAnnotationPair> pairs = new ArrayList<>();
    List<Markable> markables = JCasUtil.selectCovered(jcas, Markable.class, segment);
    for(int i = 0; i < markables.size(); i++){
      IdentifiedAnnotation ana = markables.get(i);
      List<Paragraph> pars = JCasUtil.selectCovered(jcas, Paragraph.class, 0, ana.getBegin());
      for(int j = 0; j < pars.size(); j++){
        Paragraph par = pars.get(j); // pars.get(pars.size()-j-1);
        List<Sentence> coveredSents = JCasUtil.selectCovered(jcas, Sentence.class, par);
        if(coveredSents != null && coveredSents.size() == 1){
          for(Markable anteCandidate : JCasUtil.selectCovered(jcas, Markable.class, par)){
            if(anteCandidate.getConfidence() > confidence){
              pairs.add(new IdentifiedAnnotationPair(anteCandidate, ana));
            }
          }
        }
      }
    }
    return pairs;
  }

  public List<IdentifiedAnnotationPair> getAlreadyLinkedPairs(JCas jcas, Annotation segment){
    List<IdentifiedAnnotationPair> pairs = new ArrayList<>();
    List<Markable> segMarkables = new ArrayList<>(JCasUtil.selectCovered(jcas, Markable.class, segment));
    
    // if we are testing, there are no chains in the cas yet so we have to look at the 
    // intermediate data structures we use.
    for(int i = 0; i < segMarkables.size(); i++){
      Markable ana = segMarkables.get(i);
//      if(this.isTraining()){
      for(CollectionTextRelation chain : JCasUtil.select(jcas, CollectionTextRelation.class)){
        FSList head = chain.getMembers();
        Markable last = null;
        while(head instanceof NonEmptyFSList){
          Markable m = (Markable) ((NonEmptyFSList)head).getHead();

          // ignore markables past the current anaphor or equal to it
          if(m == null || m.getEnd() > ana.getEnd()){
            break;
          }
          if(!(m.getBegin() == ana.getBegin() && m.getEnd() == ana.getEnd())){
            last = m;
          }
          head = ((NonEmptyFSList)head).getTail();
        }
        if(last != null){
          pairs.add(new IdentifiedAnnotationPair(last, ana));
        }
      }
//      }
    }
    
    return pairs;
  }
  
  public List<IdentifiedAnnotationPair> getHeadwordMatchingPairs(JCas jcas, Annotation segment){
    List<IdentifiedAnnotationPair> pairs = new ArrayList<>();
    List<Markable> segMarkables = new ArrayList<>(JCasUtil.selectCovered(jcas, Markable.class, segment));
    for(int i = 0; i < segMarkables.size(); i++){
      Markable ana = segMarkables.get(i);
      ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jcas, ana);
      String headword = null;
      if(headNode != null){
        headword = headNode.getCoveredText().toLowerCase();
      }else{
        continue;
      }
      List<Markable> previousMarkables = JCasUtil.selectCovered(jcas, Markable.class, 0, ana.getBegin());
      for(int j = 0; j < previousMarkables.size(); j++){
        Markable ante = previousMarkables.get(j);
        ConllDependencyNode anteNode = DependencyUtility.getNominalHeadNode(jcas, ante);
        if(anteNode != null){
          String anteHeadword = anteNode.getCoveredText().toLowerCase();
          if(headword.equals(anteHeadword)){
            pairs.add(new IdentifiedAnnotationPair(ante, ana));
          }
        }
      }
    }
    return pairs;
  }
  
  @Override
  protected String classify(List<Feature> features)
      throws CleartkProcessingException {
    numClassifications++;
    
    String category = super.classifier.classify(features);
    
        
    if(this.scoreAll){
      Map<String,Double> scores = super.classifier.score(features);
      this.lastScore = scores.get(IDENTITY_RELATION);
      category = IDENTITY_RELATION;
    }
    return category;
  }
  
  @Override
  protected Class<? extends Annotation> getCoveringClass() {
    return Segment.class;
  }
  
  @Override
  protected Class<? extends BinaryTextRelation> getRelationClass() {
    return CoreferenceRelation.class;
  }

  protected HashSet<IdentifiedAnnotation> foundAnaphors = new HashSet<>(); 
  int numClassifications = 0;
  
  @Override
  protected void createRelation(
      JCas jCas,
      IdentifiedAnnotation ante,
      IdentifiedAnnotation ana,
      String predictedCategory) { 
    if(this.scoreAll){
      // do this first -- if we need to score all pairs then it doesn't really make sense to talk about
      // "found anaphors" since we're not in finding mode.
      CoreferenceRelation relation = buildRelation(jCas, ante, ana, predictedCategory);
      relation.setConfidence(this.lastScore);
      relation.addToIndexes();
    } // check if its already been linked
    else if(!foundAnaphors.contains(ana)){
      // add the relation to the CAS
      CoreferenceRelation relation = buildRelation(jCas, ante, ana, predictedCategory);
      relation.addToIndexes();
      foundAnaphors.add(ana);
      if(!chains.containsKey(ante)){
        // new chain
        NonEmptyFSList anteEl = new NonEmptyFSList(jCas);
        NonEmptyFSList anaEl = new NonEmptyFSList(jCas);
        anteEl.setHead(ante);
        anaEl.setHead(ana);
        anteEl.setTail(anaEl);
        anaEl.setTail(null);
        chains.put(ante, anteEl);
        chains.put(ana, anteEl);
        anaEl.addToIndexes();
        anteEl.addToIndexes();
      }else{
        NonEmptyFSList oldChain = chains.get(ante);
        NonEmptyFSList chainEnd = oldChain;
        NonEmptyFSList anaEl = new NonEmptyFSList(jCas);
        anaEl.setHead(ana);
        anaEl.setTail(null);
        
        while(chainEnd.getTail() != null){
          chainEnd = (NonEmptyFSList) chainEnd.getTail();          
        }
        
        chainEnd.setTail(anaEl);
        chains.put(ana, oldChain);
        anaEl.addToIndexes();
      }
    }else{
      LOGGER.error("Greedy coreference resolution violated -- anaphor linked to two candidate antecedents!");
    }
  }
  
  private CoreferenceRelation buildRelation(JCas jCas, Annotation ante, Annotation ana, String predictedCategory){
    RelationArgument relArg1 = new RelationArgument(jCas);
    relArg1.setArgument(ante);
    relArg1.setRole("Antecedent");
    relArg1.addToIndexes();
    RelationArgument relArg2 = new RelationArgument(jCas);
    relArg2.setArgument(ana);
    relArg2.setRole("Anaphor");
    relArg2.addToIndexes();
    CoreferenceRelation relation = new CoreferenceRelation(jCas);
    relation.setArg1(relArg1);
    relation.setArg2(relArg2);
    relation.setCategory(predictedCategory);
    return relation;
  }
  
  @Override
  protected String getRelationCategory(
      Map<List<Annotation>, BinaryTextRelation> relationLookup,
      IdentifiedAnnotation ante, IdentifiedAnnotation ana) {
    String cat = super.getRelationCategory(relationLookup, ante, ana);
    int dist = sentsBetween(ante, ana);
    
    if(cat != null && !cat.equals(NO_RELATION_CATEGORY)){
      // cat is some coref category
      foundAnaphors.add(ana);
      LOGGER.info(String.format("DISTSALIENCE: (%d,%f,1)\n", dist, ante.getConfidence()));    
    }else{
      // sample 10 percent of negative examples:
      if(Math.random() < 0.1){
        LOGGER.info(String.format("DISTSALIENCE: (%d,%f,0)\n", dist, ante.getConfidence()));
      }
    }
    return cat;
  }

  public static int sentDist(JCas jcas, IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2){
    return JCasUtil.selectCovered(jcas, Sentence.class, arg1.getBegin(), arg2.getEnd()).size();
  }
  
  public static int sentsBetween(IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2) {
    Collection<Sentence> sents = JCasUtil.selectBetween(Sentence.class, arg1, arg2);
    return sents.size();
  }
  
  private static double calculateSimilarity(FloatArray f1, FloatArray f2){
    double sim = 0.0f;
    double f1len = 0.0;
    double f2len = 0.0;
    
    for(int i = 0; i < f1.size(); i++){
      sim += (f1.get(i) * f2.get(i));
      f1len += (f1.get(i) * f1.get(i));
      f2len += (f2.get(i) * f2.get(i));
    }
    f1len = Math.sqrt(f1len);
    f2len = Math.sqrt(f2len);
    sim = sim / (f1len * f2len);
    
    return sim;
  }
  
  class PairIterable implements Iterable<IdentifiedAnnotationPair> {

    PairIterator iter = null;
    
    public PairIterable(JCas jcas, Annotation segment){
      iter = new PairIterator(jcas, segment);
    }
    
    @Override
    public Iterator<IdentifiedAnnotationPair> iterator() {
      return iter;
    }
    
  }
  
  class PairIterator implements Iterator<IdentifiedAnnotationPair> {

    JCas jcas = null;
    Annotation segment = null;
    // need 2 passes -- first for preliminary pairs, then for linking to
    // existing chains - could bee FIXME'd by creating uima chains as we go instead
    // of using placeholder chains but that is substantially more complicated.
    List<IdentifiedAnnotationPair> pairs = new ArrayList<>();
    List<IdentifiedAnnotationPair> pass2Pairs = null;
    IdentifiedAnnotationPair next = null;
    
    public PairIterator(JCas jcas, Annotation segment) {
      this.jcas = jcas;
      this.segment = segment;
      
      pairs.addAll(getClosePairs(jcas, segment, 0.0));
      pairs.addAll(getSectionHeaderPairs(jcas, segment, 0.0));
      pairs.addAll(getAlreadyLinkedPairs(jcas, segment));
      pairs.addAll(getHeadwordMatchingPairs(jcas, segment));
//    
//      pairs.addAll(getConfidentPairs(jcas, segment, 0.25));
//      if(!isTraining()){
//        Collections.sort(pairs, new MarkableConfidenceComparator());
//        Collections.sort(pairs, new IdentifiedAnnotationPairComparator());
//      }
    }

    @Override
    public boolean hasNext() {
      while(pairs.size() > 0){
        next = pairs.remove(0);
        IdentifiedAnnotation ante = next.getArg1();
        IdentifiedAnnotation ana = next.getArg2();
        if(dominates(ante, ana) || dominates(ana,ante)) continue;
        if(!foundAnaphors.contains(ana)){
          return true;
        }
      }
      
      if(pass2Pairs == null){
        pass2Pairs = new ArrayList<>();
//        pass2Pairs.addAll(getAlreadyLinkedPairs(this.jcas, this.segment));
      }
      
      while(pass2Pairs.size() > 0){
        next = pass2Pairs.remove(0);
        IdentifiedAnnotation ante = next.getArg1();
        IdentifiedAnnotation ana = next.getArg2();
        if(dominates(ante, ana) || dominates(ana,ante)) continue;
        if(!foundAnaphors.contains(ana)){
          return true;
        }
      }
      
      return false; // if we get this far then there were no good candidates
    }

    @Override
    public IdentifiedAnnotationPair next() {
      numClassifications++;
      return next;
    }

    @Override
    public void remove() {
      // Optional implementation
    }
    
  }
  
  public class MarkablePairConfidenceComparator implements
  Comparator<IdentifiedAnnotationPair> {

    public int compare(IdentifiedAnnotationPair o1, IdentifiedAnnotationPair o2) {
      if(o1 == o2) return 0;
      int sim;
      IdentifiedAnnotation ante1 = o1.getArg1();
      IdentifiedAnnotation ante2 = o2.getArg1();
      IdentifiedAnnotation ana1 = o1.getArg2();
      IdentifiedAnnotation ana2 = o2.getArg2();
      
      // first level sorting is by anaphor:
      if(ana1.getBegin() != ana2.getBegin()){
        sim = ana1.getBegin() - ana2.getBegin() > 0 ? 1 : -1;
      }else if(ana1.getEnd() != ana2.getEnd()){
        sim = ana1.getEnd() - ana2.getEnd() > 0 ? 1 : -1;
      }else{
        // sort by antecedent
        if(ante1.getConfidence() > ante2.getConfidence()){
          sim = -1;
        }else if(ante1.getConfidence() < ante2.getConfidence()){
          sim = 1;
        }else{
          sim = 0;
        }
      }
      
      return sim;
    }

  }

  public class MarkableConfidenceComparator implements Comparator<Markable> {
    public int compare(Markable m1, Markable m2){
      if(m1 == m2) return 0;
      if(m1.getConfidence() > m2.getConfidence()){
        return -1;
      }else if(m1.getConfidence() < m2.getConfidence()){
        return 1;
      }else{
        return 0;
      }
    }
  }
  
  public class IdentifiedAnnotationPairComparator implements Comparator<IdentifiedAnnotationPair> {

    public int compare(IdentifiedAnnotationPair o1, IdentifiedAnnotationPair o2) {
      if(o1 == o2) return 0;
      int sim;
      IdentifiedAnnotation ante1 = o1.getArg1();
      IdentifiedAnnotation ante2 = o2.getArg1();
      IdentifiedAnnotation ana1 = o1.getArg2();
      IdentifiedAnnotation ana2 = o2.getArg2();
      
      // first level sorting is by anaphor:
      if(ana1.getBegin() != ana2.getBegin()){
        sim = ana1.getBegin() - ana2.getBegin() > 0 ? 1 : -1;
      }else if(ana1.getEnd() != ana2.getEnd()){
        sim = ana1.getEnd() - ana2.getEnd() > 0 ? 1 : -1;
      }else if(ante1.getBegin() != ante2.getBegin()){
        sim = ante1.getBegin() - ante2.getBegin() > 0 ? 1 : -1;
      }else if(ante1.getEnd() != ante2.getEnd()){
        sim = ante1.getEnd() - ante2.getEnd() > 0 ? 1 : -1;
      }else{
        sim = 0;
      }
      return sim;
    }
    
  }
  
  private class AnnotationComparator implements Comparator<Annotation> {

    public AnnotationComparator() {
    }

    @Override
    public int compare(Annotation o1, Annotation o2) {
      if(o1.getBegin() < o2.getBegin()){
        return -1;
      }else if(o1.getBegin() == o2.getBegin() && o1.getEnd() < o2.getEnd()){
        return -1;
      }else if(o1.getBegin() == o2.getBegin() && o1.getEnd() > o2.getEnd()){
        return 1;
      }else if(o2.getBegin() < o1.getBegin()){
        return 1;
      }else{
        return 0;
      }
    }
  }

}
