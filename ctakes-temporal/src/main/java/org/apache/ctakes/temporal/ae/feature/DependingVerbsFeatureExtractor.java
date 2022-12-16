package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

public class DependingVerbsFeatureExtractor implements RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	private static final String FEAT_NAME = "Depending_Verb"; 

	@Override
	public List<Feature> extract(JCas jcas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		List<Feature> features = new ArrayList<>();
		// first get the common ancestor of both arguments...
		ConllDependencyNode ancestor = null;
		boolean hasCommonVerb = false;
		outerloop:
		for (ConllDependencyNode firstNode : JCasUtil.selectCovered(jcas, ConllDependencyNode.class, arg1)) {//get the covered conll nodes within the first extent
			String pos = firstNode.getPostag();
			if(pos!=null && pos.startsWith("NN")){//get the head node
				for(ConllDependencyNode nextNode : JCasUtil.selectCovered(jcas, ConllDependencyNode.class, arg2)){//get the covered conll nodes within the next extent
					pos = nextNode.getPostag();
					if(pos!=null && pos.startsWith("NN")){//get the head node
						ancestor = DependencyParseUtils.getCommonAncestor(firstNode, nextNode);
						if(ancestor!=null && ancestor.getPostag().startsWith("VB")){
							features.add(new Feature(FEAT_NAME, "both_events_governed_by_the_same_verb"));
							features.add(new Feature(FEAT_NAME, ancestor.getDeprel()+"_"+ancestor.getCoveredText().toLowerCase()));
							hasCommonVerb = true;
							break outerloop;
						}
					}
				}
			}
		}

		if(!hasCommonVerb){//if arg1 and arg2 don't depend on the same verb, get their independent depending verbs
			features.addAll(getDependingVerbFeature(jcas, arg1));
			features.addAll(getDependingVerbFeature(jcas, arg2));
		}
		
		return features;
	}

	private static List<Feature> getDependingVerbFeature(JCas jcas, IdentifiedAnnotation arg) {
		List<Feature> feats = new ArrayList<>();
		
		for (ConllDependencyNode dnode : JCasUtil.selectCovered(jcas, ConllDependencyNode.class, arg)) {//get the covered conll nodes within the first extent
			String pos = dnode.getPostag();
			if(pos!=null && pos.startsWith("NN")){//get the head node
				ConllDependencyNode verbNode = getVerbAncestor(dnode);
				if(verbNode != null){//get verb node features
					Integer typeId = -1;
					if(arg instanceof EventMention){
						EventMention mention = (EventMention) arg;
						if(mention.getEvent() != null && mention.getEvent().getProperties() != null){
							typeId = mention.getEvent().getProperties().getTypeIndexID();
						}
					}
					feats.add(new Feature(FEAT_NAME, typeId+"_"+ verbNode.getPostag()));
					String depRelation = dnode.getDeprel();
					if(depRelation.startsWith("nsubj"))
						feats.add(new Feature(FEAT_NAME, typeId+"_isASubject"));
					else if(depRelation.startsWith("dobj"))
						feats.add(new Feature(FEAT_NAME, typeId+"_isAnObject"));
					
					break;
				}
			}
		}
		return feats;
	}

	private static ConllDependencyNode getVerbAncestor(ConllDependencyNode dnode) {
		ConllDependencyNode verbNode = null;
		ConllDependencyNode currNode = dnode;
	    while (currNode != null && currNode.getHead() != null) { 
	      currNode = currNode.getHead();
	      String nodepos = currNode == null ? null : currNode.getPostag();
	      if(nodepos != null && nodepos.startsWith("VB")){
	    	  verbNode = currNode;
	    	  return verbNode;
	      }
	    }
		return verbNode;
	}

	
}
