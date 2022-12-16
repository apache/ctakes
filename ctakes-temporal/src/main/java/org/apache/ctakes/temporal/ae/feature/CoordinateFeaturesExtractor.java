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

public class CoordinateFeaturesExtractor implements
RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation> {

	@SuppressWarnings("null")
	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) throws AnalysisEngineProcessException {
		ArrayList<Feature> feats = new ArrayList<>();
		
		List<BaseToken> arg1Tokens = JCasUtil.selectCovered(jCas, BaseToken.class, arg1);
		List<BaseToken> arg2Tokens = JCasUtil.selectCovered(jCas, BaseToken.class, arg2);
		
		int arg1Length = arg1Tokens == null ? 0 : arg1Tokens.size();
		int arg2Length = arg2Tokens == null ? 0 : arg2Tokens.size();
		
		if(arg1Length != arg2Length || arg1Length == 0 || arg2Length == 0){
			return feats;
		}
		
		for (int i=0; i<arg1Length; i++){
			if(!arg1Tokens.get(i).getPartOfSpeech().equals(arg2Tokens.get(i).getPartOfSpeech())){
				return feats;
			}
		}
		
		int begin = arg1.getEnd();
		int end   = arg2.getBegin();
		
		if ( begin > end ){
			begin = arg2.getEnd();
			end   = arg1.getBegin();
		}
		
		if ( begin >= end ){
			return feats;
		}
		
		List<EventMention> betweenEvents = JCasUtil.selectCovered(jCas, EventMention.class, begin, end);
		int eventInBetween  = betweenEvents == null ? 0 : betweenEvents.size();
		if(eventInBetween >0) return feats;
		
		List<BaseToken> betweenTokens = JCasUtil.selectCovered(jCas, BaseToken.class, begin, end);
		for (BaseToken token: betweenTokens){
			String tokenwd = token.getCoveredText();
			if(token.getPartOfSpeech().startsWith("CC")||tokenwd.equals(",")||tokenwd.equals(";")||tokenwd.equals("/")||tokenwd.equals("w")||tokenwd.equals("&")||tokenwd.equalsIgnoreCase("versus")||tokenwd.equalsIgnoreCase("vs")||tokenwd.equalsIgnoreCase("with")){
				feats.add(new Feature("Coordinate_feature", "Coordinate"));
				break;
			}
		}
		
		return feats;
	}

}
