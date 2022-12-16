package org.apache.ctakes.temporal.nn.ae;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.temporal.ae.TemporalRelationExtractorAnnotator.IdentifiedAnnotationPair;
import org.apache.ctakes.temporal.nn.ae.EventTimeTokenBasedAnnotator.OutputMode;
import org.apache.ctakes.temporal.nn.data.ArgContextProvider;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
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
		name = "E-E Token TLinker",
		description = "Creates Event - Event TLinks from Token type.",
		dependencies = { PipeBitInfo.TypeProduct.SECTION, PipeBitInfo.TypeProduct.SENTENCE,
				PipeBitInfo.TypeProduct.EVENT },
		products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class EventEventTokenBasedAnnotator extends CleartkAnnotator<String> {

	public static final String NO_RELATION_CATEGORY = "none";
	private static TimeMention coveringTimex;
	public static Map<String, Integer> timex_idx;

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

	private BufferedReader reader;
	private static FileWriter fstream;
	private static BufferedWriter out;

	public EventEventTokenBasedAnnotator() {
		timexMode = OutputMode.TimeclassPosSeq; //set the mode here
		if(timexMode == OutputMode.IndexTags){
			timex_idx = new HashMap<>();
		}
	}

	public Map<String, Integer> TimexIdxReader(InputStream in) throws IOException{
		reader = new BufferedReader(new InputStreamReader(in));
		Map<String, Integer> timex_index = new HashMap<>();
		String line = null;
		while((line = reader.readLine()) != null){
			line = line.trim();
			int sep = line.lastIndexOf("|");
			String timex = line.substring(0, sep);
			Integer idx = Integer.parseInt(line.substring(sep+1));
			timex_index.put(timex, idx);
		}
		reader.close();
		return timex_index;
	}

	public static void TimexIdxWriter() throws IOException{
		fstream = new FileWriter("target/eval/thyme/train_and_test/event-event/timex_idx.txt");
		out = new BufferedWriter(fstream);

		Iterator<Entry<String, Integer>> it = timex_idx.entrySet().iterator();
		while (it.hasNext()) {

			// the key/value pair is stored here in pairs
			Map.Entry<String, Integer> pairs = it.next();
			out.write(pairs.getKey() + "|" + pairs.getValue() + "\n");
		}
		out.close();
	}
	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		if(timexMode == OutputMode.IndexTags && !this.isTraining()){
			final String timexIdxMapFile = "target/eval/thyme/train_and_test/event-event/timex_idx.txt";
			try {
				timex_idx = TimexIdxReader(FileLocator.getAsStream(timexIdxMapFile));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// get all gold relation lookup
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
					System.err.println("Error! This attempted relation " + relation.getCategory() + 
							" already has a relation " + reln + " at this span: " + 
							arg1.getCoveredText() + " -- " + arg2.getCoveredText());
				} else {
					relationLookup.put(key, relation);
				}
			}
		}

		for(Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
			// collect all relevant relation arguments from the sentence
			List<IdentifiedAnnotationPair> candidatePairs = getCandidateRelationArgumentPairs(jCas, sentence);

			// walk through the pairs of annotations
			for (IdentifiedAnnotationPair pair : candidatePairs) {
				IdentifiedAnnotation arg1 = pair.getArg1();
				IdentifiedAnnotation arg2 = pair.getArg2();

				String context;
				if(arg2.getBegin() < arg1.getBegin()) {
					// ... event2 ... event1 ... scenario
					System.out.println("\n-------------- THIS NEVER NAPPENS ------------\n");
					if(timexMode == OutputMode.TokenSeq){
						context = ArgContextProvider.getTokenContext(jCas, sentence, arg2, "e2", arg1, "e1", 2); 
					}else{
						context = getTokenTimexContext(jCas, sentence, arg2, "e2", arg1, "e1", 2);
					}
				} else {
					// ... event1 ... event2 ... scenario
					if(timexMode == OutputMode.TokenSeq){
						context = ArgContextProvider.getTokenContext(jCas, sentence, arg1, "e1", arg2, "e2", 2);
					}else{
						context = getTokenTimexContext(jCas, sentence, arg1, "e1", arg2, "e2", 2);
					}
				}

				//derive features based on context:
				List<Feature> feats = new ArrayList<>();
				String[] tokens = context.split(" ");
				for (String token: tokens){
					feats.add(new Feature(token.toLowerCase()));
				}

				// during training, feed the features to the data writer
				if(this.isTraining()) {
					String category = getRelationCategory(relationLookup, arg1, arg2);

					// drop some portion of negative examples during training
					// if(category == null && coin.nextDouble() <= 0.5) {
					//   continue; // skip this negative example
					// }

					if(category == null) {
						category = NO_RELATION_CATEGORY;
					} else{
						category = category.toLowerCase();
					}
					this.dataWriter.write(new Instance<>(category, feats));
				} else {
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
		if(timexMode== OutputMode.IndexTags && !this.isTraining()){//in test time update the hashmap file for each cas
			try {
				TimexIdxWriter();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static String getTokenTimexContext(JCas jCas, Sentence sentence, IdentifiedAnnotation arg1, String leftType,
			IdentifiedAnnotation arg2, String rightType, int contextSize) {
		List<String> tokens = new ArrayList<>();
		//select prior context:
		List<TimeMention> preTimex = JCasUtil.selectCovered(jCas, TimeMention.class, sentence.getBegin(), arg1.getBegin());
		List<TimeMention> betweenTimex = JCasUtil.selectCovered(jCas, TimeMention.class, arg1.getEnd(), arg2.getBegin());
		List<TimeMention> afterTimex = JCasUtil.selectCovered(jCas, TimeMention.class, arg2.getEnd(), sentence.getEnd());

		tokens = addTimex2TokenSequence(jCas, tokens, JCasUtil.selectPreceding(jCas, BaseToken.class, arg1, contextSize), preTimex, sentence);
		//get arg1:
		tokens.add("<" + leftType + ">");
		if (arg1 instanceof TimeMention){
			String timeTag = generateTimeTag(jCas, (TimeMention)arg1);
			tokens.add(timeTag);
		}else{
			tokens.add(arg1.getCoveredText());
			//          tokens.add(getEventProperty((EventMention)arg1));
		}
		tokens.add("</" + leftType + ">");
		tokens = addTimex2TokenSequence(jCas, tokens, JCasUtil.selectBetween(jCas, BaseToken.class, arg1, arg2), betweenTimex, sentence);
		//arg2
		tokens.add("<" + rightType + ">");
		if (arg2 instanceof TimeMention){
			String timeTag = generateTimeTag(jCas, (TimeMention)arg2);
			tokens.add(timeTag);
		}else{
			tokens.add(arg2.getCoveredText());
			//          tokens.add(getEventProperty((EventMention)arg2));
		}
		tokens.add("</" + rightType + ">");
		tokens = addTimex2TokenSequence(jCas, tokens, JCasUtil.selectFollowing(jCas, BaseToken.class, arg2, contextSize), afterTimex, sentence);


		return String.join(" ", tokens).replaceAll("[\r\n]", " ");
	}

	private static String generateTimeTag(JCas jCas, TimeMention timex) {
		String timeTag = "<timex";
		if(timexMode == OutputMode.IndexTags){
			timeTag = timeTag+"_";
			int idx = 0;
			String timeWord = timex.getCoveredText().toLowerCase().replaceAll("[\r\n]", " ");
			if(timex_idx.containsKey(timeWord)){
				idx = timex_idx.get(timeWord);         
			}else{
				idx = timex_idx.size();
				timex_idx.put(timeWord, idx);
			}
			timeTag = timeTag+ idx+">";
		}else if(timexMode == OutputMode.Timeclass){
			timeTag = "<timex_"+timex.getTimeClass()+">";
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

	private static List<String> addTimex2TokenSequence(JCas jCas, List<String> tokens, List<BaseToken> tokenSequences, List<TimeMention> listOfTimex, Sentence sent) {
		coveringTimex = null;
		for (BaseToken baseToken : tokenSequences){
			if(baseToken.getEnd() <= sent.getEnd() && sent.getBegin() <= baseToken.getBegin()){
				TimeMention currentTimex = findCoveringTimex(baseToken, listOfTimex);
				if(currentTimex == null){
					if(coveringTimex != null ){
						coveringTimex = null;
					}
					tokens.add(baseToken.getCoveredText()); 
				}else{//current timex is not null
					if( currentTimex != coveringTimex){
						String timeTag = generateTimeTag(jCas, currentTimex);
						tokens.add(timeTag);
						coveringTimex = currentTimex;
					}
				}
			}
		}
		return tokens;
	}

	private static TimeMention findCoveringTimex(BaseToken baseToken, List<TimeMention> timexs) {
		for(TimeMention timex : timexs){
			if(timex.getBegin()<= baseToken.getBegin() && timex.getEnd() >= baseToken.getEnd()){
				return timex;
			}
		}
		return null;
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
			if (arg2.getBegin() < arg1.getBegin()) {
				category = relation.getCategory() + "-1";
			} else {
				category = relation.getCategory();
			}
		} else {
			relation = relationLookup.get(Arrays.asList(arg2, arg1));
			if (relation != null) {
				if(arg2.getBegin() < arg1.getBegin()){
					category = relation.getCategory();
				} else {
					category = relation.getCategory() + "-1";
				}
			}
		}

		return category;
	}

	protected String getRelationCategory2(Map<List<Annotation>, BinaryTextRelation> relationLookup,
			IdentifiedAnnotation arg1,
			IdentifiedAnnotation arg2) {

		// gold view representation (i.e. only contains relations)
		BinaryTextRelation arg1ContainsArg2 = relationLookup.get(Arrays.asList(arg1, arg2));
		BinaryTextRelation arg2ContainsArg1 = relationLookup.get(Arrays.asList(arg2, arg1));

		// now translate to position dependent representation (i.e. contains and contains-1)
		if(arg1ContainsArg2 != null) {
			// still need to know whether it's arg1 ... arg2 or arg2 ... arg1
			// because that determines whether it's contains or contains-1
			if(arg1.getBegin() < arg2.getBegin()) {
				return arg1ContainsArg2.getCategory();
			} else {
				return arg1ContainsArg2.getCategory() + "-1";
			}
		} else if(arg2ContainsArg1 != null) {
			if(arg1.getBegin() < arg2.getBegin()) {
				return arg2ContainsArg1.getCategory() + "-1";
			} else {
				return arg2ContainsArg1.getCategory();
			}
		} else {
			return null;      
		}
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

	private static List<IdentifiedAnnotationPair> getCandidateRelationArgumentPairs(JCas jCas, Sentence sentence) {

		List<IdentifiedAnnotationPair> pairs = Lists.newArrayList();
		List<EventMention> events = new ArrayList<>(JCasUtil.selectCovered(jCas, EventMention.class, sentence));
		// filter events
		List<EventMention> realEvents = Lists.newArrayList();
		for( EventMention event : events){
			if(event.getClass().equals(EventMention.class)){
				realEvents.add(event);
			}
		}
		events = realEvents;

		int eventNum = events.size();
		for (int i = 0; i < eventNum-1; i++) {
			for(int j = i+1; j < eventNum; j++) {
				EventMention eventA = events.get(i);
				EventMention eventB = events.get(j);
				pairs.add(new IdentifiedAnnotationPair(eventA, eventB));
			}
		}

		return pairs;
	}
}
