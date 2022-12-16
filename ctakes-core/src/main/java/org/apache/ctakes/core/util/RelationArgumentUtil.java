package org.apache.ctakes.core.util;


import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 5/25/2016
 */
@Immutable
final public class RelationArgumentUtil {
   private RelationArgumentUtil() {
   }

   static private final Logger LOGGER = Logger.getLogger( "RelationUtil" );


   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all identified annotations in the given relations related to the given annotation as the first argument
    */
   static public Collection<IdentifiedAnnotation> getFirstArguments(
         final Collection<? extends BinaryTextRelation> relations,
         final IdentifiedAnnotation annotation ) {
      return relations.stream()
            .filter( r -> r.getArg2().getArgument().equals( annotation ) )
            .map( r -> r.getArg1().getArgument() )
            .filter( IdentifiedAnnotation.class::isInstance )
            .map( a -> (IdentifiedAnnotation) a )
            .collect( Collectors.toList() );
   }

   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all identified annotations in the given relations related to the given annotation as the second argument
    */
   static public Collection<IdentifiedAnnotation> getSecondArguments(
         final Collection<? extends BinaryTextRelation> relations,
         final IdentifiedAnnotation annotation ) {
      return relations.stream()
            .filter( r -> r.getArg1().getArgument().equals( annotation ) )
            .map( r -> r.getArg2().getArgument() )
            .filter( IdentifiedAnnotation.class::isInstance )
            .map( a -> (IdentifiedAnnotation) a )
            .collect( Collectors.toList() );
   }

   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all identified annotations in the given relations related to the given annotation
    */
   static public Collection<IdentifiedAnnotation> getAllRelated(
         final Collection<? extends BinaryTextRelation> relations,
         final IdentifiedAnnotation annotation ) {
      return relations.stream()
            .map( r -> getRelated( r, annotation ) )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );
   }

   static private Map.Entry<BinaryTextRelation, IdentifiedAnnotation> getRelationEntry( final BinaryTextRelation relation,
                                                                                        final IdentifiedAnnotation annotation ) {
      final IdentifiedAnnotation related = getRelated( relation, annotation );
      if ( related == null ) {
         return null;
      }
      return new AbstractMap.SimpleEntry<>( relation, related );
   }


   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all identified annotations in the given relations related to the given annotation
    */
   static public Map<BinaryTextRelation, IdentifiedAnnotation> getAllRelatedMap(
         final Collection<BinaryTextRelation> relations,
         final IdentifiedAnnotation annotation ) {
      final Map<BinaryTextRelation, IdentifiedAnnotation> map = new HashMap<>();
      relations.stream()
            .map( r -> getRelationEntry( r, annotation ) )
            .filter( Objects::nonNull )
            .forEach( e -> map.put( e.getKey(), e.getValue() ) );
      return map;
   }


   /**
    * @param relation   relation of interest
    * @param annotation some annotation that might be in the relation
    * @return the other annotation in the relation if the first is present
    */
   static private IdentifiedAnnotation getRelated( final BinaryTextRelation relation,
                                                   final IdentifiedAnnotation annotation ) {
      Annotation argument = null;
      if ( relation.getArg1().getArgument().equals( annotation ) ) {
         argument = relation.getArg2().getArgument();
      } else if ( relation.getArg2().getArgument().equals( annotation ) ) {
         argument = relation.getArg1().getArgument();
      }
      if ( argument != null && IdentifiedAnnotation.class.isInstance( argument ) ) {
         return (IdentifiedAnnotation) argument;
      }
      return null;
   }


   /**
    * @param relation   relation of interest
    * @param annotation some annotation that might be in the relation
    * @return the target annotation in the relation if the first is present
    */
   static private IdentifiedAnnotation getTarget( final BinaryTextRelation relation,
                                                  final IdentifiedAnnotation annotation ) {
      if ( !relation.getArg1()
            .getArgument()
            .equals( annotation ) ) {
         return null;
      }
      final Annotation argument = relation.getArg2()
            .getArgument();
      if ( argument != null && IdentifiedAnnotation.class.isInstance( argument ) ) {
         return (IdentifiedAnnotation) argument;
      }
      return null;
   }

   static private Map.Entry<BinaryTextRelation, IdentifiedAnnotation> getRelationTargetEntry( final BinaryTextRelation relation,
                                                                                              final IdentifiedAnnotation annotation ) {
      final IdentifiedAnnotation related = getTarget( relation, annotation );
      if ( related == null ) {
         return null;
      }
      return new AbstractMap.SimpleEntry<>( relation, related );
   }

   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all identified annotations in the given relations related to the given annotation
    */
   static public Map<BinaryTextRelation, IdentifiedAnnotation> getRelatedTargetsMap(
         final Collection<BinaryTextRelation> relations,
         final IdentifiedAnnotation annotation ) {
      final Map<BinaryTextRelation, IdentifiedAnnotation> map = new HashMap<>();
      relations.stream()
            .map( r -> getRelationTargetEntry( r, annotation ) )
            .filter( Objects::nonNull )
            .forEach( e -> map.put( e.getKey(), e.getValue() ) );
      return map;
   }

   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all relations in the given relations where the first argument is the given annotation
    */
   static public <T extends BinaryTextRelation> Collection<T> getRelationsAsFirst(
         final Collection<T> relations,
         final IdentifiedAnnotation annotation ) {
      return relations.stream()
            .filter( r -> r.getArg1().getArgument().equals( annotation ) )
            .collect( Collectors.toList() );
   }

   /**
    * @param relations  relations of interest
    * @param annotation identified annotation of interest
    * @return all relations in the given relations where the second argument is the given annotation
    */
   static public <T extends BinaryTextRelation> Collection<T> getRelationsAsSecond(
         final Collection<T> relations,
         final IdentifiedAnnotation annotation ) {
      return relations.stream()
            .filter( r -> r.getArg2().getArgument().equals( annotation ) )
            .collect( Collectors.toList() );
   }


   /**
    * Candidates for primary annotations of a possible relation are preceding candidates in the same paragraph,
    * or if none then nearest following candidate in the same sentence
    *
    * @param jcas            ye olde ...
    * @param candidates      all candidates of a type in a paragraph
    * @param mainAnnotations all primary annotations of a type that has a relation in a paragraph
    * @return a map of candidates and the primary annotations for those candidates
    */
   static public Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> createCandidateMap(
         final JCas jcas,
         final List<IdentifiedAnnotation> candidates, final List<IdentifiedAnnotation> mainAnnotations ) {
      if ( candidates.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<IdentifiedAnnotation, Collection<Sentence>> coveringSentences
            = JCasUtil.indexCovering( jcas, IdentifiedAnnotation.class, Sentence.class );
      final Map<IdentifiedAnnotation, Collection<IdentifiedAnnotation>> candidateMap = new HashMap<>( candidates.size() );
      int nextJ = mainAnnotations.size() - 1;
      for ( int i = candidates.size() - 1; i >= 0; i-- ) {
         final IdentifiedAnnotation candidate = candidates.get( i );
         final int candidateBegin = candidate.getBegin();
         final Collection<IdentifiedAnnotation> candidateMains = new HashSet<>();
         for ( int j = nextJ; j >= 0; j-- ) {
            final IdentifiedAnnotation main = mainAnnotations.get( j );
            final int mainEnd = main.getEnd();
            if ( mainEnd > candidateBegin && !candidate.equals( main ) ) {
               // main is after or overlapping candidate, so add
               candidateMains.add( main );
               if ( j == 0 ) {
                  // End of mains, set nextJ below zero
                  nextJ = -1;
                  break;
               }
            } else {
               nextJ = j;
               break;
            }
         }
         if ( !candidateMains.isEmpty() ) {
            candidateMap.put( candidate, candidateMains );
         }
      }
      if ( nextJ >= 0 ) {
         // some primary annotation(s) before property.  Get the last one and test for same sentence
         final IdentifiedAnnotation main = mainAnnotations.get( nextJ );
         final IdentifiedAnnotation firstCandidate = candidates.get( 0 );
         if ( !firstCandidate.equals( main ) && coveringSentences.get( main ).equals( coveringSentences.get( firstCandidate ) ) ) {
            final Collection<IdentifiedAnnotation> candidateMains = candidateMap.computeIfAbsent( firstCandidate, s -> new HashSet<>( 1 ) );
            candidateMains.add( main );
         }
      }
      return candidateMap;
   }

   /**
    * @param jCas     ye olde ...
    * @param argument -
    * @param target   -
    * @param name     name of relation type
    * @return created relation or null if there was a problem
    */
   static public BinaryTextRelation createRelation( final JCas jCas,
                                                    final IdentifiedAnnotation argument,
                                                    final IdentifiedAnnotation target,
                                                    final String name ) {
      if ( name.equals( "hasBodySite" ) ) {
         return createRelation( jCas, new LocationOfTextRelation( jCas ), argument, target, name );
      }
      return createRelation( jCas, new BinaryTextRelation( jCas ), argument, target, name );
   }

   /**
    * @param jCas     ye olde ...
    * @param relation -
    * @param argument -
    * @param target   -
    * @param name     name of relation type
    * @return created relation or null if there was a problem
    */
   static public <T extends BinaryTextRelation> T createRelation( final JCas jCas,
                                                                  T relation,
                                                                  final IdentifiedAnnotation argument,
                                                                  final IdentifiedAnnotation target,
                                                                  final String name ) {
      if ( argument == null ) {
         LOGGER.info( "No argument for " + ((target != null) ? target.getCoveredText() : "") );
         return null;
      }
      if ( target == null ) {
         LOGGER.info( "No target to relate to " + argument.getCoveredText() );
         return null;
      }
      if ( argument.equals( target ) ) {
         LOGGER.warn( "Argument and target are identical " + argument.getCoveredText() );
         return null;
      }
      final RelationArgument relationArgument = new RelationArgument( jCas );
      relationArgument.setArgument( argument );
      relationArgument.setRole( "Argument" );
      relationArgument.addToIndexes();
      final RelationArgument relationTarget = new RelationArgument( jCas );
      relationTarget.setArgument( target );
      relationTarget.setRole( "Related_to" );
      relationTarget.addToIndexes();
      relation.setArg1( relationArgument );
      relation.setArg2( relationTarget );
      relation.setCategory( name );
      // add the relation to the CAS
      relation.addToIndexes();
      return relation;
   }

}
