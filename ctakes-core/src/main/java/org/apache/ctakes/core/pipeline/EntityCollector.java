package org.apache.ctakes.core.pipeline;


import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stores a collection of simple entity information from a run, along with their associated Document Ids
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2016
 */
public enum EntityCollector {
   INSTANCE;

   static public EntityCollector getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "EntityCollector" );


   private final Map<String, Collection<Entity>> _entityMap = new HashMap<>();

   /**
    * @return Ids for documents that have entity information stored in the EntityCollector
    */
   public Collection<String> getDocumentIds() {
      return Collections.unmodifiableCollection( _entityMap.keySet() );
   }

   /**
    * @param documentId id for some document
    * @return simple entity objects for the document
    */
   public Collection<Entity> getEntities( final String documentId ) {
      final Collection<Entity> entities = _entityMap.get( documentId );
      if ( entities == null ) {
         LOGGER.warn( "No Entities for " + documentId );
         return Collections.emptyList();
      }
      return Collections.unmodifiableCollection( entities );
   }

   /**
    * @return staggered list of document ids, entities and entity properties
    */
   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder();
      for ( Map.Entry<String, Collection<Entity>> entry : _entityMap.entrySet() ) {
         sb.append( entry.getKey() ).append( "\n" );
         entry.getValue().stream()
               .sorted( ( e1, e2 ) -> e1._begin - e2._begin )
               .map( Entity::toString )
               .forEach( sb::append );
      }
      return sb.toString();
   }

   /**
    * Holds basic information from an IdentifiedAnnotation.
    * Necessary so the IdentifiedAnnotation can be cleaned from the Cas
    */
   static public final class Entity {
      private final int _begin;
      private final int _end;
      private final String _coveredText;
      private final int _polarity;
      private final int _uncertainty;
      private final boolean _conditional;
      private final boolean _generic;
      private final String _subject;
      private final int _historyOf;

      private Entity( final IdentifiedAnnotation annotation ) {
         _begin = annotation.getBegin();
         _end = annotation.getEnd();
         _coveredText = annotation.getCoveredText();
         _polarity = annotation.getPolarity();
         _uncertainty = annotation.getUncertainty();
         _conditional = annotation.getConditional();
         _generic = annotation.getGeneric();
         _subject = annotation.getSubject();
         _historyOf = annotation.getHistoryOf();
      }

      /**
       * @return row of entity properties
       */
      @Override
      public String toString() {
         final StringBuilder sb = new StringBuilder();
         sb.append( '\t' ).append( _begin ).append( ',' ).append( _end ).append( '\t' )
               .append( _coveredText ).append( "\n" );
         sb.append( "\t\t| " ).append( _polarity < 0 ? "negated" : "affirmed" );
         sb.append( " | " ).append( _uncertainty < 0 ? "uncertain" : "certain" );
         sb.append( " | " ).append( _conditional ? "conditional" : "not conditional" );
         sb.append( " | " ).append( _generic ? "generic" : "not generic" );
         sb.append( " | " ).append( _subject == null ? "Patient" : _subject );
         sb.append( " | history of: " ).append( _historyOf );
         sb.append( '\n' );
         return sb.toString();
      }

      @Override
      public int hashCode() {
         return toString().hashCode();
      }

      @Override
      public boolean equals( final Object other ) {
         return toString().equals( other.toString() );
      }
   }

   /**
    * Analysis Engine that stores collections of cuis by document id in the CuiCollector
    */
   @PipeBitInfo(
         name = "Entity Collector",
         description = "Collects information on entities generated during a run.",
         role = PipeBitInfo.Role.SPECIAL,
         dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
   )
   static public final class EntityCollectorEngine extends JCasAnnotator_ImplBase {
      @Override
      public void process( final JCas jCas ) {
         LOGGER.info( "Starting processing" );
         final String id = DocIdUtil.getDeepDocumentId( jCas );
         final Collection<IdentifiedAnnotation> annotations = JCasUtil.select( jCas, IdentifiedAnnotation.class );
         putEntities( id, annotations );
         LOGGER.info( "Finished processing" );
      }

      static private void putEntities( final String documentId, final Collection<IdentifiedAnnotation> annotations ) {
         final Collection<Entity> entities = annotations.stream().map( Entity::new ).collect( Collectors.toSet() );
         EntityCollector.getInstance()._entityMap.put( documentId, entities );
      }
   }

}
