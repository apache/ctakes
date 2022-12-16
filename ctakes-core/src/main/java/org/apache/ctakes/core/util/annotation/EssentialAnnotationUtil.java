package org.apache.ctakes.core.util.annotation;


import org.apache.ctakes.core.util.textspan.TextSpan;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/29/2017
 */
final public class EssentialAnnotationUtil {

   static private final Logger LOGGER = Logger.getLogger( "EssentialAnnotationUtil" );

   private EssentialAnnotationUtil() {
   }

   static private final Pattern N_DOT_PATTERN = Pattern.compile( "N..?" );

   static private final java.util.function.Predicate<Annotation> ESSENTIALS
         = a -> EventMention.class.isInstance( a )
                || TimeMention.class.isInstance( a )
                || EntityMention.class.isInstance( a );

   static private Collection<IdentifiedAnnotation> getEssentialAnnotations(
         final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .filter( ESSENTIALS )
                        .collect( Collectors.toList() );
   }

   static private void cullToEssentialAnnotations(
         final Collection<Collection<IdentifiedAnnotation>> annotationCollections ) {
      final Collection<IdentifiedAnnotation> keepers = new HashSet<>();
      for ( Collection<IdentifiedAnnotation> annotations : annotationCollections ) {
         annotations.stream()
                    .filter( ESSENTIALS )
                    .forEach( keepers::add );
         annotations.retainAll( keepers );
         keepers.clear();
      }
   }

   static private Collection<IdentifiedAnnotation> getNonEssentialAnnotations(
         final Collection<IdentifiedAnnotation> allAnnotations,
         final Collection<IdentifiedAnnotation> essentialAnnotations ) {
      return allAnnotations.stream()
                           .filter( a -> !essentialAnnotations.contains( a ) )
                           .filter( a -> !Markable.class.isInstance( a ) )
                           .collect( Collectors.toList() );
   }


   static public Collection<IdentifiedAnnotation> getRequiredAnnotations( final JCas jCas,
                                                                          final Map<IdentifiedAnnotation, Collection<Integer>> corefIndexed ) {
      return getRequiredAnnotations( jCas, JCasUtil.select( jCas, IdentifiedAnnotation.class ), corefIndexed );
   }

   static public Collection<IdentifiedAnnotation> getRequiredAnnotations( final JCas jCas,
                                                                          final Collection<IdentifiedAnnotation> allAnnotations,
                                                                          final Map<IdentifiedAnnotation, Collection<Integer>> corefIndexed ) {
      return getRequiredAnnotations( allAnnotations, corefIndexed, JCasUtil.select( jCas, BinaryTextRelation.class ) );
   }

   static public Collection<IdentifiedAnnotation> getRequiredAnnotations(
         final Collection<IdentifiedAnnotation> allAnnotations,
         final Map<IdentifiedAnnotation, Collection<Integer>> corefIndexed,
         final Collection<BinaryTextRelation> relations ) {
      final Collection<IdentifiedAnnotation> essentialAnnotations = getEssentialAnnotations( allAnnotations );
      // Collection of annotations required to cover all umls annotations, relations, coreferences
      final Collection<IdentifiedAnnotation> requiredAnnotations = new HashSet<>( essentialAnnotations );
      requiredAnnotations.addAll( corefIndexed.keySet() );
      requiredAnnotations.addAll( getRelationAnnotations( relations ) );
      return requiredAnnotations;
   }

   /**
    * @param jCas ye olde ...
    * @return a map of markables to indexed chain numbers
    */
   static public Map<IdentifiedAnnotation, Collection<Integer>> createMarkableCorefs( final JCas jCas ) {
      final Collection<CollectionTextRelation> corefs = JCasUtil.select( jCas, CollectionTextRelation.class );
      final Map<Markable, IdentifiedAnnotation> markableAnnotations = mapMarkableAnnotations( jCas, corefs );
      return createMarkableCorefs( corefs, markableAnnotations );
   }

   /**
    * @param corefs coreference chains
    * @return a map of markables to indexed chain numbers
    */
   static public Map<IdentifiedAnnotation, Collection<Integer>> createMarkableCorefs(
         final Collection<CollectionTextRelation> corefs,
         final Map<Markable, IdentifiedAnnotation> markableAnnotations ) {
      if ( corefs == null || corefs.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<IdentifiedAnnotation, Collection<Integer>> corefMarkables = new HashMap<>();
      int index = 1;
      for ( CollectionTextRelation coref : corefs ) {
         final FSList chainHead = coref.getMembers();
         final Collection<Markable> markables = FSCollectionFactory.create( chainHead, Markable.class );
         for ( Markable markable : markables ) {
            final IdentifiedAnnotation annotation = markableAnnotations.get( markable );
            corefMarkables.putIfAbsent( annotation, new ArrayList<>() );
            corefMarkables.get( annotation )
                          .add( index );
         }
         index++;
      }
      return corefMarkables;
   }

   /**
    * @param corefs coreference chains
    * @return a map of markables to indexed chain numbers
    */
   static public Map<IdentifiedAnnotation, Collection<Integer>> createMarkableAssertedCorefs(
         final Collection<CollectionTextRelation> corefs,
         final Map<Markable, IdentifiedAnnotation> markableAnnotations ) {
      if ( corefs == null || corefs.isEmpty() ) {
         return Collections.emptyMap();
      }

      final List<List<IdentifiedAnnotation>> chains = new ArrayList<>();
      for ( CollectionTextRelation coref : corefs ) {
         final Map<String, List<IdentifiedAnnotation>> assertionMap = new HashMap<>();
         final FSList chainHead = coref.getMembers();
         final Collection<Markable> markables = FSCollectionFactory.create( chainHead, Markable.class );
         for ( Markable markable : markables ) {
            final IdentifiedAnnotation annotation = markableAnnotations.get( markable );
            final String assertion = getAssertion( annotation );
            assertionMap.computeIfAbsent( assertion, a -> new ArrayList<>() ).add( annotation );
         }
         for ( List<IdentifiedAnnotation> asserted : assertionMap.values() ) {
            if ( asserted.size() > 1 ) {
               asserted.sort( Comparator.comparingInt( Annotation::getBegin ) );
               chains.add( asserted );
            }
         }
      }
      chains.sort( ( l1, l2 ) -> l1.get( 0 ).getBegin() - l2.get( 0 ).getBegin() );

      final Map<IdentifiedAnnotation, Collection<Integer>> corefMarkables = new HashMap<>();
      int index = 1;
      for ( Collection<IdentifiedAnnotation> chain : chains ) {
         for ( IdentifiedAnnotation annotation : chain ) {
            corefMarkables.computeIfAbsent( annotation, a -> new ArrayList<>() ).add( index );
         }
         index++;
      }
      return corefMarkables;
   }

   /**
    * Sometimes there will be two overlapping annotations, 1 affirmed 1 negated.
    * Use this for matching assertions in coreference chains.
    *
    * @param jCas ye olde ...
    * @return a map of markables to indexed chain numbers
    */
   static public Map<IdentifiedAnnotation, Collection<Integer>> createAllMarkableAssertedCorefs( final JCas jCas ) {
      final Collection<CollectionTextRelation> corefs = JCasUtil.select( jCas, CollectionTextRelation.class );
      final Map<Markable, Collection<IdentifiedAnnotation>> markableAnnotations
            = mapAllMarkableAnnotations( jCas, corefs );
      return createAllMarkableAssertedCorefs( corefs, markableAnnotations );
   }

   /**
    * @param corefs coreference chains
    * @return a map of markables to indexed chain numbers
    */
   static public Map<IdentifiedAnnotation, Collection<Integer>> createAllMarkableAssertedCorefs(
         final Collection<CollectionTextRelation> corefs,
         final Map<Markable, Collection<IdentifiedAnnotation>> markableAnnotations ) {
      if ( corefs == null || corefs.isEmpty() ) {
         return Collections.emptyMap();
      }

      final List<List<IdentifiedAnnotation>> chains = new ArrayList<>();
      for ( CollectionTextRelation coref : corefs ) {
         final Map<String, List<IdentifiedAnnotation>> assertionMap = new HashMap<>();
         final FSList chainHead = coref.getMembers();
         final Collection<Markable> markables = FSCollectionFactory.create( chainHead, Markable.class );
         for ( Markable markable : markables ) {
            for ( IdentifiedAnnotation annotation : markableAnnotations.get( markable ) ) {
               final String assertion = getAssertion( annotation );
               assertionMap.computeIfAbsent( assertion, a -> new ArrayList<>() ).add( annotation );
            }
         }
         for ( List<IdentifiedAnnotation> asserted : assertionMap.values() ) {
            if ( asserted.size() > 1 ) {
               asserted.sort( Comparator.comparingInt( Annotation::getBegin ) );
               chains.add( asserted );
            }
         }
      }
      chains.sort( ( l1, l2 ) -> l1.get( 0 ).getBegin() - l2.get( 0 ).getBegin() );

      final Map<IdentifiedAnnotation, Collection<Integer>> corefMarkables = new HashMap<>();
      int index = 1;
      for ( Collection<IdentifiedAnnotation> chain : chains ) {
         for ( IdentifiedAnnotation annotation : chain ) {
            corefMarkables.computeIfAbsent( annotation, a -> new ArrayList<>() ).add( index );
         }
         index++;
      }
      return corefMarkables;
   }


   static private String getAssertion( final IdentifiedAnnotation annotation ) {
      final StringBuilder sb = new StringBuilder();
      if ( annotation.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT ) {
         sb.append( "AFFIRMED" );
      } else {
         sb.append( "NEGATED" );
      }
      if ( annotation.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT ) {
         sb.append( "UNCERTAIN" );
      }
      if ( annotation.getGeneric() ) {
         sb.append( "GENERIC" );
      }
      if ( annotation.getConditional() ) {
         sb.append( "CONDITIONAL" );
      }
      return sb.toString();
   }

   /**
    * This is a bit messy, but necessary.
    *
    * @param jCas   -
    * @param corefs -
    * @return map of markable to identified annotation
    */
   static private Map<Markable, IdentifiedAnnotation> mapMarkableAnnotations(
         final JCas jCas, final Collection<CollectionTextRelation> corefs ) {
      if ( corefs == null || corefs.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<Markable, Collection<ConllDependencyNode>> markableNodes
            = JCasUtil.indexCovered( jCas, Markable.class, ConllDependencyNode.class );
      final Map<ConllDependencyNode, Collection<IdentifiedAnnotation>> nodeAnnotations
            = JCasUtil.indexCovering( jCas, ConllDependencyNode.class, IdentifiedAnnotation.class );
      final Map<Markable, IdentifiedAnnotation> annotationMap = new HashMap<>();
      for ( CollectionTextRelation coref : corefs ) {
         final Collection<Markable> markables = JCasUtil.select( coref.getMembers(), Markable.class );
         for ( Markable markable : markables ) {
            final Collection<ConllDependencyNode> nodes = markableNodes.get( markable );
            if ( nodes == null || nodes.isEmpty() ) {
               continue;
            }
            final ConllDependencyNode headNode = getNominalHeadNode( new ArrayList<>( nodes ) );
            final Collection<IdentifiedAnnotation> headNodeAnnotations = nodeAnnotations.get( headNode );
            final Collection<IdentifiedAnnotation> essentialAnnotations
                  = getEssentialAnnotations( headNodeAnnotations );
            final Collection<IdentifiedAnnotation> nonEssentialAnnotations
                  = getNonEssentialAnnotations( headNodeAnnotations,
                  essentialAnnotations );

            IdentifiedAnnotation bestAnnotation = null;
            int bestLength = Integer.MAX_VALUE;
            for ( IdentifiedAnnotation annotation : essentialAnnotations ) {
               if ( !EventMention.class.equals( annotation.getClass() )
                    && annotation.getBegin() == markable.getBegin()
                    && annotation.getEnd() == markable.getEnd() ) {
                  // Prefer an exact non-event match over the longest match
                  bestAnnotation = annotation;
                  break;
               }
               if ( annotation.getEnd() - annotation.getBegin() < bestLength ) {
                  bestLength = annotation.getEnd() - annotation.getBegin();
                  bestAnnotation = annotation;
               }
            }
            if ( bestAnnotation != null ) {
               annotationMap.put( markable, bestAnnotation );
               continue;
            }
            for ( IdentifiedAnnotation annotation : nonEssentialAnnotations ) {
               if ( annotation.getEnd() - annotation.getBegin() < bestLength ) {
                  bestLength = annotation.getEnd() - annotation.getBegin();
                  bestAnnotation = annotation;
               }
            }
            if ( bestAnnotation != null ) {
               annotationMap.put( markable, bestAnnotation );
            } else {
               annotationMap.put( markable, markable );
            }
         }
      }
      return annotationMap;
   }

   /**
    * This is a bit messy, but necessary.
    *
    * @param jCas   -
    * @param corefs -
    * @return map of markable to identified annotation
    */
   static public Map<Markable, Collection<IdentifiedAnnotation>> mapAllMarkableAnnotations(
         final JCas jCas, final Collection<CollectionTextRelation> corefs ) {
      if ( corefs == null || corefs.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<Markable, Collection<ConllDependencyNode>> markableNodes
            = JCasUtil.indexCovered( jCas, Markable.class, ConllDependencyNode.class );
      final Map<ConllDependencyNode, Collection<IdentifiedAnnotation>> nodeAnnotations
            = JCasUtil.indexCovering( jCas, ConllDependencyNode.class, IdentifiedAnnotation.class );
      final Map<Markable, Collection<IdentifiedAnnotation>> annotationMap = new HashMap<>();
      for ( CollectionTextRelation coref : corefs ) {
         final Collection<Markable> markables = JCasUtil.select( coref.getMembers(), Markable.class );
         for ( Markable markable : markables ) {
            final Collection<ConllDependencyNode> nodes = markableNodes.get( markable );
            if ( nodes == null || nodes.isEmpty() ) {
               LOGGER.warn( "No Dependency node for markable " + markable.getCoveredText() );
               continue;
            }
            final ConllDependencyNode headNode = getNominalHeadNode( new ArrayList<>( nodes ) );
            final Collection<IdentifiedAnnotation> headNodeAnnotations = nodeAnnotations.get( headNode );
            final Collection<IdentifiedAnnotation> essentialAnnotations
                  = getEssentialAnnotations( headNodeAnnotations );
            final Collection<IdentifiedAnnotation> nonEssentialAnnotations
                  = getNonEssentialAnnotations( headNodeAnnotations,
                  essentialAnnotations );

//            IdentifiedAnnotation bestAnnotation = null;
            final Collection<IdentifiedAnnotation> bestAnnotations = new ArrayList<>();
            final Collection<IdentifiedAnnotation> overlapAnnotations = new ArrayList<>();
            int bestLength = Integer.MAX_VALUE;
            for ( IdentifiedAnnotation annotation : essentialAnnotations ) {
               if ( !EventMention.class.equals( annotation.getClass() )
                    && annotation.getBegin() == markable.getBegin()
                    && annotation.getEnd() == markable.getEnd()
                    && !bestAnnotations.contains( annotation ) ) {
                  // Prefer an exact non-event match over the longest match
                  bestAnnotations.add( annotation );
               }
               if ( ((annotation.getBegin() <= markable.getBegin() && markable.getEnd() <= annotation.getEnd())
                     || (markable.getBegin() <= annotation.getBegin() && annotation.getEnd() <= markable.getEnd()))
                    && annotation.getEnd() - annotation.getBegin() < bestLength ) {
                  bestLength = annotation.getEnd() - annotation.getBegin();
                  overlapAnnotations.add( annotation );
               }
            }
            if ( bestAnnotations.isEmpty() && bestLength != Integer.MAX_VALUE ) {
               for ( IdentifiedAnnotation annotation : overlapAnnotations ) {
                  if ( annotation.getEnd() - annotation.getBegin() == bestLength ) {
                     bestAnnotations.add( annotation );
                  }
               }
            }

            if ( !bestAnnotations.isEmpty() ) {
               annotationMap.put( markable, bestAnnotations );
               continue;
            }
            for ( IdentifiedAnnotation annotation : nonEssentialAnnotations ) {
               if ( ((annotation.getBegin() <= markable.getBegin() && markable.getEnd() <= annotation.getEnd())
                     || (markable.getBegin() <= annotation.getBegin() && annotation.getEnd() <= markable.getEnd()))
                    && annotation.getEnd() - annotation.getBegin() < bestLength ) {
                  bestLength = annotation.getEnd() - annotation.getBegin();
                  overlapAnnotations.add( annotation );
               }
            }
            if ( bestAnnotations.isEmpty() && bestLength != Integer.MAX_VALUE ) {
               for ( IdentifiedAnnotation annotation : overlapAnnotations ) {
                  if ( annotation.getEnd() - annotation.getBegin() == bestLength ) {
                     bestAnnotations.add( annotation );
                  }
               }
            }

            if ( !bestAnnotations.isEmpty() ) {
               annotationMap.put( markable, bestAnnotations );
            } else {
               annotationMap.put( markable, Collections.singletonList( markable ) );
            }
         }
      }
      return annotationMap;
   }


   /**
    * Finds the head node out of a few ConllDependencyNodes. Biased toward nouns.
    **/
   static public ConllDependencyNode getNominalHeadNode( final List<ConllDependencyNode> nodes ) {
      final ArrayList<ConllDependencyNode> anodes = new ArrayList<>( nodes );
      final Boolean[][] matrixofheads = new Boolean[ anodes.size() ][ anodes.size() ];
      final List<ConllDependencyNode> outnodes = new ArrayList<>();

      // Remove root from consideration
      for ( int i = 0; i < anodes.size(); i++ ) {
         if ( anodes.get( i )
                    .getId() == 0 ) {
            anodes.remove( i );
         }
      }

      // Create a dependency matrix
      for ( int id1 = 0; id1 < anodes.size(); id1++ ) {
         for ( int id2 = 0; id2 < anodes.size(); id2++ ) {
            // no head-dependency relationship between id1 and id2
            matrixofheads[ id2 ][ id1 ]
                  = id1 != id2
                    && anodes.get( id2 ).getHead() != null
                    && anodes.get( id1 ).getId() == anodes.get( id2 ).getHead().getId();
         }
      }

      // Search the dependency matrix for the head
      for ( int idhd = 0; idhd < anodes.size(); idhd++ ) {
         boolean occupiedCol = false;
         for ( int row = 0; row < anodes.size(); row++ ) {
            if ( matrixofheads[ row ][ idhd ] ) {
               occupiedCol = true;
            }
         }
         if ( occupiedCol ) {
            boolean occupiedRow = false;
            for ( int col = 0; col < anodes.size(); col++ ) {
               if ( matrixofheads[ idhd ][ col ] ) {
                  occupiedRow = true;
               }
            }
            if ( !occupiedRow ) {
               outnodes.add( anodes.get( idhd ) );
            }
         }
      }

      // Unheaded phrases
      if ( outnodes.isEmpty() ) {
         // pick a noun from the left, if there is one
         for ( int i = 0; i < anodes.size(); i++ ) {
            if ( anodes.get( i ) != null && anodes.get( i ).getPostag() != null
                 && N_DOT_PATTERN.matcher( anodes.get( i ).getPostag() ).matches() ) {
               return anodes.get( i );
            }
         }
         // default to picking the rightmost node
         return anodes.get( anodes.size() - 1 );
      }
      // Headed phrases
      else {
         // pick a noun from the left, if there is one
         for ( int i = 0; i < outnodes.size(); i++ ) {
            if ( outnodes.get( i ) != null && outnodes.get( i ).getPostag() != null
                 && N_DOT_PATTERN.matcher( outnodes.get( i ).getPostag() ).matches() ) {
               return outnodes.get( i );
            }
         }
         // otherwise, pick the rightmost node with dependencies
         return outnodes.get( outnodes.size() - 1 );
      }
   }


   static public Collection<IdentifiedAnnotation> getRelationAnnotations(
         final Collection<BinaryTextRelation> relations ) {
      final Collection<IdentifiedAnnotation> relationAnnotations = new HashSet<>();
      for ( BinaryTextRelation relation : relations ) {
         IdentifiedAnnotation sourceIA;
         IdentifiedAnnotation targetIA;
         final RelationArgument arg1 = relation.getArg1();
         final Annotation source = arg1.getArgument();
         if ( source instanceof IdentifiedAnnotation ) {
            sourceIA = (IdentifiedAnnotation)source;
         } else {
            LOGGER.error( "Relation source is not an IdentifiedAnnotation " + source.getCoveredText() );
            continue;
         }
         final RelationArgument arg2 = relation.getArg2();
         final Annotation target = arg2.getArgument();
         if ( target instanceof IdentifiedAnnotation ) {
            targetIA = (IdentifiedAnnotation)target;
         } else {
            LOGGER.error( "Relation target is not an IdentifiedAnnotation " + source.getCoveredText() );
            continue;
         }
         relationAnnotations.add( sourceIA );
         relationAnnotations.add( targetIA );
      }
      return relationAnnotations;
   }


   // The assumption is that any given span can only have one exact EventMention.
   static private Collection<IdentifiedAnnotation> getEventMentions(
         final Collection<IdentifiedAnnotation> annotations ) {
      return annotations.stream()
                        .filter( a -> EventMention.class.equals( a.getClass() ) )
                        .collect( Collectors.toList() );
   }

   /**
    * @param annotationMap -
    * @return map of umls annotations to events
    */
   static private Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> getAnnotationEvents(
         final Map<TextSpan, Collection<IdentifiedAnnotation>> annotationMap ) {
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> annotationEvents = new HashMap<>();
      final Map<TextSpan, Collection<IdentifiedAnnotation>> unusedEvents = new HashMap<>();
      for ( Map.Entry<TextSpan, Collection<IdentifiedAnnotation>> entry : annotationMap.entrySet() ) {
         final Collection<IdentifiedAnnotation> annotations = entry.getValue();
         final Collection<IdentifiedAnnotation> eventMentions = getEventMentions( annotations );
         if ( eventMentions != null && !eventMentions.isEmpty() ) {
            if ( annotations.size() > 1 ) {
               final int pre = annotationEvents.size();
               annotations.stream()
                          .filter( EventMention.class::isInstance )
                          .filter( a -> !eventMentions.contains( a ) )
                          .forEach( a -> annotationEvents.put( a, eventMentions ) );
               if ( annotationEvents.size() > pre ) {
                  annotations.removeAll( eventMentions );
               } else {
                  unusedEvents.put( entry.getKey(), eventMentions );
               }
            } else {
               unusedEvents.put( entry.getKey(), eventMentions );
            }
         }
      }
      if ( unusedEvents.isEmpty() ) {
         return annotationEvents;
      }
      final Map<TextSpan, Collection<IdentifiedAnnotation>> usedEvents = new HashMap<>();
      for ( Map.Entry<TextSpan, Collection<IdentifiedAnnotation>> entry : annotationMap.entrySet() ) {
         final TextSpan span = entry.getKey();
         TextSpan usedEventSpan = null;
         for ( Map.Entry<TextSpan, Collection<IdentifiedAnnotation>> unusedEvent : unusedEvents.entrySet() ) {
            if ( !span.equals( unusedEvent.getKey() ) && span.contains( unusedEvent.getKey() ) ) {
               entry.getValue()
                    .stream()
                    .filter( EventMention.class::isInstance )
                    .forEach( a -> annotationEvents.put( a, unusedEvent.getValue() ) );
               usedEventSpan = unusedEvent.getKey();
               usedEvents.put( usedEventSpan, unusedEvent.getValue() );
               break;
            }
         }
         if ( usedEventSpan != null ) {
            unusedEvents.remove( usedEventSpan );
            if ( unusedEvents.isEmpty() ) {
               break;
            }
         }
      }
      usedEvents.forEach( ( s, e ) -> annotationMap.get( s )
                                                   .remove( e ) );
      final Collection<TextSpan> emptySpans = annotationMap.entrySet()
                                                           .stream()
                                                           .filter( e -> e.getValue()
                                                                          .isEmpty() )
                                                           .map( Map.Entry::getKey )
                                                           .collect( Collectors.toList() );
      annotationMap.keySet()
                   .removeAll( emptySpans );
      return annotationEvents;
   }

   static private String createSectionName( final Segment section ) {
      final String sectionPref = section.getPreferredText();
      final String sectionId = section.getId();
      if ( sectionId != null && !sectionId.isEmpty() && !sectionId.equals( sectionPref ) ) {
         if ( sectionPref == null || sectionPref.isEmpty() ) {
            return sectionId;
         }
         return sectionPref + " " + sectionId;
      }
      if ( sectionPref != null && !sectionPref.isEmpty() ) {
         return sectionPref;
      }
      final String tagText = section.getTagText();
      if ( tagText == null || tagText.isEmpty() ) {
         return "Unknown Section";
      }
      return tagText;
   }


}
