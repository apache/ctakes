package org.apache.ctakes.temporal.ae;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.ae.TemporalRelationExtractorAnnotator.IdentifiedAnnotationPair;
import org.apache.ctakes.temporal.nn.data.ArgContextProvider;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
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
		name = "E-E CNN TLinker",
		description = "Creates Event - Event TLinks with Convolutional Neural Network.",
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE, PipeBitInfo.TypeProduct.EVENT },
		products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class EventEventCNNAnnotator extends CleartkAnnotator<String> {

	public static final String NO_RELATION_CATEGORY = "none";

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

//		Map<EventMention, Collection<EventMention>> coveringMap =
//				JCasUtil.indexCovering(jCas, EventMention.class, EventMention.class);

		//get all gold relation lookup
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
					System.err.println("Error! This attempted relation " + relation.getCategory() + " already has a relation " + reln + " at this span: " + arg1.getCoveredText() + " -- " + arg2.getCoveredText());
				}else{
					relationLookup.put(key, relation);
				}
			}
		}

		// go over sentences, extracting event-time relation instances
		for(Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
			// collect all relevant relation arguments from the sentence
			List<IdentifiedAnnotationPair> candidatePairs =
					getCandidateRelationArgumentPairs(jCas, sentence);

			// walk through the pairs of annotations
			for (IdentifiedAnnotationPair pair : candidatePairs) {
				IdentifiedAnnotation arg1 = pair.getArg1();
				IdentifiedAnnotation arg2 = pair.getArg2();

				String context;
				if(arg2.getBegin() < arg1.getBegin()) {
					// ... time ... event ... scenario
					context = ArgContextProvider.getTokenContext(jCas, sentence, arg2, "e", arg1, "e", 5); 
//					context = getTokensBetweenExpanded(jCas, sentence, arg2, "e", arg1, "e", 5, coveringMap);
				} else {
					// ... event ... time ... scenario
					context = ArgContextProvider.getTokenContext(jCas, sentence, arg1, "e", arg2, "e", 5);
//					context = getTokensBetweenExpanded(jCas, sentence, arg1, "e", arg2, "e", 5, coveringMap);
				}

				//derive features based on context:
				List<Feature> feats = new ArrayList<>();
				String[] tokens = context.split(" ");
				for (String token: tokens){
					feats.add(new Feature(token.toLowerCase()));
				}

				// during training, feed the features to the data writer
				if (this.isTraining()) {
					String category = getRelationCategory(relationLookup, arg1, arg2);
					if (category == null) {
						category = NO_RELATION_CATEGORY;
					}else{
						category = category.toLowerCase();
					}
					this.dataWriter.write(new Instance<>(category, feats));
				}else {
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
	 * Print context from left to right.
	 * @param contextSize number of tokens to include on the left of arg1 and on the right of arg2
	 */
	public static String getTokensBetweenExpanded(
			JCas jCas, 
			Sentence sent, 
			Annotation left,
			String leftType,
			Annotation right,
			String rightType,
			int contextSize,
			Map<EventMention, Collection<EventMention>> coveringMap) {

		boolean leftIsExpanded = false;
		Annotation longerLeft = left;
		if(left instanceof EventMention){
			longerLeft = getLongerEvent(coveringMap, left);
			if(longerLeft != left){
				leftIsExpanded = true;
			}
		}

		boolean rightIsExpanded = false;
		Annotation longerRight = right;
		if(right instanceof EventMention){
			longerRight = getLongerEvent(coveringMap, right);
			if(longerRight != right){
				rightIsExpanded = true;
			}
		}

		List<String> tokens = new ArrayList<>();
		if(leftIsExpanded){
			for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, longerLeft, contextSize)) {
				if(sent.getBegin() <= baseToken.getBegin()) {
					tokens.add(baseToken.getCoveredText()); 
				}
			}
		}else{
			for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, left, contextSize)) {
				if(sent.getBegin() <= baseToken.getBegin()) {
					tokens.add(baseToken.getCoveredText()); 
				}
			}
		}
		tokens.add("<" + leftType + ">");
		tokens.add(left.getCoveredText());
		tokens.add("</" + leftType + ">");
		if(leftIsExpanded){
			if(rightIsExpanded){
				for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, longerLeft, longerRight)) {
					tokens.add(baseToken.getCoveredText());
				}
			}else{
				for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, longerLeft, right)) {
					tokens.add(baseToken.getCoveredText());
				}
			}
		}else if(rightIsExpanded){
			for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, longerRight)) {
				tokens.add(baseToken.getCoveredText());
			}
		}else{
			for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, right)) {
				tokens.add(baseToken.getCoveredText());
			}
		}
		tokens.add("<" + rightType + ">");
		tokens.add(right.getCoveredText());
		tokens.add("</" + rightType + ">");
		if(rightIsExpanded){
			for(BaseToken baseToken : JCasUtil.selectFollowing(jCas, BaseToken.class, longerRight, contextSize)) {
				if(baseToken.getEnd() <= sent.getEnd()) {
					tokens.add(baseToken.getCoveredText());
				}
			}
		}else{
			for(BaseToken baseToken : JCasUtil.selectFollowing(jCas, BaseToken.class, right, contextSize)) {
				if(baseToken.getEnd() <= sent.getEnd()) {
					tokens.add(baseToken.getCoveredText());
				}
			}
		}

		return String.join(" ", tokens).replaceAll("[\r\n]", " ");
	}

	private static Annotation getLongerEvent(Map<EventMention, Collection<EventMention>> coveringMap,
			Annotation event) {
		int maxSpan = getSpan(event);
		Annotation longerEvent = event;
		Collection<EventMention> eventList = coveringMap.get(event);
		for(EventMention covEvent : eventList){
			int span = getSpan(covEvent);
			if(span > maxSpan){
				maxSpan = span;
				longerEvent = covEvent;
			}
		}
		return longerEvent;
	}

	private static int getSpan(Annotation left) {
		return (left.getEnd()-left.getBegin());
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
			if(arg2.getBegin() < arg1.getBegin()){
				category = relation.getCategory() + "-1";
			}else{
				category = relation.getCategory();
			}
		} else {
			relation = relationLookup.get(Arrays.asList(arg2, arg1));
			if (relation != null) {
				if(arg2.getBegin() < arg1.getBegin()){
					category = relation.getCategory();
				}else{
					category = relation.getCategory() + "-1";
				}
			}
		}

		return category;
	}

	/** Dima's way of getting lables
	 * @param relationLookup
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	//	protected String getRelationCategory(Map<List<Annotation>, BinaryTextRelation> relationLookup,
	//			IdentifiedAnnotation arg1,
	//			IdentifiedAnnotation arg2){
	//		BinaryTextRelation relation = relationLookup.get(Arrays.asList(arg1, arg2));
	//		String category = null;
	//		if (relation != null) {
	//			category = relation.getCategory();
	//			if(arg1 instanceof EventMention){
	//				category = category + "-1";
	//			}
	//		} else {
	//			relation = relationLookup.get(Arrays.asList(arg2, arg1));
	//			if (relation != null) {
	//				category = relation.getCategory();
	//				if(arg2 instanceof EventMention){
	//					category = category + "-1";
	//				}
	//			}
	//		}
	//
	//		return category;
	//
	//	}


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
//		Map<EventMention, Collection<EventMention>> coveringMap =
//				JCasUtil.indexCovering(jCas, EventMention.class, EventMention.class);

		List<IdentifiedAnnotationPair> pairs = Lists.newArrayList();
		List<EventMention> events = new ArrayList<>(JCasUtil.selectCovered(jCas, EventMention.class, sentence));
		//filter events:
		List<EventMention> realEvents = Lists.newArrayList();
		for( EventMention event : events){
			if(event.getClass().equals(EventMention.class)){
				realEvents.add(event);
			}
		}
		events = realEvents;

		int eventNum = events.size();

		for (int i = 0; i < eventNum-1; i++){
			for(int j = i+1; j < eventNum; j++){
				EventMention eventA = events.get(i);
				EventMention eventB = events.get(j);

//				boolean eventAMedical = false;
//				for( EventMention aEve : JCasUtil.selectCovering(jCas, EventMention.class, eventA)){
//					if(!aEve.getClass().equals(EventMention.class)){//this event cover a UMLS semantic type
//						eventAMedical = true;
//						break;
//					}
//				}
//
//				boolean eventBMedical = false;
//				for( EventMention bEve : JCasUtil.selectCovering(jCas, EventMention.class, eventB)){
//					if(!bEve.getClass().equals(EventMention.class)){//this event cover a UMLS semantic type
//						eventBMedical = true;
//						break;
//					}
//				}

//				if(this.isTraining()){
//					//pairing covering system events:
//					if(eventAMedical || eventBMedical){
//						for (EventMention event1 : coveringMap.get(eventA)){
//							if(!hasOverlap(event1,eventB)){//don't generate overlapping arguments
//								pairs.add(new IdentifiedAnnotationPair(event1, eventB));
//							}
//
//							for(EventMention event2 : coveringMap.get(eventB)){
//								if(!hasOverlap(event1,event2)){//don't generate overlapping arguments
//									pairs.add(new IdentifiedAnnotationPair(event1, event2));
//								}
//							}
//						}
//						for(EventMention event2 : coveringMap.get(eventB)){
//							if(!hasOverlap(eventA,event2)){//don't generate overlapping arguments
//								pairs.add(new IdentifiedAnnotationPair(eventA, event2));
//							}
//						}
//					}
//				}
				pairs.add(new IdentifiedAnnotationPair(eventA, eventB));

			}
		}

		return pairs;
	}
	
	private static boolean hasOverlap(Annotation event1, Annotation event2) {
		if(event1.getEnd()>=event2.getBegin()&&event1.getEnd()<=event2.getEnd()){
			return true;
		}
		if(event2.getEnd()>=event1.getBegin()&&event2.getEnd()<=event1.getEnd()){
			return true;
		}
		return false;
	}

}
