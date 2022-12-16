package org.apache.ctakes.coreference.ae.features.cluster;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;

public class MentionClusterSemTypeDepPrefsFeatureExtractor implements RelationFeaturesExtractor<CollectionTextRelation,IdentifiedAnnotation> {

  private HashMap<String,HashMap<String,Double>> probs = new HashMap<>();
  
  public MentionClusterSemTypeDepPrefsFeatureExtractor() throws FileNotFoundException {
    try(Scanner scanner = new Scanner(FileLocator.getAsStream("org/apache/ctakes/coreference/pref_probs.txt"))){
    	while(scanner.hasNextLine()){
    		String line = scanner.nextLine().trim();
    		String[] parts = line.split("\t");
    		if(!probs.containsKey(parts[0])){
    			probs.put(parts[0], new HashMap<String,Double>());
    		}
    		probs.get(parts[0]).put(parts[1], Double.parseDouble(parts[2]));
    	}
    }
    
  }
  
  @Override
  public List<Feature> extract(JCas jcas, CollectionTextRelation cluster,
      IdentifiedAnnotation mention) throws AnalysisEngineProcessException {
    List<Feature> feats = new ArrayList<>();
    double maxProb = 0.0;
    String mentionText = mention.getCoveredText().toLowerCase();
    
    if(mentionText.equals("this") || mentionText.equals("it") || mentionText.equals("that")){
      ConllDependencyNode head = DependencyUtility.getNominalHeadNode(jcas, mention);
      String key = head.getHead().getCoveredText().toLowerCase() + "::" + head.getDeprel();
      Map<String,Double> semProbs = probs.get(key);
      if(semProbs == null) return feats;

      for(Markable m : JCasUtil.select(cluster.getMembers(), Markable.class)){
        if(mention.getBegin() < m.getEnd()){
          // during training this might happen -- see a member of a cluster that
          // is actually subsequent to the candidate mention
          continue;
        }
        List<IdentifiedAnnotation> ents = JCasUtil.selectCovering(jcas, IdentifiedAnnotation.class, m);
        for(IdentifiedAnnotation ent : ents){
          String semKey = ent.getClass().getSimpleName();
          if(semProbs.containsKey(semKey)){
            double prob = semProbs.get(semKey);
            if(prob > maxProb) maxProb = prob;
          }
        }
      }
      feats.add(new Feature("InferredSemTypeMaxProb", maxProb));
    }
    return feats;
  }

}
