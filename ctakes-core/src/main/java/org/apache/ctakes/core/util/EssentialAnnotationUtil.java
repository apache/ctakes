package org.apache.ctakes.core.util;


import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/29/2017
 * @deprecated Use EssentialAnnotationUtil in (sub) package annotation
 */
@Deprecated
final public class EssentialAnnotationUtil {

   private EssentialAnnotationUtil() {
   }

   @Deprecated
   static public Collection<IdentifiedAnnotation> getRequiredAnnotations( final JCas jCas,
                                                                          final Map<IdentifiedAnnotation, Collection<Integer>> corefIndexed ) {
      return org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil.getRequiredAnnotations( jCas, corefIndexed );
   }

   @Deprecated
   static public Collection<IdentifiedAnnotation> getRequiredAnnotations( final JCas jCas,
                                                                          final Collection<IdentifiedAnnotation> allAnnotations,
                                                                          final Map<IdentifiedAnnotation, Collection<Integer>> corefIndexed ) {
      return org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil.getRequiredAnnotations( jCas, allAnnotations, corefIndexed );
   }

   @Deprecated
   static public Collection<IdentifiedAnnotation> getRequiredAnnotations( final Collection<IdentifiedAnnotation> allAnnotations,
                                                                          final Map<IdentifiedAnnotation, Collection<Integer>> corefIndexed,
                                                                          final Collection<BinaryTextRelation> relations ) {
      return org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil.getRequiredAnnotations( allAnnotations, corefIndexed, relations );
   }

   /**
    * @param jCas ye olde ...
    * @return a map of markables to indexed chain numbers
    */
   @Deprecated
   static public Map<IdentifiedAnnotation, Collection<Integer>> createMarkableCorefs( final JCas jCas ) {
      return org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil.createMarkableCorefs( jCas );
   }

   /**
    * @param corefs coreference chains
    * @return a map of markables to indexed chain numbers
    */
   @Deprecated
   static public Map<IdentifiedAnnotation, Collection<Integer>> createMarkableCorefs(
         final Collection<CollectionTextRelation> corefs,
         final Map<Markable, IdentifiedAnnotation> markableAnnotations ) {
      return org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil.createMarkableCorefs( corefs, markableAnnotations );
   }

   /**
    * @param corefs coreference chains
    * @return a map of markables to indexed chain numbers
    */
   @Deprecated
   static public Map<IdentifiedAnnotation, Collection<Integer>> createMarkableAssertedCorefs(
         final Collection<CollectionTextRelation> corefs,
         final Map<Markable, IdentifiedAnnotation> markableAnnotations ) {
      return org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil.createMarkableAssertedCorefs( corefs, markableAnnotations );
   }

   /**
    * Finds the head node out of a few ConllDependencyNodes. Biased toward nouns.
    **/
   @Deprecated
   static public ConllDependencyNode getNominalHeadNode( final List<ConllDependencyNode> nodes ) {
      return org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil.getNominalHeadNode( nodes );
   }

   @Deprecated
   static public Collection<IdentifiedAnnotation> getRelationAnnotations(
         final Collection<BinaryTextRelation> relations ) {
      return org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil.getRelationAnnotations( relations );
   }


}
