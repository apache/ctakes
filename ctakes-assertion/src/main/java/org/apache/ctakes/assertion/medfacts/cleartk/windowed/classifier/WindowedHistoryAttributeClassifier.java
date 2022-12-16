package org.apache.ctakes.assertion.medfacts.cleartk.windowed.classifier;


import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.DocumentAnnotation;

import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/26/2018
 */
public class WindowedHistoryAttributeClassifier {

   private static final String POSTCOORD_NMOD = "donor_srlarg";
   private static final String DISCUSSION_DEPPATH = "discussion_deppath";
   private static final String SUBSUMED_CHUNK = "other_token";
   private static final String SUBSUMED_ANNOT = "other_deppath";
   private static final String IN_HIST_SECTION = "in_history_section";
   public static ArrayList<String> FeatureIndex = new ArrayList<String>();

   static {
      FeatureIndex.add( POSTCOORD_NMOD );
      FeatureIndex.add( DISCUSSION_DEPPATH );
      FeatureIndex.add( SUBSUMED_CHUNK );
      FeatureIndex.add( SUBSUMED_ANNOT );
   }

   // Only enter lower-case strings here for comparison with sentences in isInHistSection
   private static final String[] GHC_HIST_SECTIONS =
         {
               "fh",
               "sh",
               //"hpi",  // based on 8/30 review of errors, this is a pretty lousy indicator of history
               "pmh", // missed a bunch of these in 8/20 run reviewed on 8/30. am i forgetting lowercase?
               "psh",
               "social history:",
               "family history",
               "past medical history",
               "pmh/psh" // missed a bunch of these in 8/20 run reviewed on 8/30. am i forgetting lowercase?
         };

   // currently goes from entityMention to Sentence to SemanticArgument
   public static Boolean getHistory( JCas jCas,
                                     final List<Sentence> sentences,
                                     final Sentence sentence,
                                     final int sentenceIndex,
                                     IdentifiedAnnotation mention ) {

      HashMap<String, Boolean> vfeat = extract( jCas, sentences, sentence, sentenceIndex, mention );

      return classifyWithLogic( vfeat );

   }

   /**
    * @param jCas - the jcas of the document
    * @param arg  - the node getting features added to it
    * @return whether or not arg is a token preceded by "h/o"
    */
   public static Boolean precededByH_O( JCas jCas, Annotation arg ) {
      Boolean answer = false;

      return answer;
   }


   public static Boolean classifyWithLogic( HashMap<String, Boolean> vfeat ) {
      // Logic to identify cases, may be replaced by learned classification
      int subsumectr = 0;
      if ( vfeat.get( SUBSUMED_CHUNK ) ) {
      } //subsumectr++; }
      if ( vfeat.get( SUBSUMED_ANNOT ) ) {
         subsumectr++;
      }
      if ( vfeat.get( POSTCOORD_NMOD ) ) {
         subsumectr++;
      }
      Boolean subsume_summary = (subsumectr > 0);
      if ( vfeat.get( DISCUSSION_DEPPATH ) || subsume_summary ) {
         return true;
      }
      return false;
   }

   /*
    * SRH adding 8/19/13
    * Idea is that I want to know if I am in a "sentence" that starts with
    * a GH history section name.
    * There's some work to be done here.
    * Let's define paragraphs as what's delimited by \n in GH docs
    * Then we can define these sections as I've seen them by what's in a
    * paragraph.
    * But a paragraph may have more than one sentence in it.
    * So I have to actually not find the first part of the sentence that
    * contains the thing, but the paragraph.
    * So actually I have to start from the sentence and search backwards
    * for a newline.
    * So what's written below works (untested/unerified) in the case that I have
    * the starting sentence of a paragraph.
    * But I still have to find that first sentence.
    */
   private static boolean isInHistSection( Sentence s ) {
      // We want to trim the covered text before attempting substring, otherwise the substring call indices can be out of bounds
      String sText = s.getCoveredText().trim();

      for ( String secStart : GHC_HIST_SECTIONS ) {
         int slen = secStart.length();

         if ( sText.length() >= slen ) {
            String sentStart = sText.substring( 0, slen ).toLowerCase();
            if ( sentStart.equals( secStart ) ) {
               return true;
            }
         }
      }

      return false;
   }


   /*
    * This comparator compares two Annotations for location for purposes of
    * sorting. Annotations are equal in location iff begin and end locations are equal.
    * Otherwise, the annotation that has the earlier begin sorts above later begin.
    * If begins are equal but ends are not, then that with earlier end sorts higher.
    */
   public static class SpanComparator implements Comparator<Annotation> {
      public int compare( Annotation a1, Annotation a2 ) {
         final int bDistance = a1.getBegin() - a2.getBegin();
         if ( bDistance != 0 ) {
            return bDistance;
         }
         return a1.getEnd() - a2.getEnd();
      }
   }


   public static HashMap<String, Boolean> extract( JCas jCas,
                                                   final List<Sentence> sentences,
                                                   final Sentence sentence,
                                                   final int sentenceIndex,
                                                   Annotation arg ) {
      final SpanComparator spanComparator = new SpanComparator();
      HashMap<String, Boolean> vfeat = new HashMap<>();
      for ( String feat : FeatureIndex ) {
         vfeat.put( feat, false );
      }

      Sentence sEntity = sentence;

      DocumentAnnotation docAnnot = null;

      Collection<DocumentAnnotation> docAnnots =
            JCasUtil.select( jCas, DocumentAnnotation.class );

      if ( !docAnnots.isEmpty() ) {
         Object[] docAnnotArray = docAnnots.toArray();
         docAnnot = (DocumentAnnotation)docAnnotArray[ 0 ];
      }

      if ( sEntity != null ) {

         // but I actually need to find out if this sentence is preceded by
         // a newline or if I have to find the preceding one that does.
         if ( docAnnot != null ) {
            String doctext = docAnnot.getCoveredText();
            int sentStart = sEntity.getBegin();

            if ( sentStart > 0 ) {
               boolean argInHistSection = false;

               List<Sentence> sentList = sentences;

               // get index of sEntity
               int currind = sentenceIndex;

               if ( currind == 0 ) {
                  argInHistSection = isInHistSection( sEntity );
               } else {
                  currind--;
                  Sentence prevSent = sentList.get( currind );
                  String tweenSents = "";
                  try {
                     tweenSents = doctext.substring( prevSent.getEnd(), sentStart );
                  } catch ( IndexOutOfBoundsException e ) {
                     // this is of no consequence
                     tweenSents = "";
                  }

                  if ( tweenSents.indexOf( "\n" ) != -1 ) {
                     // there is a newline between this sentence and prior sentence
                     argInHistSection = isInHistSection( sEntity );
                  } else if ( currind == 0 ) {
                     argInHistSection = isInHistSection( prevSent );
                  } else {
                     while ( currind > 0 ) {
                        Sentence currSent = prevSent;
                        currind--;
                        prevSent = sentList.get( currind );

                        sentStart = currSent.getBegin();
                        int prevSentEnd = prevSent.getEnd();

                        try {
                           tweenSents = doctext.substring( prevSentEnd, sentStart );
                        } catch ( StringIndexOutOfBoundsException e ) {
                           tweenSents = "";
                        }

                        if ( tweenSents.indexOf( "\n" ) != -1 || currind == 0 ) {
                           argInHistSection = isInHistSection( currSent );
                           break;
                        } else if ( currind == 0 ) {
                           argInHistSection = isInHistSection( prevSent );
                           break;
                        }
                     }
                  }
               }

               // and here do something with argInHistSection.
               // ie, create the feature
               vfeat.put( IN_HIST_SECTION, argInHistSection );
            }

         }

         // 2) some other identified annotation subsumes this one?

         // Get all IdentifiedAnnotations covering the boundaries of the
         // annotation
         List<IdentifiedAnnotation> lsmentions = JCasUtil.selectCovering( jCas,
               IdentifiedAnnotation.class, arg.getBegin(),
               arg.getEnd() );

         Collections.sort( lsmentions, spanComparator );

         // NB: arg is annotation input to this method. annot is current
         // lsmentions in loop
         for ( IdentifiedAnnotation annot : lsmentions ) {
            if ( annot.getBegin() > arg.getBegin() ) {
               // annot starts after our arg, so if ordered correctly(?)
               // then I break b/c I won't find any more that cover arg
               break;
            }

            // INVARIANT: arg starts at or after annot begins
            if ( annot.getEnd() < arg.getEnd() ) {
               // INVARIANT: arg ends at or after annot ends
               continue;
            } else if ( !DependencyUtility.equalCoverage(
                  DependencyUtility.getNominalHeadNode( jCas, annot ),
                  DependencyUtility.getNominalHeadNode( jCas, arg ) ) ) {
               // INVARIANT: arg start at or before annot starts
               // INVARIANT: arg ends at or before annot ends
               // INVARIANT: ergo, arg falls within bounds of annot
               // now verify that annot is an EventMention or EntityMention
               if ( (annot instanceof EntityMention) || (annot instanceof EventMention) ) {
                  // annot has boundaries at or exceeding those of arg.
                  // They also have different head nodes (I guess)
                  // and annot is either an EntityMention of EventMention
                  vfeat.put( SUBSUMED_ANNOT, true );
                  break; // no reason to keep checking
               }
            }
         }

         // 3) some chunk subsumes this?
         List<Chunk> lschunks = JCasUtil.selectPreceding( jCas, Chunk.class, arg, 5 );
         lschunks.addAll( JCasUtil.selectFollowing( jCas, Chunk.class, arg, 5 ) );
         for ( Chunk chunk : lschunks ) {
            if ( chunk.getBegin() > arg.getBegin() ) {
               break;
            }
            if ( chunk.getEnd() < arg.getEnd() ) {
               continue;
            } else if ( !DependencyUtility.equalCoverage(
                  DependencyUtility.getNominalHeadNode( jCas, chunk ),
                  DependencyUtility.getNominalHeadNode( jCas, arg ) ) ) {
               // the case that annot is a superset
               vfeat.put( SUBSUMED_CHUNK, true );
            }
         }
      }


      List<ConllDependencyNode> depnodes = JCasUtil.selectCovered( jCas, ConllDependencyNode.class, arg );
      if ( !depnodes.isEmpty() ) {
         ConllDependencyNode depnode = DependencyUtility.getNominalHeadNode( depnodes );

         // 1) check if the head node of the entity mention is really just part of a larger noun phrase
         if ( depnode.getDeprel().matches( "(NMOD|amod|nmod|det|predet|nn|poss|possessive|infmod|partmod|rcmod)" ) ) {
            vfeat.put( POSTCOORD_NMOD, true );
         }

         // 4) search dependency paths for discussion context
         for ( ConllDependencyNode dn : DependencyUtility.getPathToTop( jCas, depnode ) ) {
            if ( isDiscussionContext( dn ) ) {
               vfeat.put( DISCUSSION_DEPPATH, true );
            }
         }
      }
      return vfeat;
   }


   private static boolean isDonorTerm( Annotation arg ) {
      return arg.getCoveredText().toLowerCase()
                .matches( "(donor).*" );
   }


   private static boolean isDiscussionContext( Annotation arg ) {
      return arg.getCoveredText().toLowerCase()
                .matches( "(discuss|ask|understand|understood|tell|told|mention|talk|speak|spoke|address).*" );
   }


   // a main method for regex testing
   public static void main( String[] args ) {
      String s = "steps";
      if ( s.toLowerCase().matches( ".*(in-law|stepc|stepd|stepso|stepf|stepm|step-).*" ) ) {
         System.out.println( "match" );
      } else {
         System.out.println( "no match" );
      }
   }

}
