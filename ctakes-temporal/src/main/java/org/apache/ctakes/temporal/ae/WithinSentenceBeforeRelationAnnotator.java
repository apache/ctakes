package org.apache.ctakes.temporal.ae;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.temporal.duration.Utils;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

public class WithinSentenceBeforeRelationAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		//1. find all timex that can be normalized to timestamp. form a map of timex-timestamp
		Map<TimeMention, Timestamp> timeNorm = new HashMap<>();
		//find docTime:
		TimeMention docTime = null;
		for (TimeMention timex : JCasUtil.select(jCas, TimeMention.class)) {
			if(timex.getTimeClass().equals("DOCTIME")){
				docTime = timex;
				break;
			}
		}
		if(docTime != null){
			for (TimeMention timex : JCasUtil.select(jCas, TimeMention.class)) {
				if(!timex.getTimeClass().equals("DOCTIME")){
					//add normalized timex
					String value = Utils.getTimexMLValue(timex.getCoveredText(), docTime.getCoveredText());
					if(value != null){
						try{
							SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
							Date parsedDate = dateFormat.parse(value);
							Timestamp timestamp = new Timestamp(parsedDate.getTime());
							timeNorm.put(timex, timestamp);
						}catch(Exception e){//this generic but you can control another types of exception
							System.out.println("cannot parse timex :" + value);
							continue;
						}
					}
				}
			}
		}
		
		//find all timex that involved in temporal relations:
		Set<TimeMention> relationalTimex = new HashSet<>();
		for(RelationArgument relarg: JCasUtil.select(jCas, RelationArgument.class)){
			Annotation arg = relarg.getArgument();
			if(arg instanceof TimeMention){
				relationalTimex.add((TimeMention) arg);
			}
		}
		relationalTimex.retainAll(timeNorm.keySet());
		
		//find all events that contained by those timex
		Map<TimeMention, Set<EventMention>> timeEvents = new HashMap<>();
		for(TimeMention time : relationalTimex){
			Set<EventMention> containedEvents = new HashSet<>();
			for (TemporalTextRelation rel : JCasUtil.select(jCas, TemporalTextRelation.class)){
				if(rel.getCategory().equals("CONTAINS")){
					if(rel.getArg1().getArgument()==time){
						containedEvents.add((EventMention)rel.getArg2().getArgument());
					}
				}else if(rel.getCategory().equals("CONTAINS-1")){
					if(rel.getArg2().getArgument()==time){
						containedEvents.add((EventMention)rel.getArg1().getArgument());
					}
				}
			}
			timeEvents.put(time, containedEvents);
		}

		//iterate the List of timx, find all pairs of timex that are in the same line -- i.e. there is no newLine in between
		int timexNum = relationalTimex.size();
		List<TimeMention> timexLst = new ArrayList<>(relationalTimex);
		for (int i=0; i< timexNum-1; i++){
			TimeMention timeA = timexLst.get(i);
			for(int j= i+1; j < timexNum; j++){
				TimeMention timeB = timexLst.get(j);
				//check if timeA and timeB are in the same line:
				if(timeA!=timeB && JCasUtil.selectBetween(jCas, NewlineToken.class, timeA, timeB).isEmpty()){
					Timestamp stampA = timeNorm.get(timeA);
					Timestamp stampB = timeNorm.get(timeB);
					int compareResult =stampA.compareTo(stampB);
					if(compareResult<0){//if before
						for(EventMention eventA : timeEvents.get(timeA)){
							for(EventMention eventB: timeEvents.get(timeB)){
								if(eventA != eventB){
									createRelation(jCas, eventA, eventB, "BEFORE", 1d);
								}
							}
						}
						
					}else if(compareResult>0){//if after
						for(EventMention eventB : timeEvents.get(timeB)){
							for(EventMention eventA: timeEvents.get(timeA)){
								if(eventA != eventB){
									createRelation(jCas, eventB, eventA, "BEFORE", 1d);
								}
							}
						}
						
					}else{//if they are equal
						Set<EventMention> groupA = new HashSet<>();
						groupA.addAll(timeEvents.get(timeA));
						groupA.removeAll(timeEvents.get(timeB));
						for(EventMention event: groupA){
							createRelation(jCas, timeB, event, "CONTAINS", 1d);
						}
						Set<EventMention> groupB = new HashSet<>();
						groupB.addAll(timeEvents.get(timeB));
						groupB.removeAll(timeEvents.get(timeA));
						for(EventMention event: groupB){
							createRelation(jCas, timeA, event, "CONTAINS", 1d);
						}
						
					}
					
				}
			}
		}

		//3. if so find the relationship between those two timestamp, then induce the relations of covered events.
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
