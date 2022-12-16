package org.apache.ctakes.temporal.ae;

import com.google.common.collect.Lists;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.relationextractor.ae.features.PartOfSpeechFeaturesExtractor;
import org.apache.ctakes.relationextractor.ae.features.RelationFeaturesExtractor;
import org.apache.ctakes.temporal.ae.feature.*;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;
import java.util.*;


@PipeBitInfo(
      name = "E-E Consecutive Sentence TLinker",
      description = "Creates Event - Event TLinks between consecutive sentences.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
                       PipeBitInfo.TypeProduct.EVENT },
      products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class ConsecutiveSentencesEventEventRelationAnnotator extends RelationExtractorAnnotator {

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<? extends DataWriter<String>> dataWriterClass,
					File outputDirectory,
					double probabilityOfKeepingANegativeExample) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				ConsecutiveSentencesEventEventRelationAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				true,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				dataWriterClass,
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				outputDirectory,
				RelationExtractorAnnotator.PARAM_PROBABILITY_OF_KEEPING_A_NEGATIVE_EXAMPLE,
				// not sure why this has to be cast; something funny going on in uimaFIT maybe?
				(float) probabilityOfKeepingANegativeExample);
	}

	public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				ConsecutiveSentencesEventEventRelationAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(modelDirectory, "model.jar"));
	}

	@Override
	protected List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> getFeatureExtractors()
			throws ResourceInitializationException {
		return Lists.newArrayList(
				new UnexpandedTokenFeaturesExtractor() //use unexpanded version for i2b2 data
				, new OverlappedHeadFeaturesExtractor()
				, new EventArgumentPropertyExtractor()
				, new PartOfSpeechFeaturesExtractor()
				, new NumberOfEventTimeBetweenCandidatesExtractor()
				, new UmlsFeatureExtractor()
				, new SRLRelationFeaturesExtractor()
				, new SectionHeaderRelationExtractor()
				, new TimeXRelationFeaturesExtractor()
				, new EventPositionRelationFeaturesExtractor()
				, new CheckSpecialWordRelationExtractor()
				, new EventTimeRelationFeatureExtractor()
//				, new DeterminerRelationFeaturesExtractor()
				);
	}

	@Override
	protected Class<? extends Annotation> getCoveringClass() {
		return DocumentAnnotation.class;
	}

	@SuppressWarnings("null")
	@Override
	public List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(
			JCas jCas,
			Annotation document) {

		List<IdentifiedAnnotationPair> pairs = Lists.newArrayList();

		Collection<Segment> segments = JCasUtil.select(jCas, Segment.class);
		List<Segment> segList = Lists.newArrayList();
		for(Segment seg: segments){
			if (!seg.getId().equals("SIMPLE_SEGMENT")){//remove simple segment
				segList.add(seg);
			}
		}

		for(Segment segment : segList){
			List<Sentence> sentList = JCasUtil.selectCovered(jCas, Sentence.class, segment);
			int sentListLength 		= sentList.size();
			if( sentListLength >=2){
				for (int i=0; i<sentListLength-1; i++ ) {
					Sentence currentSent = sentList.get(i);
					Sentence nextSent	 = sentList.get(i+1);
					List<EventMention> currentEvents = JCasUtil.selectCovered(jCas, EventMention.class, currentSent);
					List<EventMention> nextEvents	 = JCasUtil.selectCovered(jCas, EventMention.class, nextSent);

					//filtering events
					List<EventMention> realEvents = new ArrayList<>();
					//filtering events
					for(EventMention event : currentEvents){
						// filter out ctakes events
						if(event.getClass().equals(EventMention.class)){
							realEvents.add(event);
						}
					}
					currentEvents = realEvents;
					realEvents = new ArrayList<>();
					//filtering events
					for(EventMention event : nextEvents){
						// filter out ctakes events
						if(event.getClass().equals(EventMention.class)){
							realEvents.add(event);
						}
					}
					nextEvents = realEvents;

					//scheme2 : pairing major events + time
					int currentSize = currentEvents == null ? 0 : currentEvents.size();
					int nextSize    = nextEvents == null ? 0 : nextEvents.size();
					if( currentSize == 0 || nextSize ==0){
						continue;
					}

					EventMention currentFirst = null;
					EventMention currentLast  = null;
					EventMention nextFirst 	  = null;
					EventMention nextLast 	  = null;

					if( currentSize ==1 ){
						currentFirst = currentEvents.get(0);
					}else if(currentSize > 1){
						currentFirst = currentEvents.get(0);
						currentLast  = currentEvents.get(currentSize-1);
					}

					if( nextSize == 1){
						nextFirst = nextEvents.get(0);
					}else if( nextSize > 1 ){
						nextFirst = nextEvents.get(0);
						nextLast  = nextEvents.get(nextSize-1); 
					}

					//pair them up
					if(currentFirst != null){
						if(nextFirst != null){
							pairs.add(new IdentifiedAnnotationPair(nextFirst, currentFirst));
						}
						if( nextLast != null){
							pairs.add(new IdentifiedAnnotationPair(nextLast, currentFirst));
						}
					}
					if( currentLast != null ){
						if(nextFirst != null){
							pairs.add(new IdentifiedAnnotationPair(nextFirst, currentLast));
						}
						if( nextLast != null){
							pairs.add(new IdentifiedAnnotationPair(nextLast, currentLast));
						}
					} 
				}
			}
		}
		
		//add system generated events:
//		if(this.isTraining()){
//			List<IdentifiedAnnotationPair> eventPairs = ImmutableList.copyOf(pairs);
//			for(IdentifiedAnnotationPair epair: eventPairs){
//				EventMention eventA = (EventMention) epair.getArg1();
//				EventMention eventB = (EventMention) epair.getArg2();
//				//pairing covered system events:
//				for(EventMention event1 : JCasUtil.selectCovered(jCas, EventMention.class, eventA)){
//					for(EventMention event2 : JCasUtil.selectCovered(jCas, EventMention.class, eventB)){
//						pairs.add(new IdentifiedAnnotationPair(event1, event2));
//					}
//					pairs.add(new IdentifiedAnnotationPair(event1, eventB));
//				}
//				for(EventMention event2 : JCasUtil.selectCovered(jCas, EventMention.class, eventB)){
//					pairs.add(new IdentifiedAnnotationPair(eventA, event2));
//				}
//			}
//		}

		return pairs;
	}


	//	private static boolean hasOverlapTokens(JCas jCas, EventMention event1, EventMention event2) {
	//		List<WordToken> currentTokens = JCasUtil.selectCovered(jCas, WordToken.class, event1);
	//		int tokenSize1 = currentTokens.size();
	//		List<WordToken> nextTokens = JCasUtil.selectCovered(jCas, WordToken.class, event2);
	//		int tokenSize2 = nextTokens.size();
	//		int tokenSize = Math.min(tokenSize1, tokenSize2);
	//		int matches = 0;
	//		for(WordToken t1: currentTokens){
	//			for(WordToken t2: nextTokens){
	//				if(t1.getCoveredText().toLowerCase().equals(t2.getCoveredText().toLowerCase())){
	//					matches++;
	//				}
	//			}
	//		}
	//		float matchRatio = (float)matches/tokenSize;
	//		if( matchRatio >= 0.5)
	//			return true;
	//		return false;
	//	}

	@Override
	protected void createRelation(JCas jCas, IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2, String predictedCategory) {
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
		relation.addToIndexes();
	}


	@Override
	protected String getRelationCategory(
			Map<List<Annotation>, BinaryTextRelation> relationLookup,
			IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) {
		BinaryTextRelation relation = relationLookup.get(Arrays.asList(arg1, arg2));
		String category = null;
		if (relation != null) {
			category = relation.getCategory();
		} else {
			relation = relationLookup.get(Arrays.asList(arg2, arg1));
			if (relation != null) {
				if(relation.getCategory().equals("OVERLAP")){
					category = relation.getCategory();
				}else if (relation.getCategory().equals("BEFORE")){
					category = "AFTER";
				}else if (relation.getCategory().equals("AFTER")){
					category = "BEFORE";
				}
				//				else{
				//					category = relation.getCategory() + "-1";
				//				}
			}
		}
		if (category == null && coin.nextDouble() <= this.probabilityOfKeepingANegativeExample) {
			category = NO_RELATION_CATEGORY;
		}
		return category;
	}
}
