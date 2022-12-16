package org.apache.ctakes.core.pipeline;


import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.log4j.Logger;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stores a collection of Cuis from a run, along with their associated Document Ids
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2016
 */
public enum CuiCollector {
   INSTANCE;

   static public CuiCollector getInstance() {
      return INSTANCE;
   }

   static private final Logger LOGGER = Logger.getLogger( "CuiCollector" );

   private final Map<String, Map<String, Long>> _cuiCountMap = new HashMap<>();

   /**
    * @return Ids for documents that have Cuis stored in the CuiCollector
    */
   public Collection<String> getDocumentIds() {
      return Collections.unmodifiableCollection( _cuiCountMap.keySet() );
   }

   /**
    * @param documentId id for some document
    * @return cuis discovered in the document
    */
   public Collection<String> getCuis( final String documentId ) {
      return Collections.unmodifiableCollection( get( documentId ).keySet() );
   }

   /**
    * @param documentId id for some document
    * @return map of cuis discovered in the document and how many times they were discovered
    */
   public Map<String, Long> getCuiCounts( final String documentId ) {
      return get( documentId );
   }

   /**
    * @return all cuis found in all documents in the run
    */
   public Collection<String> getCuis() {
      return Collections.unmodifiableCollection( _cuiCountMap.values().stream()
            .map( Map::keySet )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() ) );
   }

   /**
    * @return map of cuis discovered in all documents in the run and how many times they were discovered
    */
   public Map<String, Long> getCuiCounts() {
      return Collections.unmodifiableMap(
            _cuiCountMap.values().stream()
                  .map( Map::entrySet )
                  .flatMap( Collection::stream )
                  .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue, ( n1, n2 ) -> n1 + n2 ) ) );
   }

   /**
    * @param documentId id for some document
    * @return map of cuis discovered in the document and how many times they were discovered, with a warning if none exist
    */
   private Map<String, Long> get( final String documentId ) {
      final Map<String, Long> cuiCounts = _cuiCountMap.get( documentId );
      if ( cuiCounts == null ) {
         LOGGER.warn( "No Cuis for " + documentId );
         return Collections.emptyMap();
      }
      return Collections.unmodifiableMap( cuiCounts );
   }

   /**
    * @return table of document ids and cui counts
    */
   @Override
   public String toString() {
      final List<String> allCuis = getCuis().stream().sorted().collect( Collectors.toList() );
      final String header = "DOCUMENT_ID|" + String.join( "|", allCuis );
      final String rows = _cuiCountMap.entrySet().stream()
            .sorted()
            .map( e -> createRowText( e.getKey(), e.getValue(), allCuis ) )
            .collect( Collectors.joining() );
      return header + "\n" + rows;
   }

   /**
    * @param documentId id for some document
    * @param cuiCounts  map of cuis discovered in the document and how many times they were discovered
    * @param allCuis    all cuis found in all documents in the run
    * @return row of document id and cui counts
    */
   static private String createRowText( final String documentId, final Map<String, Long> cuiCounts,
                                        final List<String> allCuis ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( documentId );
      for ( String cui : allCuis ) {
         sb.append( "|" );
         final Long count = cuiCounts.get( cui );
         sb.append( count == null ? "0" : count );
      }
      sb.append( "\n" );
      return sb.toString();
   }

   /**
    * Analysis Engine that stores collections of cuis by document id in the CuiCollector
    */
   @PipeBitInfo(
         name = "CUI Collector",
         description = "Collects all CUIs generated during a run.",
         role = PipeBitInfo.Role.SPECIAL,
         dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
   )
   static public final class CuiCollectorEngine extends JCasAnnotator_ImplBase {
      @Override
      public void process( final JCas jCas ) {
         LOGGER.info( "Starting processing" );
         final String id = DocIdUtil.getDeepDocumentId( jCas );
         final Map<String, Long> cuiCounts = OntologyConceptUtil.getCuiCounts( jCas );
         CuiCollector.getInstance()._cuiCountMap.put( id, cuiCounts );
         LOGGER.info( "Finished processing" );
      }
   }

}
