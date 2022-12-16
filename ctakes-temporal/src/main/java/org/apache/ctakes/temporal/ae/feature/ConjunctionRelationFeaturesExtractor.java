package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

public class ConjunctionRelationFeaturesExtractor implements
RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {
	
	private String name = "ConjunctionFeature";
	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		ArrayList<Feature> feats = new ArrayList<>();
		
		int begin = arg1.getEnd();
		int end   = arg2.getBegin();
		
		if ( begin > end ){
			begin = arg2.getEnd();
			end   = arg1.getBegin();
		}
		
		if ( begin >= end ){
			return feats;
		}
		
		List<BaseToken> betweenTokens = JCasUtil.selectCovered(jCas, BaseToken.class, begin, end);
		List<EventMention> eventsInBetween = JCasUtil.selectCovered(jCas, EventMention.class, begin, end);
		//filter events:
		List<EventMention> realEvents = new ArrayList<>();
		//filtering events
		for(EventMention event : eventsInBetween){
			// filter out ctakes events
			if(event.getClass().equals(EventMention.class)){
				realEvents.add(event);
			}
		}
		eventsInBetween = realEvents;
		if(eventsInBetween.size() ==0 ){
			for (BaseToken token: betweenTokens){
				String pos = token.getPartOfSpeech();
				if(pos == null) continue;
				if(pos.startsWith("CC")||pos.equals(",")||pos.startsWith("IN")){
					feats.add(new Feature(this.name, "Contain_Conjunction_inBetween"));
					feats.add(new Feature(this.name, pos));
				}
			}
		}
		
		return feats;
	}

}
