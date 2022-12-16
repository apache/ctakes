package org.apache.ctakes.core.patient;


import org.apache.ctakes.core.util.NumberedSuffixComparator;
import org.apache.log4j.Logger;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility class to get view information from a JCas.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/27/2017
 */
final public class PatientViewUtil {

   static public final String DEFAULT_VIEW = "_InitialView";
   static public final String GOLD_PREFIX = "GoldView";
   static private final Predicate<String> isNameGold = s -> s.startsWith( GOLD_PREFIX );
   static private final Predicate<JCas> isCasGold = c -> c.getViewName().startsWith( GOLD_PREFIX );

   static private final Logger LOGGER = Logger.getLogger( "PatientViewUtil" );

   private PatientViewUtil() {
   }

   /**
    * @param jCas ye olde ...
    * @return Names of all views, including gold and default
    */
   static public Collection<String> getAllViewNames( final JCas jCas ) {
      return getAllViews( jCas ).stream().map( JCas::getViewName ).collect( Collectors.toList() );
   }

   /**
    * @return {@link #DEFAULT_VIEW}
    */
   static public String getDefaultViewName() {
      return DEFAULT_VIEW;
   }

   /**
    * @param jCas ye olde ...
    * @return Names of all gold views, which are views with the prefix {@link #GOLD_PREFIX}
    */
   static public Collection<String> getGoldViewNames( final JCas jCas ) {
      return getAllViewNames( jCas ).stream().filter( isNameGold ).collect( Collectors.toList() );
   }

   /**
    * @param jCas ye olde ...
    * @return Names of all document views, which are views that are not the default and not gold
    */
   static public Collection<String> getDocumentViewNames( final JCas jCas ) {
      return getDocumentViews( jCas ).stream().map( JCas::getViewName ).collect( Collectors.toList() );
   }

   /**
    * @param jCas ye olde ...
    * @return All views, including gold and default
    */
   static public Collection<JCas> getAllViews( final JCas jCas ) {
      if ( jCas == null ) {
         return Collections.emptyList();
      }
      final Collection<JCas> views = new ArrayList<>();
      try {
         jCas.getViewIterator().forEachRemaining( views::add );
      } catch ( CASException casE ) {
         LOGGER.error( casE.getMessage() );
      }
      return views;
   }

   /**
    * @return The view with name {@link #DEFAULT_VIEW}
    */
   static public JCas getDefaultView( final JCas jCas ) {
      try {
         return jCas.getView( DEFAULT_VIEW );
      } catch ( CASException casE ) {
         LOGGER.error( casE.getMessage() );
      }
      return jCas;
   }

   /**
    * @param jCas ye olde ...
    * @return All gold views, which are views with the prefix {@link #GOLD_PREFIX}
    */
   static public Collection<JCas> getGoldViews( final JCas jCas ) {
      return getAllViews( jCas ).stream()
            .filter( isCasGold )
            .sorted( new ViewComparator() )
            .collect( Collectors.toList() );
   }

   /**
    * @param jCas ye olde ...
    * @return All document views, which are views that are not the default and not gold
    */
   static public Collection<JCas> getDocumentViews( final JCas jCas ) {
      final Collection<JCas> unwantedViews = new ArrayList<>( getGoldViews( jCas ) );
      unwantedViews.add( getDefaultView( jCas ) );
      return getAllViews( jCas ).stream()
            .filter( c -> !unwantedViews.contains( c ) )
            .sorted( new ViewComparator()
            ).collect( Collectors.toList() );
   }

   /**
    * Use to sort views by logical number suffix.  {@link NumberedSuffixComparator}
    */
   static final class ViewComparator implements Comparator<JCas> {
      private final Comparator<String> __delegate = new NumberedSuffixComparator();

      public int compare( final JCas jCas1, final JCas jCas2 ) {
         return __delegate.compare( jCas1.getViewName(), jCas2.getViewName() );
      }
   }

   public static JCas getAlignedGoldCas(final JCas patientJCas, final JCas docView) throws CASException {
      String pid = PatientNoteStore.getDefaultPatientId( patientJCas );
      String docId = PatientNoteStore.getDefaultDocumentId( docView ).replace(CAS.NAME_DEFAULT_SOFA, "");
      String goldViewName = PatientNoteStore.getInternalViewname( pid, docId, GOLD_PREFIX );
      return patientJCas.getView(goldViewName);
   }

   public static JCas getAlignedDocCas(final JCas patientJCas, final JCas goldView) throws CASException {
      String pid = PatientNoteStore.getDefaultPatientId( patientJCas );
      String docId = PatientNoteStore.getDefaultDocumentId( goldView ).replace("GoldView_", "");
      String docViewName = PatientNoteStore.getInternalViewname( pid, docId, CAS.NAME_DEFAULT_SOFA );
      return patientJCas.getView(docViewName);
   }
}
