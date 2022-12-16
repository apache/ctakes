package org.apache.ctakes.coreference.util;


import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelationIdentifiedAnnotationRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.util.ViewUriUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/6/2017
 */
final public class ClusterMentionFetcher {

   static private final Logger LOGGER = Logger.getLogger( "ClusterMentionFetcher" );

   private ClusterMentionFetcher() {
   }

   static public Map<CollectionTextRelationIdentifiedAnnotationPair,
           CollectionTextRelationIdentifiedAnnotationRelation> getPairRelationsForDocument( final JCas jCas ) throws AnalysisEngineProcessException {
      final Map<CollectionTextRelationIdentifiedAnnotationPair,
              CollectionTextRelationIdentifiedAnnotationRelation> relationLookup = new HashMap<>();
      return addToRelationLookup(jCas, relationLookup);
   }

   static public Map<CollectionTextRelationIdentifiedAnnotationPair,
         CollectionTextRelationIdentifiedAnnotationRelation> getPairRelationsForPatient( final JCas ptCas )
         throws AnalysisEngineProcessException {

      Map<CollectionTextRelationIdentifiedAnnotationPair,
              CollectionTextRelationIdentifiedAnnotationRelation> relationLookup = new HashMap<>();

      JCas[] cases = ThymeCasOrderer.getOrderedCases(ptCas).toArray(new JCas[]{}); //PatientViewUtil.getDocumentViews(ptCas).toArray(new JCas[]{});

      for (JCas docCas : cases) {
         relationLookup = addToRelationLookup(docCas, relationLookup);
      }
      return relationLookup;
   }

   static private Map<CollectionTextRelationIdentifiedAnnotationPair,
           CollectionTextRelationIdentifiedAnnotationRelation> addToRelationLookup( final JCas docCas, final Map<CollectionTextRelationIdentifiedAnnotationPair,
           CollectionTextRelationIdentifiedAnnotationRelation> relationLookup){
         for (CollectionTextRelation cluster : JCasUtil.select(docCas, CollectionTextRelation.class)) {
            for (IdentifiedAnnotation mention : JCasUtil.select(cluster.getMembers(), Markable.class)) {
               final CollectionTextRelationIdentifiedAnnotationRelation relation =
                       new CollectionTextRelationIdentifiedAnnotationRelation(docCas);
               relation.setCluster(cluster);
               relation.setMention(mention);
               relation.setCategory("CoreferenceClusterMember");
               relation.addToIndexes();
               // The key is a list of args so we can do bi-directional lookup
               final CollectionTextRelationIdentifiedAnnotationPair key = new CollectionTextRelationIdentifiedAnnotationPair(cluster, mention);
               if (relationLookup.containsKey(key)) {
                  String category = relationLookup.get(key).getCategory();
                  //System.err.println( "Error in: " + ViewUriUtil.getURI( jCas ).toString() );
                  System.err.println("Error! This attempted relation " + relation.getCategory() + " already has a relation " + category + " at this span: " + mention.getCoveredText());
               }
               relationLookup.put(key, relation);
            }
         }
      return relationLookup;
   }

   static public void populatePairs( final JCas jCas,
                                     final IdentifiedAnnotation mention,
                                     final Collection<Markable> headSet,
                                     final Collection<CollectionTextRelationIdentifiedAnnotationPair> pairs ) {
      for ( CollectionTextRelation cluster : JCasUtil.select( jCas, CollectionTextRelation.class ) ) {
         final FSList members = cluster.getMembers();
         final Annotation mostRecent = ClusterUtils.getMostRecent( (NonEmptyFSList) members, mention );
         if ( mostRecent == null ) {
            continue;
         }
         for ( Markable m : JCasUtil.select( members, Markable.class ) ) {
            if ( headSet.contains( mostRecent ) ) {
               pairs.add( new CollectionTextRelationIdentifiedAnnotationPair( cluster, mention ) );
               break;
            }
            if ( m == mostRecent ) {
               break;
            }
         }
      }
   }


   public static class CollectionTextRelationIdentifiedAnnotationPair {
      private final CollectionTextRelation cluster;
      private final IdentifiedAnnotation mention;

      public CollectionTextRelationIdentifiedAnnotationPair( final CollectionTextRelation cluster, final IdentifiedAnnotation mention ) {
         this.cluster = cluster;
         this.mention = mention;
      }

      public final CollectionTextRelation getCluster() {
         return this.cluster;
      }

      public final IdentifiedAnnotation getMention() {
         return this.mention;
      }

      @Override
      public boolean equals( final Object object ) {
         if ( !CollectionTextRelationIdentifiedAnnotationPair.class.isInstance( object ) ) {
            return false;
         }
         final CollectionTextRelationIdentifiedAnnotationPair other
               = (CollectionTextRelationIdentifiedAnnotationPair) object;
         return this.cluster == other.cluster && this.mention == other.mention;
      }

      @Override
      public int hashCode() {
         return 31 * cluster.hashCode() + (mention == null ? 0 : mention.hashCode());
      }
   }


}
