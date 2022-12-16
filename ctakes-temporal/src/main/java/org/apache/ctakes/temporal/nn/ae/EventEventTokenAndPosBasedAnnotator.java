package org.apache.ctakes.temporal.nn.ae;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.ae.TemporalRelationExtractorAnnotator.IdentifiedAnnotationPair;
import org.apache.ctakes.temporal.nn.data.ArgContextProvider;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.util.ViewUriUtil;

import com.google.common.collect.Lists;

@PipeBitInfo(
      name = "E-E POS and Token TLinker",
      description = "Creates Event - Event TLinks from Part of Speech and Token Type.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
            PipeBitInfo.TypeProduct.EVENT },
      products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class EventEventTokenAndPosBasedAnnotator extends CleartkAnnotator<String> {

  public static final String NO_RELATION_CATEGORY = "none";
  // private Random coin = new Random(0);

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {

    // get all gold relation lookup
    Map<List<Annotation>, BinaryTextRelation> relationLookup;
    relationLookup = new HashMap<>();
    if (this.isTraining()) {
      relationLookup = new HashMap<>();
      for (BinaryTextRelation relation : JCasUtil.select(jCas, BinaryTextRelation.class)) {
        Annotation arg1 = relation.getArg1().getArgument();
        Annotation arg2 = relation.getArg2().getArgument();
        // The key is a list of args so we can do bi-directional lookup
        List<Annotation> key = Arrays.asList(arg1, arg2);
        if(relationLookup.containsKey(key)){
          String reln = relationLookup.get(key).getCategory();
          System.err.println("Error in: "+ ViewUriUtil.getURI(jCas).toString());
          System.err.println("Error! This attempted relation " + relation.getCategory() + 
              " already has a relation " + reln + " at this span: " + 
              arg1.getCoveredText() + " -- " + arg2.getCoveredText());
        } else {
          relationLookup.put(key, relation);
        }
      }
    }

    for(Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
      // collect all relevant relation arguments from the sentence
      List<IdentifiedAnnotationPair> candidatePairs = getCandidateRelationArgumentPairs(jCas, sentence);

      // walk through the pairs of annotations
      for (IdentifiedAnnotationPair pair : candidatePairs) {
        IdentifiedAnnotation arg1 = pair.getArg1();
        IdentifiedAnnotation arg2 = pair.getArg2();

        String tokenContext;
        String posContext;
        if(arg2.getBegin() < arg1.getBegin()) {
          // ... event2 ... event1 ... scenario
          System.out.println("\n-------------- THIS NEVER NAPPENS ------------\n");
          tokenContext = ArgContextProvider.getTokenContext(jCas, sentence, arg2, "e2", arg1, "e1", 2); 
          posContext = ArgContextProvider.getPosContext(jCas, sentence, arg2, "e2", arg1, "e1", 2); 
        } else {
          // ... event1 ... event2 ... scenario
          tokenContext = ArgContextProvider.getTokenContext(jCas, sentence, arg1, "e1", arg2, "e2", 2);
          posContext = ArgContextProvider.getPosContext(jCas, sentence, arg1, "e1", arg2, "e2", 2);
        }

        //derive features based on context:
        List<Feature> feats = new ArrayList<>();
        String[] tokens = (tokenContext + "|" + posContext).split(" ");
        for (String token: tokens){
          feats.add(new Feature(token.toLowerCase()));
        }

        // during training, feed the features to the data writer
        if(this.isTraining()) {
          String category = getRelationCategory(relationLookup, arg1, arg2);
          
          // drop some portion of negative examples during training
          // if(category == null && coin.nextDouble() <= 0.5) {
          //   continue; // skip this negative example
          // }
          
          if(category == null) {
            category = NO_RELATION_CATEGORY;
          } else{
            category = category.toLowerCase();
          }
          this.dataWriter.write(new Instance<>(category, feats));
        } else {
          String predictedCategory = this.classifier.classify(feats);

          // add a relation annotation if a true relation was predicted
          if (predictedCategory != null && !predictedCategory.equals(NO_RELATION_CATEGORY)) {

            // if we predict an inverted relation, reverse the order of the
            // arguments
            if (predictedCategory.endsWith("-1")) {
              predictedCategory = predictedCategory.substring(0, predictedCategory.length() - 2);
              IdentifiedAnnotation temp = arg1;
              arg1 = arg2;
              arg2 = temp;
            }

            createRelation(jCas, arg1, arg2, predictedCategory.toUpperCase(), 0.0);
          }
        }
      }

    }
  }

  /**
   * original way of getting label
   * @param relationLookup
   * @param arg1
   * @param arg2
   * @return
   */
  protected String getRelationCategory(
      Map<List<Annotation>, BinaryTextRelation> relationLookup,
      IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2) {

    BinaryTextRelation relation = relationLookup.get(Arrays.asList(arg1, arg2));
    String category = null;
    if (relation != null) {
      if (arg2.getBegin() < arg1.getBegin()) {
        category = relation.getCategory() + "-1";
      } else {
        category = relation.getCategory();
      }
    } else {
      relation = relationLookup.get(Arrays.asList(arg2, arg1));
      if (relation != null) {
        if(arg2.getBegin() < arg1.getBegin()){
          category = relation.getCategory();
        } else {
          category = relation.getCategory() + "-1";
        }
      }
    }

    return category;
  }

  protected String getRelationCategory2(Map<List<Annotation>, BinaryTextRelation> relationLookup,
      IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2) {
    
    // gold view representation (i.e. only contains relations)
    BinaryTextRelation arg1ContainsArg2 = relationLookup.get(Arrays.asList(arg1, arg2));
    BinaryTextRelation arg2ContainsArg1 = relationLookup.get(Arrays.asList(arg2, arg1));

    // now translate to position dependent representation (i.e. contains and contains-1)
    if(arg1ContainsArg2 != null) {
      // still need to know whether it's arg1 ... arg2 or arg2 ... arg1
      // because that determines whether it's contains or contains-1
      if(arg1.getBegin() < arg2.getBegin()) {
        return arg1ContainsArg2.getCategory();
      } else {
        return arg1ContainsArg2.getCategory() + "-1";
      }
    } else if(arg2ContainsArg1 != null) {
      if(arg1.getBegin() < arg2.getBegin()) {
        return arg2ContainsArg1.getCategory() + "-1";
      } else {
        return arg2ContainsArg1.getCategory();
      }
    } else {
      return null;      
    }
  }

  protected void createRelation(JCas jCas, IdentifiedAnnotation arg1,
      IdentifiedAnnotation arg2, String predictedCategory, double confidence) {
    RelationArgument relArg1 = new RelationArgument(jCas);
    relArg1.setArgument(arg1);
    relArg1.setRole("Arg1");
    relArg1.addToIndexes();
    RelationArgument relArg2 = new RelationArgument(jCas);
    relArg2.setArgument(arg2);
    relArg2.setRole("Arg2");
    relArg2.addToIndexes();
    TemporalTextRelation relation = new TemporalTextRelation(jCas);
    relation.setArg1(relArg1);
    relation.setArg2(relArg2);
    relation.setCategory(predictedCategory);
    relation.setConfidence(confidence);
    relation.addToIndexes();
  }

  private static List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(JCas jCas, Sentence sentence) {

    List<IdentifiedAnnotationPair> pairs = Lists.newArrayList();
    List<EventMention> events = new ArrayList<>(JCasUtil.selectCovered(jCas, EventMention.class, sentence));
    // filter events
    List<EventMention> realEvents = Lists.newArrayList();
    for( EventMention event : events){
      if(event.getClass().equals(EventMention.class)){
        realEvents.add(event);
      }
    }
    events = realEvents;

    int eventNum = events.size();
    for (int i = 0; i < eventNum-1; i++) {
      for(int j = i+1; j < eventNum; j++) {
        EventMention eventA = events.get(i);
        EventMention eventB = events.get(j);
        pairs.add(new IdentifiedAnnotationPair(eventA, eventB));
      }
    }

    return pairs;
  }
}
