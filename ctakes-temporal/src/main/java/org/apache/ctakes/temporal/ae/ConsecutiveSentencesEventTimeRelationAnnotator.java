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
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
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

//import org.apache.ctakes.temporal.ae.feature.TemporalAttributeForMixEventTimeExtractor;
//import org.apache.ctakes.typesystem.type.syntax.WordToken;
//import org.apache.ctakes.typesystem.type.textspan.Paragraph;

@PipeBitInfo(
		name = "E-T Consecutive Sentence TLinker",
		description = "Creates Event - Time TLinks between consecutive sentences.",
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
							  PipeBitInfo.TypeProduct.EVENT, PipeBitInfo.TypeProduct.TIMEX },
		products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class ConsecutiveSentencesEventTimeRelationAnnotator extends RelationExtractorAnnotator {

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<? extends DataWriter<String>> dataWriterClass,
					File outputDirectory,
					double probabilityOfKeepingANegativeExample) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				ConsecutiveSentencesEventTimeRelationAnnotator.class,
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
				ConsecutiveSentencesEventTimeRelationAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(modelDirectory, "model.jar"));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<RelationFeaturesExtractor<IdentifiedAnnotation,IdentifiedAnnotation>> getFeatureExtractors()
			throws ResourceInitializationException {
		return Lists.newArrayList(
				new UnexpandedTokenFeaturesExtractor() //use unexpanded version for i2b2 data
				//				, new OverlappedHeadFeaturesExtractor()
				, new EventArgumentPropertyExtractor()
				, new PartOfSpeechFeaturesExtractor()
				, new NumberOfEventTimeBetweenCandidatesExtractor()
				, new UmlsFeatureExtractor()
				, new SRLRelationFeaturesExtractor()
				, new SectionHeaderRelationExtractor()
				, new TimeXRelationFeaturesExtractor()
				, new EventPositionRelationFeaturesExtractor()
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
			List<TimeMention> segTimes = JCasUtil.selectCovered(jCas, TimeMention.class, segment);
			for( TimeMention time : segTimes){
				//get the sentence before this timex
				List<Sentence> consecutiveSents = Lists.newArrayList();
				List<Sentence> sents = JCasUtil.selectPreceding(jCas, Sentence.class, time, 1);
				if(sameSegment(sents,segment)){
					consecutiveSents.addAll(sents);
				}
				sents = JCasUtil.selectFollowing(jCas, Sentence.class, time, 1);
				if(sameSegment(sents,segment)){
					consecutiveSents.addAll(sents);
				}
				for(Sentence sent : consecutiveSents){
					List<EventMention> events = new ArrayList<>(JCasUtil.selectCovered(jCas, EventMention.class, sent));
					//filter events:
					List<EventMention> realEvents = Lists.newArrayList();
					for( EventMention event : events){
						if(event.getClass().equals(EventMention.class)){
							realEvents.add(event);
						}
					}
					events = realEvents;
					int eventNum = events.size();
					if(eventNum >=1){
						pairs.add(new IdentifiedAnnotationPair(events.get(0), time));//pair up the first event and the time
						if(eventNum > 1){
							pairs.add(new IdentifiedAnnotationPair(events.get(eventNum-1), time));//pair up the last event and the time
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
//				TimeMention time = (TimeMention) epair.getArg2();
//				//pairing covered system events:
//				for(EventMention event : JCasUtil.selectCovered(jCas, EventMention.class, eventA)){
//					pairs.add(new IdentifiedAnnotationPair(event, time));
//				}
//			}
//		}
		return pairs;
	}

	private static boolean sameSegment(List<Sentence> sents, Segment segment) {
		if(sents == null || sents.size()==0) return false;
		Sentence sent = sents.get(0);
		if(segment.getBegin()<=sent.getBegin() && segment.getEnd()>= sent.getEnd()) return true;
		return false;
	}

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
			}
		}
		if (category == null && coin.nextDouble() <= this.probabilityOfKeepingANegativeExample) {
			category = NO_RELATION_CATEGORY;
		}
		return category;
	}
}
