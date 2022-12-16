package org.apache.ctakes.coreference.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.coreference.ae.features.salience.ClinicalFeatureExtractor;
import org.apache.ctakes.coreference.ae.features.salience.GrammaticalRoleFeatureExtractor;
import org.apache.ctakes.coreference.ae.features.salience.MorphosyntacticFeatureExtractor;
import org.apache.ctakes.coreference.ae.features.salience.SemanticEnvironmentFeatureExtractor;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PipeBitInfo(
      name = "Markable Salience Annotator",
      description = "Annotates Markable Salience.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.PARAGRAPH, PipeBitInfo.TypeProduct.SENTENCE,
            PipeBitInfo.TypeProduct.MARKABLE, PipeBitInfo.TypeProduct.DEPENDENCY_NODE },
      usables = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class MarkableSalienceAnnotator extends CleartkAnnotator<Boolean> {

  static private final Logger LOGGER = Logger.getLogger( "MarkableSalienceAnnotator" );

  List<FeatureExtractor1<Markable>> extractors = new ArrayList<>();
  
  public static AnalysisEngineDescription createDataWriterDescription(
      Class<? extends DataWriter<Boolean>> dataWriterClass,
      File outputDirectory) throws ResourceInitializationException{
    return AnalysisEngineFactory.createEngineDescription(
        MarkableSalienceAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        true,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
        dataWriterClass,
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        outputDirectory);
  }
  
  public static AnalysisEngineDescription createAnnotatorDescription(String modelPath) throws ResourceInitializationException{
    return AnalysisEngineFactory.createEngineDescription(
        MarkableSalienceAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING,
        false,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        modelPath);
  }
  
  @Override
  public void initialize( final UimaContext context ) throws ResourceInitializationException {
    LOGGER.info( "Initializing ..." );
    super.initialize( context );

    extractors.add( new MorphosyntacticFeatureExtractor() );
    extractors.add( new GrammaticalRoleFeatureExtractor() );
    extractors.add( new SemanticEnvironmentFeatureExtractor() );
    extractors.add( new ClinicalFeatureExtractor() );
    LOGGER.info( "Finished." );
  }
  
  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {
    LOGGER.info( "Processing ..." );
    for(Markable markable : JCasUtil.select(jcas, Markable.class)){
      boolean outcome;
      List<Feature> features = new ArrayList<>();
      for(FeatureExtractor1<Markable> extractor : extractors){
        features.addAll(extractor.extract(jcas, markable));
      }
      Instance<Boolean> instance = new Instance<>(features);
      
      if(this.isTraining()){
        outcome = markable.getConfidence() > 0.5;
        instance.setOutcome(outcome);
        this.dataWriter.write(instance);
      }else{
        Map<Boolean,Double> outcomes = this.classifier.score(features);
        markable.setConfidence(outcomes.get(true).floatValue());
      }
    }
    LOGGER.info( "Finished." );
  }
}
