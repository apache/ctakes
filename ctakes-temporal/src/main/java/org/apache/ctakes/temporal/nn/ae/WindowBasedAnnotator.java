package org.apache.ctakes.temporal.nn.ae;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.temporal.ae.TemporalRelationExtractorAnnotator.IdentifiedAnnotationPair;
import org.apache.ctakes.temporal.nn.ae.EventTimeTokenBasedAnnotator.OutputMode;
import org.apache.ctakes.temporal.utils.TokenPreprocForWord2Vec;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.util.ViewUriUtil;

import com.google.common.collect.Lists;
/**
 * a joint annotator for annotating both event-time and event-event relations using neural models.
 * @author chenlin
 *
 */
public class WindowBasedAnnotator extends CleartkAnnotator<String> {

	public static final String NO_RELATION_CATEGORY = "none";
	public static final int WINDOW_SIZE = 60;
	public static List<Integer> tokenCount;

	//output modes:
	//"TokenSeq": original token sequence
	//"TokenTimeclass": original token sequence + timeClass tag
	//"TokenTimeclassPosSeq": original token sequence + timeClass + pos seq tag
	//"timeclass": <timex_timeClass>
	//"timeclassPosSeq": <timex_timeClass_POStags>
	//"posSeq": <timex_jj_nn>
	//"singleTag": <timex>
	//"indexTags": <timex_0>
	public static OutputMode timexMode;

	private static FileWriter fstream;
	private static BufferedWriter out;

	public WindowBasedAnnotator() {
		timexMode = OutputMode.Timeclass; //set the mode here
		tokenCount= new ArrayList<>();
	}


	public static void TokenStatisticWriter() throws IOException{
		fstream = new FileWriter("target/eval/thyme/train_and_test/tokenStatistic.txt");
		out = new BufferedWriter(fstream);

		for(int i = 0; i < tokenCount.size(); i++){
			out.write(tokenCount.get(i)+"\n");
		}
		out.close();
	}
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		//		Map<EventMention, Collection<EventMention>> coveringMap =
		//				JCasUtil.indexCovering(jCas, EventMention.class, EventMention.class);

		// get all gold relation lookup
		Map<List<Annotation>, BinaryTextRelation> relationLookup;
		relationLookup = new HashMap<>();
		if (this.isTraining()) {
			relationLookup = new HashMap<>();
			for (BinaryTextRelation relation : JCasUtil.select(jCas, BinaryTextRelation.class)) {
				Annotation arg1 = relation.getArg1().getArgument();
				Annotation arg2 = relation.getArg2().getArgument();
				// The key is a list of args so we can do bi-directional lookup
				//				int tokenNum = JCasUtil.selectCovered(jCas, BaseToken.class, arg1.getEnd(), arg2.getBegin()).size();
				//				tokenCount.add(tokenNum);
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

		//		Boolean expandEvents = false;
		List<String> badSegs = new ArrayList<>(Arrays.asList("SIMPLE_SEGMENT","20104","20105","20116","20138"));//,"20110"

		Collection<Segment> segments = JCasUtil.select(jCas, Segment.class);
		List<Segment> segList = Lists.newArrayList();
		for(Segment seg: segments){
			//if (!seg.getId().equals("SIMPLE_SEGMENT")){//remove simple segment
			if (!badSegs.contains(seg.getId())){
				segList.add(seg);
			}
		}

		List<IdentifiedAnnotationPair> candidatePairs = Lists.newArrayList();
		for(Segment segment : segList){
			List<IdentifiedAnnotation> entities = JCasUtil.selectCovered(jCas, IdentifiedAnnotation.class, segment);
			//filter entities:
			List<IdentifiedAnnotation> realEntities = new ArrayList<>();
			//filtering events
			for(IdentifiedAnnotation entity : entities){
				// filter out ctakes events
				if(entity instanceof EventMention && entity.getClass().equals(EventMention.class)){
					realEntities.add(entity);
				}else if( entity instanceof TimeMention){
					realEntities.add(entity);
				}
			}
			entities = realEntities;
			int entityNum = entities.size();
			for(int i=0; i< entityNum -1; i++){
				IdentifiedAnnotation entityA = entities.get(i);
				for(int j=i+1; j< entityNum; j++){
					IdentifiedAnnotation entityB = entities.get(j);
					//check if two entity are two far from each other
					int baseTokenNum = JCasUtil.selectCovered(jCas, BaseToken.class, entityA.getEnd(), entityB.getBegin()).size();
					if(baseTokenNum > WINDOW_SIZE){
						break;
					}
					if(entityA instanceof EventMention || entityB instanceof EventMention){//don't consider Time-Time relations
						candidatePairs.add(new IdentifiedAnnotationPair(entityA, entityB));
					}
				}
			}
		}

		// walk through the pairs of annotations
		for (IdentifiedAnnotationPair pair : candidatePairs) {
			IdentifiedAnnotation arg1 = pair.getArg1();
			IdentifiedAnnotation arg2 = pair.getArg2();

			String context;
			if(arg1 instanceof TimeMention){
				context = getTokenContext(jCas, arg1, "t", arg2, "e");
			}else if(arg2 instanceof TimeMention){
				context = getTokenContext(jCas, arg1, "e", arg2, "t");
			}else{
				context = getTokenContext(jCas, arg1, "ea", arg2, "eb");
			}

			List<Feature> feats = new ArrayList<>();
			String[] tokens = context.split(" ");
			for (String token: tokens){
				feats.add(new Feature(token));//.toLowerCase()
			}

			// during training, feed the features to the data writer
			if(this.isTraining()) {
				String category = getRelationCategory(relationLookup, arg1, arg2);

				if(category == null) {
					category = NO_RELATION_CATEGORY;
				} else{
					category = category.toLowerCase();
				}
				this.dataWriter.write(new Instance<>(category, feats));
			} else {
				String predictedCategory = this.classifier.classify(feats);

				if (predictedCategory != null && !predictedCategory.equals(NO_RELATION_CATEGORY)) {

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


		//
		//		if(this.isTraining()){//in training time, output the token statistics for relations
		//			try {
		//				TokenStatisticWriter();
		//			} catch (IOException e) {
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}
		//		}
	}


	private static String getTokenContext(JCas jCas, IdentifiedAnnotation arg1, String type1, IdentifiedAnnotation arg2,
			String type2) {
		List<String> tokens = new ArrayList<>();

		//Two tokens prior
		for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, arg1, 2) ) {
			String stringValue = TokenPreprocForWord2Vec.tokenToString(baseToken);
			tokens.add(stringValue);
		}

		//arg1
		tokens.add(type1 + "s");
		if (arg1 instanceof TimeMention){
			String timeTag = generateTimeTag(jCas, (TimeMention)arg1);
			tokens.add(timeTag);
		}else{
			tokens.add(arg1.getCoveredText().replaceAll("[\r\n?|\n]"," newline ").toLowerCase());//.toLowerCase()
		}
		tokens.add(type1 + "e");

		//tokens in the middle
		for(BaseToken baseToken :  JCasUtil.selectCovered(jCas, BaseToken.class, arg1.getEnd(), arg2.getBegin()) ) {
			String stringValue = TokenPreprocForWord2Vec.tokenToString(baseToken);
			tokens.add(stringValue);
		}

		//arg2
		tokens.add(type2 + "s");
		if (arg2 instanceof TimeMention){
			String timeTag = generateTimeTag(jCas, (TimeMention)arg2);
			tokens.add(timeTag);
		}else{
			tokens.add(arg2.getCoveredText().replaceAll("[\r\n?|\n]"," newline ").toLowerCase());//.toLowerCase()
		}
		tokens.add(type2 + "e");

		//two tokens after
		for(BaseToken baseToken :  JCasUtil.selectFollowing(jCas, BaseToken.class, arg2, 2) ) {
			String stringValue = TokenPreprocForWord2Vec.tokenToString(baseToken);
			tokens.add(stringValue);
		}

		return String.join(" ", tokens).replaceAll("[\r\n?|\n]", " newline ");
	}


	private static String generateTimeTag(JCas jCas, TimeMention timex) {
		String timeTag = "<timex";
		if(timexMode == OutputMode.IndexTags){
			timeTag = timeTag+"_";
			int idx = 0;
			timeTag = timeTag+ idx+">";
		}else if(timexMode == OutputMode.Timeclass){
			//timeTag = "<timex_"+timex.getTimeClass()+">";
			timeTag = timex.getTimeClass().toLowerCase();
		}else if(timexMode == OutputMode.TimeclassPosSeq){
			timeTag = "<timex_"+timex.getTimeClass();
			for(BaseToken token : JCasUtil.selectCovered(jCas, BaseToken.class, timex)){
				timeTag = timeTag+"_"+token.getPartOfSpeech();
			}
			timeTag = timeTag+">";
		}else if(timexMode == OutputMode.PosSeq){
			timeTag = "<timex";
			for(BaseToken token : JCasUtil.selectCovered(jCas, BaseToken.class, timex)){
				timeTag = timeTag+"_"+token.getPartOfSpeech();
			}
			timeTag = timeTag+">";
		}else if(timexMode == OutputMode.SingleTag){
			timeTag = timeTag+">";
		}else if(timexMode == OutputMode.TokenTimeclass){
			timeTag = timex.getCoveredText()+" <timex_"+timex.getTimeClass()+">";
		}else if(timexMode == OutputMode.TokenTimeclassPosSeq){
			timeTag = timex.getCoveredText()+" <timex_"+timex.getTimeClass();
			for(BaseToken token : JCasUtil.selectCovered(jCas, BaseToken.class, timex)){
				timeTag = timeTag+"_"+token.getPartOfSpeech();
			}
			timeTag = timeTag+">";
		}else if(timexMode == OutputMode.NoTag){
			timeTag="";
		}
		return timeTag;
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
		//for event-event relations:
		if (relation != null) {
			category = relation.getCategory();
		} else {
			relation = relationLookup.get(Arrays.asList(arg2, arg1));
			if (relation != null) {
				category = relation.getCategory() + "-1";
			}
		}


		return category;
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
}
