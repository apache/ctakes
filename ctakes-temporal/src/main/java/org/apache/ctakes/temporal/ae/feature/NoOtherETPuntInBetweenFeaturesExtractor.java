package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.features.TokenFeaturesExtractor;
import org.apache.ctakes.typesystem.type.syntax.PunctuationToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.apache.uima.fit.util.JCasUtil;

/**
 * Extract the feature to tell if there is no event or timex in between two arguments
 * @author CH151862
 *
 */
public class NoOtherETPuntInBetweenFeaturesExtractor extends TokenFeaturesExtractor {

	@Override
	public List<Feature> extract(JCas jCas, IdentifiedAnnotation mention1, IdentifiedAnnotation mention2)
			throws AnalysisEngineProcessException {
		List<Feature> features = new ArrayList<>();
		
		String eventFeatName = "noOtherEventsInBetween";
		String timexFeatName = "noOtherTimexsInBetween";
		String punctFeatName = "noPunctuationInBetween";

		int sizeLimit = 0;
		
		List<EventMention> events = JCasUtil.selectBetween(jCas, EventMention.class, mention1, mention2);
		List<TimeMention> times   = JCasUtil.selectBetween(jCas, TimeMention.class, mention1, mention2);
		List<PunctuationToken> puncts = JCasUtil.selectBetween(jCas, PunctuationToken.class, mention1, mention2);
		if(events.size()<=sizeLimit){
			features.add(new Feature(eventFeatName, true));
		}
		if(times.size()<=sizeLimit){
			features.add(new Feature(timexFeatName, true));
		}
		if(puncts.size()<=sizeLimit){
			features.add(new Feature(punctFeatName, true));
		}
		return features;
	}

}
