package org.apache.ctakes.temporal.ae;


import com.google.common.collect.Lists;
import org.apache.ctakes.core.util.annotation.WordTokenUtil;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

//import org.apache.ctakes.typesystem.type.textspan.Segment;

public class TemporalRelationRuleAnnotator extends JCasAnnotator_ImplBase {

   @SuppressWarnings( "null" )
   @Override
   public void process( JCas jCas ) throws AnalysisEngineProcessException {

      //1: linking E0-T0, E1-T1:
      Collection<Sentence> sents = JCasUtil.select( jCas, Sentence.class );
      List<Sentence> sentList = Lists.newArrayList();
      sentList.addAll( sents );
      EventMention admission = null;
      //		EventMention discharge = null;
      //		TimeMention  admissionDate = null;
      //		TimeMention  dischargeDate = null;
      int sentListLength = sentList.size();
      if ( sentListLength >= 4 ) {//the first 4 sentences are discharge date and admission date
         for ( int i = 0; i < 4; i += 2 ) {
            Sentence currentSent = sentList.get( i );
            Sentence nextSent = sentList.get( i + 1 );
            List<EventMention> currentEvents = JCasUtil.selectCovered( jCas, EventMention.class, currentSent );
            List<TimeMention> nextTimes = JCasUtil.selectCovered( jCas, TimeMention.class, nextSent );

            int currentSize = currentEvents == null ? 0 : currentEvents.size();
            int nextTimeSize = nextTimes == null ? 0 : nextTimes.size();

            if ( currentSize == 0 || nextTimeSize == 0 ) {
               continue;
            }

            EventMention currentEvent = currentEvents.get( 0 );
            TimeMention nextTime = nextTimes.get( 0 );

            if ( i == 0 ) {
               admission = currentEvent;
               //					admissionDate = nextTime;
               //				}else{
               //					discharge = currentEvent;
               //					dischargeDate = nextTime;
            }

            createRelation( jCas, currentEvent, nextTime, "OVERLAP" );
         }
      }

      //rule 3: link Timexes with the same strings
      Collection<TimeMention> times = JCasUtil.select( jCas, TimeMention.class );
      List<TimeMention> allTimes = Lists.newArrayList();
      allTimes.addAll( times );
      int timeNum = allTimes.size();
      if ( timeNum > 2 ) {
         for ( int i = 0; i < timeNum - 1; i++ ) {
            TimeMention firstTime = allTimes.get( i );
            for ( int j = i + 1; j < timeNum; j++ ) {
               TimeMention secondTime = allTimes.get( j );
               if ( sameTime( jCas, firstTime, secondTime ) ) {
                  createRelation( jCas, secondTime, firstTime, "OVERLAP" );
               }
            }
         }
      }

      //2: linking coreferent event pairs, lift section restriction
      Collection<Sentence> sentences = JCasUtil.select( jCas, Sentence.class );

      Collection<EventMention> allEvents = JCasUtil.select( jCas, EventMention.class );
      List<EventMention> realEvents = new ArrayList<>();
      //filtering events
      for ( EventMention event : allEvents ) {
         // filter out ctakes events
         if ( event.getClass().equals( EventMention.class ) ) {
            realEvents.add( event );
         }
      }
      allEvents = realEvents;

      for ( Sentence sent : sentences ) {
         List<EventMention> currentEvents = JCasUtil.selectCovered( jCas, EventMention.class, sent );
         //filter out ctakes events
         realEvents = new ArrayList<>();
         for ( EventMention event : currentEvents ) {
            // filter out ctakes events
            if ( event.getClass().equals( EventMention.class ) ) {
               realEvents.add( event );
            }
         }
         currentEvents = realEvents;

         //get dependent pairs:
//			int eventNum = currentEvents.size();
//			if(eventNum >= 4){
//				EventMention first = currentEvents.get(0);
//				//find dependent pairs between first and the rest
//				for (ConllDependencyNode firstNode : JCasUtil.selectCovered(jCas, ConllDependencyNode.class, first)) {//get the covered conll nodes within the first event
//					String pos = firstNode.getPostag();
//					if(pos.startsWith("NN")||pos.startsWith("VB")){//get the head node
//						for(int j=1;j<eventNum;j++){
//							EventMention nextEvent = currentEvents.get(j);
//							for(ConllDependencyNode nextNode : JCasUtil.selectCovered(jCas, ConllDependencyNode.class, nextEvent)){//get the covered conll nodes within the next event
//								pos = nextNode.getPostag();
//								if(pos.startsWith("NN")||pos.startsWith("VB")){//get the head node
//									ConllDependencyNode ancestor = DependencyParseUtils.getCommonAncestor(firstNode, nextNode);
//									if(ancestor==firstNode || ancestor==nextNode){
//										createRelation(jCas, nextEvent, first, "OVERLAP");
//										break;
//									}
//								}
//							}
//						}
//					}
//
//
//				}
//			}

         //remove current Events from allEvents:
         for ( EventMention event : currentEvents ) {
            allEvents.remove( event );
            //check if current event is the admission event
            if ( admission != null && event != admission &&
                 event.getCoveredText().toLowerCase().startsWith( "admitted" ) ) {
               createRelation( jCas, event, admission, "OVERLAP" );
            }
         }

         for ( EventMention arg1 : currentEvents ) {
            for ( EventMention arg2 : allEvents ) {
               if ( hasOverlapNNs( jCas, arg1, arg2 ) ) {//hasSameSemanticType(jCas, arg1, arg2) &&
                  createRelation( jCas, arg2, arg1, "OVERLAP" );
               }
            }
         }

      }
   }

   //	private static boolean hasSameSemanticType(JCas jCas, EventMention arg1,
   //			EventMention arg2) {
   //		List<EventMention> arg1Events = JCasUtil.selectCovered(jCas, EventMention.class, arg1);
   //		List<EventMention> arg2Events = JCasUtil.selectCovered(jCas, EventMention.class, arg2);
   //		for (EventMention event1 : arg1Events){
   //			if(!event1.getClass().equals(EventMention.class)){//&& event1.getBegin()==arg1.getBegin() && event1.getEnd()==arg1.getEnd()){
   //				for (EventMention event2 : arg2Events){
   //					if(!event2.getClass().equals(EventMention.class)){// && event2.getBegin()==arg2.getBegin() && event2.getEnd()==arg2.getEnd()){
   //						if(event1.getClass().equals(event2.getClass())){
   //							return true;
   //						}
   //					}
   //				}
   //			}
   //		}
   //		return false;
   //	}

   private static boolean sameTime( JCas jCas, TimeMention firstTime,
                                    TimeMention secondTime ) {
      List<BaseToken> currentTokens = JCasUtil.selectCovered( jCas, BaseToken.class, firstTime );
      List<BaseToken> nextTokens = JCasUtil.selectCovered( jCas, BaseToken.class, secondTime );
      int tokenSize = currentTokens.size();
      if ( tokenSize != nextTokens.size() ) {
         return false;
      }
      for ( int i = 0; i < tokenSize; i++ ) {
         if ( !currentTokens.get( i ).getCoveredText().equals( nextTokens.get( i ).getCoveredText() ) ) {
            return false;
         }
      }
      return true;
   }

   private static void createRelation( JCas jCas, IdentifiedAnnotation arg1,
                                       IdentifiedAnnotation arg2, String cagegory ) {
      RelationArgument relArg1 = new RelationArgument( jCas );
      relArg1.setArgument( arg1 );
      relArg1.setRole( "Arg1" );
      relArg1.addToIndexes();
      RelationArgument relArg2 = new RelationArgument( jCas );
      relArg2.setArgument( arg2 );
      relArg2.setRole( "Arg2" );
      relArg2.addToIndexes();
      TemporalTextRelation relation = new TemporalTextRelation( jCas );
      relation.setArg1( relArg1 );
      relation.setArg2( relArg2 );
      relation.setCategory( cagegory );
      relation.addToIndexes();
   }

   /**
    * Method for checking if two arguments share some common NNs ob VBs.
    *
    * @param jCas
    * @param event1
    * @param event2
    * @return
    */
   private static boolean hasOverlapNNs( final JCas jCas, final EventMention event1, final EventMention event2 ) {
      final Collection<WordToken> currentTokens = JCasUtil.selectCovered( jCas, WordToken.class, event1 );
      final Collection<WordToken> nextTokens = JCasUtil.selectCovered( jCas, WordToken.class, event2 );
      if ( currentTokens == null || currentTokens.isEmpty() || nextTokens == null || nextTokens.isEmpty() ) {
         return false;
      }
      int NNSize1 = 0;
      int NNSize2 = 0;
      int matches = 0;
      for ( WordToken t1 : currentTokens ) {
         if ( t1.getPartOfSpeech().startsWith( "NN" ) || t1.getPartOfSpeech().startsWith( "VB" ) ) {
            NNSize1++;
            for ( WordToken t2 : nextTokens ) {
               if ( t2.getPartOfSpeech().startsWith( "NN" ) || t2.getPartOfSpeech().startsWith( "VB" ) ) {
                  NNSize2++;
                  if ( WordTokenUtil.getCanonicalForm( t1 ).equals( WordTokenUtil.getCanonicalForm( t2 ) ) ) {
                     matches++;
                  }
               }
            }
         }
      }
      final int NNSize = Math.min( NNSize1, NNSize2 );
      if ( NNSize == 0 ) {
         return false;
      }
      final float matchRatio = (float)matches / NNSize;
      // Try to avoid [float1] == [float2] primitive comparison
      return Float.compare( matchRatio, 1f ) == 0;
   }

}
