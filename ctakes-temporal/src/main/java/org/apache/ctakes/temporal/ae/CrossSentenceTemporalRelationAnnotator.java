package org.apache.ctakes.temporal.ae;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.duration.Utils;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.syntax.PunctuationToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

@PipeBitInfo(
		name = "E-E Cross- Sentence TLinker",
		description = "Creates Event - Event TLinks across sentences.",
		dependencies = { PipeBitInfo.TypeProduct.EVENT, PipeBitInfo.TypeProduct.TIMEX },
		products = { PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class CrossSentenceTemporalRelationAnnotator extends JCasAnnotator_ImplBase {

	public static boolean isValidDate(String inDate) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setLenient(false);
		try {
			dateFormat.parse(inDate.trim());
		} catch (ParseException pe) {
			return false;
		}
		return true;
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		//1. look for all timex that are dates, of format xxxx-xx-xx
		for (TimeMention timex : JCasUtil.select(jCas, TimeMention.class)) {
			//add normalized timex
			String value = Utils.getTimexMLValue(timex.getCoveredText());
			boolean hasComma = false;
			EventMention precedingEvent = null;
			if(value != null && isValidDate(value)){
				//2. check if there is any ":" in the immediate context of the timex
				List<BaseToken> beforeTokens = JCasUtil.selectPreceding(jCas, BaseToken.class, timex, 2);
				List<BaseToken> afterTokens = JCasUtil.selectFollowing(jCas, BaseToken.class, timex, 2);
				for(BaseToken token: beforeTokens){
					if(token instanceof PunctuationToken && token.getCoveredText().equals(":")){
						hasComma = true;
					}else{//3. check if there is any event immediately before the timex
						for(EventMention event: JCasUtil.selectCovering(jCas, EventMention.class, token)){
							if(event.getClass().equals(EventMention.class)){//make sure it's a real event
								precedingEvent = event;
								break;
							}
						}
					}
				}
				for(BaseToken token: afterTokens){
					if(token instanceof PunctuationToken && token.getCoveredText().equals(":")){
						hasComma = true;
						break;
					}
				}

				if(hasComma && precedingEvent != null){
					createRelation(jCas, timex, precedingEvent, "CONTAINS", 1d);
					//find the next newline token:
					List<NewlineToken> newlines = JCasUtil.selectFollowing(jCas, NewlineToken.class, timex, 1);
					if(!newlines.isEmpty()){
						NewlineToken newline = newlines.get(0);
						//find all event between timex and newline token
						for(EventMention event : JCasUtil.selectBetween(jCas, EventMention.class, timex, newline)){
							if(event.getClass().equals(EventMention.class)){//make sure it's a real event
//								createRelation(jCas, timex, event, "CONTAINS", 1d);
								createRelation(jCas, precedingEvent, event, "CONTAINS", 1d);
							}
						}
					}
				}

			}
		}





		//4. link this timex to the other events in the same line
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
