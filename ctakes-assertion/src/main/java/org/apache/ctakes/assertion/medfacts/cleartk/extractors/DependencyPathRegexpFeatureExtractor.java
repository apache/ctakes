package org.apache.ctakes.assertion.medfacts.cleartk.extractors;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.dependency.parser.util.DependencyPath;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

public class DependencyPathRegexpFeatureExtractor implements
    FeatureExtractor1<IdentifiedAnnotation> {

  HashMap<String,Integer> patts = new HashMap<>();
  
  public DependencyPathRegexpFeatureExtractor() throws FileNotFoundException{
    File pathFile = FileLocator.getFile("org/apache/ctakes/assertion/models/uncDepPathRegexps.txt");
    Scanner scanner = new Scanner(pathFile);
    while(scanner.hasNextLine()){
      String[] featAndWeight = scanner.nextLine().trim().split("\t");
      String feat = featAndWeight[0];
      Double weight = Double.parseDouble(featAndWeight[1]);
      int val;
      val = patts.size(); // one feat per pattern
//      val = 1; // map all to same feat
//      val = feat.split("[<>]").length;  // different feats for bi-,tri-,4-gram features
//      val = (int) Math.round(Math.log(weight));
//      if(val > 3){
        patts.put(feat, val);
//      }
    }
    scanner.close();
  }
  
  public List<Feature> extract(JCas jcas, IdentifiedAnnotation mention)
      throws CleartkExtractorException {
    List<Feature> feats = new ArrayList<>();
    int sentWeight = 0;
    ConllDependencyNode node = DependencyUtility.getNominalHeadNode(jcas, mention);
    List<ConllDependencyNode> sentNodes = DependencyUtility.getDependencyNodes(jcas, DependencyUtility.getSentence(jcas, node));
    for(ConllDependencyNode neighborNode : sentNodes){
      if(node == neighborNode) continue;
      DependencyPath path = DependencyUtility.getPath(sentNodes, node, neighborNode);
      String pathString = path.toString().replace('\n', ' ').replaceFirst("\\{[^\\}]+\\}", "{CONCEPT}").replace(' ', '_');
      if(patts.containsKey(pathString)){
//        sentWeight += patts.get(pathString);
        feats.add(new Feature("DepPathRegexp" + patts.get(pathString), true));    // one feat per pattern
      }
//      out.println("dep: " + path.toString().replace('\n', ' ').replaceFirst("\\{[^\\}]+\\}", "{CONCEPT}"));
    }
//    feats.add(new Feature("DepPathRegexp", sentWeight));
    return feats;
  }

}
