package org.apache.ctakes.coreference.eval;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.assertion.medfacts.cleartk.PolarityCleartkAnalysisEngine;
import org.apache.ctakes.coreference.ae.DeterministicMarkableAnnotator;
import org.apache.ctakes.coreference.ae.MarkableSalienceAnnotator;
import org.apache.ctakes.coreference.eval.EvaluationOfEventCoreference.DocumentIDPrinter;
import org.apache.ctakes.coreference.eval.EvaluationOfEventCoreference.RemovePersonMarkables;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.liblinear.LibLinearBooleanOutcomeDataWriter;

import com.google.common.base.Function;
import com.lexicalscope.jewel.cli.CliFactory;

public class EvaluationOfMarkableSalience extends Evaluation_ImplBase<AnnotationStatistics<Boolean>> {

  public static void main(String[] args) throws Exception {
    Options options = CliFactory.parseArguments(Options.class, args);
    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = getTrainItems(options);
    List<Integer> testItems = getTestItems(options);
    
    EvaluationOfMarkableSalience eval = 
        new EvaluationOfMarkableSalience(new File("target/eval/salience"), 
            options.getRawTextDirectory(), 
            options.getXMLDirectory(), 
            options.getXMLFormat(), 
            options.getSubcorpus(), 
            options.getXMIDirectory(), null);
    eval.prepareXMIsFor(patientSets);

    AnnotationStatistics<Boolean> stats = eval.trainAndTest(trainItems, testItems);
    System.out.println(stats);
    System.out.println(stats.confusions());
  }

  public EvaluationOfMarkableSalience(File baseDirectory,
      File rawTextDirectory, File xmlDirectory,
      org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMLFormat xmlFormat,
      org.apache.ctakes.temporal.eval.Evaluation_ImplBase.Subcorpus subcorpus,
      File xmiDirectory, File treebankDirectory) {
    super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, subcorpus,
        xmiDirectory, treebankDirectory);
  }

  @Override
  protected void train(CollectionReader collectionReader, File directory)
      throws Exception {
    AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
    aggregateBuilder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DocumentIDPrinter.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DeterministicMarkableAnnotator.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemovePersonMarkables.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(SetGoldConfidence.class, SetGoldConfidence.PARAM_GOLD_VIEW, GOLD_VIEW_NAME));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(MarkableSalienceAnnotator.createDataWriterDescription(
        LibLinearBooleanOutcomeDataWriter.class,
        directory
        )));
    SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());
    // s=0 -> logistic regression with L2-norm (gives probabilistic outputs)
    String[] optArray = new String[]{ "-s", "0", "-c", "1", "-w1", "1"};
    JarClassifierBuilder.trainAndPackage(directory, optArray);
  }

  @Override
  protected AnnotationStatistics<Boolean> test(
      CollectionReader collectionReader, File directory) throws Exception {
    AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
    aggregateBuilder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DocumentIDPrinter.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(DeterministicMarkableAnnotator.class));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(RemovePersonMarkables.class));
    
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(ViewCreatorAnnotator.class, ViewCreatorAnnotator.PARAM_VIEW_NAME, "PseudoGold"));
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(CreatePseudoGoldMarkables.class, CreatePseudoGoldMarkables.PARAM_GOLD_VIEW, GOLD_VIEW_NAME, CreatePseudoGoldMarkables.PARAM_PSEUDO_GOLD_VIEW, "PseudoGold"));
    aggregateBuilder.add(MarkableSalienceAnnotator.createAnnotatorDescription(directory.getAbsolutePath() + File.separator + "model.jar"));
    AnnotationStatistics<Boolean> stats = new AnnotationStatistics<>();
    
    for(Iterator<JCas> casIter = new JCasIterator(collectionReader, aggregateBuilder.createAggregate()); casIter.hasNext();){
      JCas jCas = casIter.next();
      JCas goldView = jCas.getView("PseudoGold");
      JCas systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      
      stats.add(JCasUtil.select(goldView, Markable.class),
          JCasUtil.select(systemView, Markable.class),
          AnnotationStatistics.<Markable>annotationToSpan(),
          mapConfidenceToBoolean());      
    }
    
    
    return stats;
  }
  
  public static class SetGoldConfidence extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

    public static final String PARAM_GOLD_VIEW = "GoldViewName";
    @ConfigurationParameter(name=PARAM_GOLD_VIEW, mandatory=true, description="View containing gold standard annotations")
    private String goldViewName;
    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      JCas goldView = null;
      try {
        goldView = jcas.getView(goldViewName);
      } catch (CASException e) {
        e.printStackTrace();
        throw new AnalysisEngineProcessException(e);
      }
      
      Map<ConllDependencyNode,Collection<Markable>> depIndex = JCasUtil.indexCovering(jcas, ConllDependencyNode.class, Markable.class);
      
      // iterate over every gold coreference chain
      for(CollectionTextRelation goldChain : JCasUtil.select(goldView, CollectionTextRelation.class)){
        FSList head = goldChain.getMembers();
        
        // iterate over every gold markable in the chain
        // first one is guaranteed to be nonempty otherwise it would not be in cas
        do{
          NonEmptyFSList element = (NonEmptyFSList) head;
          Markable goldMarkable = (Markable) element.getHead();
          if(!(goldMarkable.getBegin() < 0 || goldMarkable.getEnd() >= jcas.getDocumentText().length())){
            // get the head of this markable, then check if there are any system markables with the same
            // head, and if so, that markable is "true" for being coreferent, AKA high confidence.
            ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jcas, goldMarkable);

            for(Markable sysMarkable : depIndex.get(headNode)){
              ConllDependencyNode markNode = DependencyUtility.getNominalHeadNode(jcas, sysMarkable);
              if(markNode == headNode){
                sysMarkable.setConfidence(1.0f);
                break;
              }
            }
          }
          head = element.getTail();
        }while(head instanceof NonEmptyFSList);
      }
    }
  }
  
  public static class CreatePseudoGoldMarkables extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

    public static final String PARAM_PSEUDO_GOLD_VIEW = "PseudoViewName";
    @ConfigurationParameter(name = PARAM_PSEUDO_GOLD_VIEW)
    private String fakeGoldName;
    
    public static final String PARAM_GOLD_VIEW = "GoldViewName";
    @ConfigurationParameter(name = PARAM_GOLD_VIEW)
    private String goldViewName;
    
    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      JCas fakeView = null;
      JCas goldView = null;
      
      try{
        fakeView = jcas.getView(fakeGoldName);
        goldView = jcas.getView(goldViewName);
      }catch(CASException e){
        throw new AnalysisEngineProcessException(e);
      }
      // create a set of markables that map to gold
      Set<Markable> sys = new HashSet<>();
      Map<ConllDependencyNode,Collection<Markable>> depIndex = JCasUtil.indexCovering(jcas, ConllDependencyNode.class, Markable.class);
      
      // iterate over every gold coreference chain
      for(CollectionTextRelation goldChain : JCasUtil.select(goldView, CollectionTextRelation.class)){
        FSList head = goldChain.getMembers();
        
        // iterate over every gold markable in the chain
        // first one is guaranteed to be nonempty otherwise it would not be in cas
        do{
          NonEmptyFSList element = (NonEmptyFSList) head;
          Markable goldMarkable = (Markable) element.getHead();
          if(!(goldMarkable.getBegin() < 0 || goldMarkable.getEnd() >= jcas.getDocumentText().length())){
            // get the head of this markable, then check if there are any system markables with the same
            // head, and if so, that markable is "true" for being coreferent, AKA high confidence.
            ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jcas, goldMarkable);

            for(Markable sysMarkable : depIndex.get(headNode)){
              ConllDependencyNode markNode = DependencyUtility.getNominalHeadNode(jcas, sysMarkable);
              if(markNode == headNode){
                sys.add(sysMarkable);
                break;
              }
            }
          }
          head = element.getTail();
        }while(head instanceof NonEmptyFSList);
      }
      
      // add all system markables to psuedo-gold and with confidence based on whether they map
      for(Markable markable : JCasUtil.select(jcas, Markable.class)){
        Markable fakeMarkable = new Markable(fakeView, markable.getBegin(), markable.getEnd());
        
        if(sys.contains(markable)){
          fakeMarkable.setConfidence(1.0f);
        }else{
          fakeMarkable.setConfidence(0.0f);
        }
        fakeMarkable.addToIndexes();
      } 
    }
  }
  
  // this is predicting non-singletons rather than singletons
  public static Function<Markable,Boolean> mapConfidenceToBoolean(){
    return new Function<Markable,Boolean>() {
      public Boolean apply(Markable markable) {
        return markable.getConfidence() > 0.5;
      }
    };
  }
}
