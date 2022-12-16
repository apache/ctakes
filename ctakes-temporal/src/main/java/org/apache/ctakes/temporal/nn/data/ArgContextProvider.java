package org.apache.ctakes.temporal.nn.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.ctakes.temporal.utils.TokenPreprocForWord2Vec;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

public class ArgContextProvider {

	private static TimeMention coveringTimex;
	private static final String BTAG = "<B";
	private static final String OTAG = "<O>";
	private static final String ITAG = "<I";

	/**
	 * Position features for event-time relations
	 */
	public static String getEventTimePositionContext(
			JCas jCas, 
			Sentence sent, 
			IdentifiedAnnotation time,
			IdentifiedAnnotation event) {

		// get sentence as a list of tokens
		List<String> tokens = new ArrayList<>();
		for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, sent)) {
			tokens.add(baseToken.getCoveredText());  
		}

		// find the positions of time and event mentions
		// assume time consists of multipe words; event of one

		int currentPosition = 0;       // current token index
		int timeFirstPosition = -1000; // timex's start index
		int timeLastPosition = -1000;  // timex's end index
		int eventPosition = -1000;     // event's index
		for(BaseToken token : JCasUtil.selectCovered(jCas, BaseToken.class, sent)) {
			if(time.getBegin() == token.getBegin()) { 
				timeFirstPosition = currentPosition; // start of time expression found
			}
			if(time.getEnd() == token.getEnd()) {
				timeLastPosition = currentPosition;  // end of time expression found
			}
			if(event.getBegin() == token.getBegin()) { 
				eventPosition = currentPosition;     // event postion found
			} 
			currentPosition++;
		}

		// try to locate events that weren't found
		// e.g. "this can be re-discussed tomorrow"
		// "discussed" not found due to incorrect tokenization
		if(eventPosition == -1000) {
			currentPosition = 0;
			for(BaseToken token : JCasUtil.selectCovered(jCas, BaseToken.class, sent)) {
				if(token.getCoveredText().contains(event.getCoveredText())) {
					eventPosition = currentPosition; 
				}
				currentPosition++;
			}
		}

		if(eventPosition == -1000) {
			System.out.println("event not found: " + event.getCoveredText());
			System.out.println(sent.getCoveredText());
			System.out.println();
			eventPosition = 0; // just set it to zero for now
		}

		// now need to see if some times weren't found
		if(timeFirstPosition == -1000 || timeLastPosition == -1000) {
			System.out.println("time not found: " + time.getCoveredText());
			System.out.println(sent.getCoveredText());
			System.out.println();
			timeFirstPosition = 0; // just set it to zero for now
			timeLastPosition = 0;  // just set it to zero for now
		}

		List<String> positionsWrtToTime = new ArrayList<>();
		List<String> positionsWrtToEvent = new ArrayList<>();
		int tokensInSentence = JCasUtil.selectCovered(jCas, BaseToken.class, sent).size();
		for(int tokenIndex = 0; tokenIndex < tokensInSentence; tokenIndex++) {
			if(tokenIndex < timeFirstPosition) {
				positionsWrtToTime.add(Integer.toString(tokenIndex - timeFirstPosition));
			} else if(tokenIndex >= timeFirstPosition && tokenIndex <= timeLastPosition) {
				positionsWrtToTime.add("0");
			} else {
				positionsWrtToTime.add(Integer.toString(tokenIndex - timeLastPosition));
			}
			positionsWrtToEvent.add(Integer.toString(tokenIndex - eventPosition));
		}

		String tokensAsString = String.join(" ", tokens).replaceAll("[\r\n]", " ");
		String distanceToTime = String.join(" ", positionsWrtToTime);
		String distanceToEvent = String.join(" ", positionsWrtToEvent);

		return tokensAsString + "|" + distanceToTime + "|" + distanceToEvent;
	} 

	/**
	 * Position features for event-event relations
	 */
	public static String getEventEventPositionContext(
			JCas jCas, 
			Sentence sent, 
			IdentifiedAnnotation event1,
			IdentifiedAnnotation event2) {

		// get sentence as a list of tokens
		List<String> tokens = new ArrayList<>();
		for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, sent)) {
			tokens.add(baseToken.getCoveredText());  
		}

		// find the positions of event mentions
		// assume both events consists of just head words

		int currentPosition = 0;       // current token index
		int event1Position = -1000;    // event1's index
		int event2Position = -1000;    // event2's index
		for(BaseToken token : JCasUtil.selectCovered(jCas, BaseToken.class, sent)) {
			if(event1.getBegin() == token.getBegin()) {
				event1Position = currentPosition;     // event1 position found
			}
			if(event2.getBegin() == token.getBegin()) { 
				event2Position = currentPosition;     // event2 postion found
			} 
			currentPosition++;
		}

		List<String> positionsWrtToEvent1 = new ArrayList<>();
		List<String> positionsWrtToEvent2 = new ArrayList<>();    
		int tokensInSentence = JCasUtil.selectCovered(jCas, BaseToken.class, sent).size();
		for(int tokenIndex = 0; tokenIndex < tokensInSentence; tokenIndex++) {
			positionsWrtToEvent1.add(Integer.toString(tokenIndex - event1Position));
			positionsWrtToEvent2.add(Integer.toString(tokenIndex - event2Position));
		}

		String tokensAsString = String.join(" ", tokens).replaceAll("[\r\n]", " ");
		String distanceToTime = String.join(" ", positionsWrtToEvent1);
		String distanceToEvent = String.join(" ", positionsWrtToEvent2);

		return tokensAsString + "|" + distanceToTime + "|" + distanceToEvent;
	} 

	/**
	 * Return tokens between arg1 and arg2 as string 
	 * @param contextSize number of tokens to include on the left of arg1 and on the right of arg2
	 */
	public static String getRegions(JCas jCas, Sentence sent, Annotation left, Annotation right, int contextSize) {


		// tokens to the left from the left argument
		List<String> leftTokens = new ArrayList<>();
		for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, left, contextSize)) {
			if(sent.getBegin() <= baseToken.getBegin()) {
				leftTokens.add(baseToken.getCoveredText()); 
			}
		}
		String leftAsString = String.join(" ", leftTokens).replaceAll("[\r\n]", " ");

		// left arg tokens
		List<String> arg1Tokens = new ArrayList<>(); 
		for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, left)) {
			arg1Tokens.add(baseToken.getCoveredText());
		}
		String arg1AsString = String.join(" ", arg1Tokens).replaceAll("[\r\n]", " ");

		// tokens between the arguments
		List<String> betweenTokens = new ArrayList<>();
		for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, right)) {
			betweenTokens.add(baseToken.getCoveredText());
		}
		String betweenAsString = String.join(" ", betweenTokens).replaceAll("[\r\n]", " ");

		// right arg tokens
		List<String> arg2Tokens = new ArrayList<>(); 
		for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, right)) {
			arg2Tokens.add(baseToken.getCoveredText());
		}
		String arg2AsString = String.join(" ", arg2Tokens).replaceAll("[\r\n]", " ");

		// tokens to the right from the right argument
		List<String> rightTokens = new ArrayList<>();
		for(BaseToken baseToken : JCasUtil.selectFollowing(jCas, BaseToken.class, right, contextSize)) {
			if(baseToken.getEnd() <= sent.getEnd()) {
				rightTokens.add(baseToken.getCoveredText());
			}
		}
		String rightAsString = String.join(" ", rightTokens).replaceAll("[\r\n]", " ");

		return leftAsString + "|" + arg1AsString + "|" + betweenAsString + "|" + arg2AsString + "|" + rightAsString;
	}

	/**
	 * Print words from left to right.
	 * @param contextSize number of tokens to include on the left of arg1 and on the right of arg2
	 */
	public static String getTokenContext(
			JCas jCas, 
			Sentence sent, 
			Annotation left,
			String leftType,
			Annotation right,
			String rightType,
			int contextSize) {

		List<String> tokens = new ArrayList<>();
		for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, left, contextSize)) {
			if(sent.getBegin() <= baseToken.getBegin()) {
				//				if(!(baseToken instanceof NewlineToken)){
				String stringValue = TokenPreprocForWord2Vec.tokenToString(baseToken);
				tokens.add(stringValue);//baseToken.getCoveredText()); 
				//				}
			}
		}
		tokens.add("<" + leftType + ">");
		//tokens.add(left.getCoveredText());
		for(BaseToken base : JCasUtil.selectCovered(jCas, BaseToken.class, left)){
			String stringValue = TokenPreprocForWord2Vec.tokenToString(base);
			tokens.add(stringValue);
		}

		tokens.add("</" + leftType + ">");
		for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, right)) {
			//			if(!(baseToken instanceof NewlineToken)){
			String stringValue = TokenPreprocForWord2Vec.tokenToString(baseToken);
			tokens.add(stringValue);//baseToken.getCoveredText()); 
			//			}
		}
		tokens.add("<" + rightType + ">");
		//tokens.add(right.getCoveredText());
		for(BaseToken base : JCasUtil.selectCovered(jCas, BaseToken.class, right)){
			String stringValue = TokenPreprocForWord2Vec.tokenToString(base);
			tokens.add(stringValue);
		}

		tokens.add("</" + rightType + ">");
		for(BaseToken baseToken : JCasUtil.selectFollowing(jCas, BaseToken.class, right, contextSize)) {
			if(baseToken.getEnd() <= sent.getEnd()) {
				//				if(!(baseToken instanceof NewlineToken)){
				String stringValue = TokenPreprocForWord2Vec.tokenToString(baseToken);
				tokens.add(stringValue);//baseToken.getCoveredText()); 
				//				}
			}
		}

		return String.join(" ", tokens).replaceAll("[\r\n]", " ");
	}


	/**
	 * Print POS tags from left to right.
	 * @param contextSize number of tokens to include on the left of arg1 and on the right of arg2
	 */
	public static String getPosContext(
			JCas jCas, 
			Sentence sent, 
			Annotation left,
			String leftType,
			Annotation right,
			String rightType,
			int contextSize) {

		List<String> tokens = new ArrayList<>();
		for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, left, contextSize)) {
			if(sent.getBegin() <= baseToken.getBegin()) {
				if(!baseToken.getCoveredText().equals(" ")){
					tokens.add(baseToken.getPartOfSpeech());
				}
			}
		}
		tokens.add("<" + leftType + ">");
		for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, left)) {
			if(!(baseToken instanceof NewlineToken)){
				tokens.add(baseToken.getPartOfSpeech());
			}
		}
		tokens.add("</" + leftType + ">");
		for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, right)) {
			if(!(baseToken instanceof NewlineToken)){
				tokens.add(baseToken.getPartOfSpeech());
			}
		}
		tokens.add("<" + rightType + ">");
		for(BaseToken baseToken : JCasUtil.selectCovered(jCas, BaseToken.class, right)) {
			if(!(baseToken instanceof NewlineToken)){
				tokens.add(baseToken.getPartOfSpeech());
			}
		}
		tokens.add("</" + rightType + ">");
		for(BaseToken baseToken : JCasUtil.selectFollowing(jCas, BaseToken.class, right, contextSize)) {
			if(baseToken.getEnd() <= sent.getEnd()) {
				if(!(baseToken instanceof NewlineToken)){
					tokens.add(baseToken.getPartOfSpeech());
				}
			}
		}

		return String.join(" ", tokens).replaceAll("[\r\n]", " ");
	}

	public static String getBIOContext(JCas jCas, Sentence sentence, IdentifiedAnnotation arg1, String leftType,
			IdentifiedAnnotation arg2, String rightType, int contextSize) {
		List<String> tokens = new ArrayList<>();
		//select prior context:
		List<TimeMention> preTimex = JCasUtil.selectCovered(jCas, TimeMention.class, sentence.getBegin(), arg1.getBegin());
		List<TimeMention> betweenTimex = JCasUtil.selectCovered(jCas, TimeMention.class, arg1.getEnd(), arg2.getBegin());
		List<TimeMention> afterTimex = JCasUtil.selectCovered(jCas, TimeMention.class, arg2.getEnd(), sentence.getEnd());

		tokens = addTimex2TokenSequence(jCas, tokens, JCasUtil.selectPreceding(jCas, BaseToken.class, arg1, contextSize), preTimex, sentence);
		//get arg1:
		tokens.add(OTAG);//"<" + leftType + ">");
		if (arg1 instanceof TimeMention){
			String timeTag = generateTimeTag(jCas, (TimeMention)arg1);
			tokens.add(timeTag);
		}else{
			tokens.add(OTAG);//arg1.getCoveredText());
			//          tokens.add(getEventProperty((EventMention)arg1));
		}
		tokens.add(OTAG);//"</" + leftType + ">");
		tokens = addTimex2TokenSequence(jCas, tokens, JCasUtil.selectBetween(jCas, BaseToken.class, arg1, arg2), betweenTimex, sentence);
		//arg2
		tokens.add(OTAG);//"<" + rightType + ">");
		if (arg2 instanceof TimeMention){
			String timeTag = generateTimeTag(jCas, (TimeMention)arg2);
			tokens.add(timeTag);
		}else{
			tokens.add(OTAG);//arg2.getCoveredText());
			//          tokens.add(getEventProperty((EventMention)arg2));
		}
		tokens.add(OTAG);//"</" + rightType + ">");
		tokens = addTimex2TokenSequence(jCas, tokens, JCasUtil.selectFollowing(jCas, BaseToken.class, arg2, contextSize), afterTimex, sentence);


		return String.join(" ", tokens).replaceAll("[\r\n]", " ");
	}

	private static String generateTimeTag(JCas jCas, TimeMention timex) {
		String timeTag;
		//check if there is any number token within the timex mention:
		List<BaseToken> tokens = JCasUtil.selectCovered(jCas, BaseToken.class, timex);
		int numTokens = tokens.size();
		if(numTokens>0){
			timeTag=BTAG+"_"+timex.getTimeClass()+">";
		}else{
			return "";
		}

		if(numTokens==1){
			return timeTag;
		}
		for(int i=0;i<numTokens-1; i++){
			timeTag= timeTag+ " " + ITAG+"_"+timex.getTimeClass()+">";
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
					tokens.add(OTAG); 
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

	public static String getTokenContext(JCas jCas, Sentence sent, IdentifiedAnnotation left, String leftType,
			String umlsleft, IdentifiedAnnotation right, String rightType, String umlsright, int contextSize) {
		List<String> tokens = new ArrayList<>();
		for(BaseToken baseToken :  JCasUtil.selectPreceding(jCas, BaseToken.class, left, contextSize)) {
			if(sent.getBegin() <= baseToken.getBegin()) {
				//				if(!(baseToken instanceof NewlineToken)){
				tokens.add(baseToken.getCoveredText()); 
				//				}
			}
		}
		tokens.add("<" + leftType + ">");
		tokens.add(umlsleft);
		tokens.add("</" + leftType + ">");
		//		for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, right)) {
		////			if(!(baseToken instanceof NewlineToken)){
		//				tokens.add(baseToken.getCoveredText()); 
		////			}
		//		}
		//find all non-overlapping events between to arguments:
		List<EventMention> nonOverlapEvents = new ArrayList<>();
		for(EventMention event : JCasUtil.selectBetween(jCas, EventMention.class, left, right)){
			int coveringNum = JCasUtil.selectCovering(jCas, EventMention.class, event).size();
			int coveredWord = JCasUtil.selectCovered(jCas, WordToken.class, event).size();
			if(coveringNum <=1 && !event.getClass().equals(EventMention.class) && coveredWord > 1){
				nonOverlapEvents.add(event);
			}
		}
		if(nonOverlapEvents.size()==0){
			for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, left, right)) {
				//			if(!(baseToken instanceof NewlineToken)){
				tokens.add(baseToken.getCoveredText()); 
				//			}
			}
		}else{
			IdentifiedAnnotation leftentity = left;
			for(EventMention event : nonOverlapEvents){
				for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, leftentity, event)) {
					tokens.add(baseToken.getCoveredText()); 
				}
				tokens.add("umls_"+event.getTypeID());
				leftentity=event;
			}
			for(BaseToken baseToken : JCasUtil.selectBetween(jCas, BaseToken.class, leftentity, right)) {
				tokens.add(baseToken.getCoveredText()); 
			}
		}

		tokens.add("<" + rightType + ">");
		tokens.add(umlsright);
		tokens.add("</" + rightType + ">");
		for(BaseToken baseToken : JCasUtil.selectFollowing(jCas, BaseToken.class, right, contextSize)) {
			if(baseToken.getEnd() <= sent.getEnd()) {
				//				if(!(baseToken instanceof NewlineToken)){
				tokens.add(baseToken.getCoveredText()); 
				//				}
			}
		}

		return String.join(" ", tokens).replaceAll("[\r\n]", " ");
	}

}
