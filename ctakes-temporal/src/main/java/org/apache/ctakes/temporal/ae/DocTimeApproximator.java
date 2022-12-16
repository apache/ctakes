package org.apache.ctakes.temporal.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.temporal.utils.CalendarUtil;
import org.apache.ctakes.typesystem.type.structured.SourceData;
import org.apache.ctakes.typesystem.type.textsem.DateAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Calendar.DAY_OF_MONTH;
import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION;
import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.TIMEX;
import static org.apache.ctakes.temporal.utils.CalendarUtil.NULL_CALENDAR;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2018
 */
@PipeBitInfo(
      name = "DocTimeApproximator",
      description = "Sets the document time based upon the latest normalized date earlier than now.",
      role = PipeBitInfo.Role.ANNOTATOR,
      usables = { TIMEX, IDENTIFIED_ANNOTATION }
)
final public class DocTimeApproximator extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "DocTimeApproximator" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      final SourceData sourceData = SourceMetadataUtil.getOrCreateSourceData( jCas );
      final String docTime = sourceData.getSourceOriginalDate();
      if ( docTime != null && !docTime.isEmpty() ) {
         LOGGER.info( "Document Time is " + docTime );
         return;
      }

      final Collection<Calendar> calendars = new HashSet<>();

      JCasUtil.select( jCas, TimeMention.class ).stream()
              .map( CalendarUtil::getCalendar )
              .forEach( calendars::add );

      JCasUtil.select( jCas, DateAnnotation.class ).stream()
              .map( CalendarUtil::getCalendar )
              .forEach( calendars::add );

      final Calendar lastCalendar = getLastCalendar( calendars );
      if ( NULL_CALENDAR.equals( lastCalendar ) ) {
         LOGGER.info( "Could not parse Document Time." );
         return;
      }

      setDocTime( sourceData, lastCalendar );
   }

   /**
    * @param calendars calendars in the document.
    * @return the calendar with the latest date preceding "yesterday" or {@link CalendarUtil#NULL_CALENDAR}
    */
   static private Calendar getLastCalendar( final Collection<Calendar> calendars ) {
      if ( calendars.isEmpty() ) {
         return NULL_CALENDAR;
      }
      final Calendar nineteen = new Calendar.Builder().setDate( 1900, 0, 1 ).build();
      final Calendar now = Calendar.getInstance();
      now.add( DAY_OF_MONTH, -1 );
      final List<Calendar> calendarList = calendars.stream()
                                                   .filter( c -> !NULL_CALENDAR.equals( c ) )
                                                   .filter( c -> c.compareTo( nineteen ) > 0 )
                                                   .filter( c -> c.compareTo( now ) < 0 )
                                                   .distinct()
                                                   .sorted()
                                                   .collect( Collectors.toList() );
      if ( calendarList.isEmpty() ) {
         return NULL_CALENDAR;
      }
      return calendarList.get( calendarList.size() - 1 );
   }

   /**
    * Set the document time (source original date) to the calendar value.
    *
    * @param sourceData -
    * @param calendar   -
    */
   static private void setDocTime( final SourceData sourceData, final Calendar calendar ) {
      final String docTime = CalendarUtil.createDigitDateText( calendar );
      sourceData.setSourceOriginalDate( docTime );
      LOGGER.info( "Parsed Document Time is " + docTime );
   }

}
