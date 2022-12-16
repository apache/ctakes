package org.apache.ctakes.core.util;


import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.refsem.Time;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Calendar.*;
import static org.apache.ctakes.typesystem.type.constants.CONST.TIME_CLASS_DATE;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/8/2019
 */
final public class CalendarUtil {

   private CalendarUtil() {
   }

   static public final Calendar NULL_CALENDAR = new Builder().setDate( 1, 1, 1 ).build();

   static private final Collection<String> DATE_FORMAT_CODES = Arrays.asList(
         "M-d-yy",
         "MM-dd-yy",
         "MM-dd-yyyy",
         "M/d/yy",
         "MM/dd/yy",
         "MM/dd/yyyy",
         "MMM d",
         "MMM yyyy",
         "MMM d yyyy",
         "dd-MMM-yy",
         "dd-MMM-yyyy"
   );

   static private final Collection<String> TIME_FORMAT_CODES = Arrays.asList(
         "h:mm",
         "h:mm a",
         "h:mm:ss",
         "h:mm:ss.SSS",
         "h:mm z",
         "h:mm a z",
         "h:mm a, z",
         "h a",
         "h 'o''clock'",
         "h 'o''clock' a"
   );


   static private final Collection<DateFormat> DATE_FORMATS = new ArrayList<>();
   static private final Collection<DateFormat> DATE_TIME_FORMATS = new ArrayList<>();

   static {
      for ( String date : DATE_FORMAT_CODES ) {
         DATE_FORMATS.add( new SimpleDateFormat( date ) );
         DATE_FORMATS.add( new SimpleDateFormat( "EEE " + date ) );
         DATE_FORMATS.add( new SimpleDateFormat( "EEE, " + date ) );
         for ( String time : TIME_FORMAT_CODES ) {
            DATE_TIME_FORMATS.add( new SimpleDateFormat( date + " " + time ) );
            DATE_TIME_FORMATS.add( new SimpleDateFormat( date + " 'at' " + time ) );
            DATE_TIME_FORMATS.add( new SimpleDateFormat( "EEE " + date + " " + time ) );
            DATE_TIME_FORMATS.add( new SimpleDateFormat( "EEE, " + date + " 'at' " + time ) );

            DATE_TIME_FORMATS.add( new SimpleDateFormat( time + " " + date ) );
            DATE_TIME_FORMATS.add( new SimpleDateFormat( time + " 'on' " + date ) );
            DATE_TIME_FORMATS.add( new SimpleDateFormat( time + ", EEE " + date ) );
            DATE_TIME_FORMATS.add( new SimpleDateFormat( time + " 'on' EEE " + date ) );
            DATE_TIME_FORMATS.add( new SimpleDateFormat( time + " 'on' EEE, " + date ) );
         }
      }
   }


   /**
    * @param jCas  ye olde ...
    * @param begin begin index in doc text
    * @param end   end index in doc text
    * @return TimeMention with normalized date and time and character offsets
    */
   static public TimeMention createTimeMention( final JCas jCas,
                                                final int begin,
                                                final int end ) {
      final String docText = jCas.getDocumentText();
      if ( begin < 0 || end > docText.length() || begin >= end ) {
         throw new InvalidParameterException( "Offsets " + begin + "," + end
                                              + " are outside document bounds 0," + docText.length() );
      }
      final String text = docText.substring( begin, end );
      final TimeMention timeMention = createTimeMention( jCas, text );
      timeMention.setBegin( begin );
      timeMention.setEnd( end );
      return timeMention;
   }

   /**
    * @param jCas     ye olde ...
    * @param begin    begin index in doc text
    * @param end      end index in doc text
    * @param calendar some calendar with actual date information
    * @return TimeMention with normalized date and time and character offsets
    */
   static public TimeMention createTimeMention( final JCas jCas,
                                                final int begin,
                                                final int end,
                                                final Calendar calendar ) {
      final String docText = jCas.getDocumentText();
      if ( begin < 0 || end > docText.length() || begin >= end ) {
         throw new InvalidParameterException( "Offsets " + begin + "," + end
                                              + " are outside document bounds 0," + docText.length() );
      }
      final TimeMention timeMention = createTimeMention( jCas, calendar );
      timeMention.setBegin( begin );
      timeMention.setEnd( end );
      return timeMention;
   }

   /**
    * @param jCas ye olde ...
    * @param text doc text
    * @return TimeMention with normalized date and time and no character offsets
    */
   static public TimeMention createTimeMention( final JCas jCas,
                                                final String text ) {
      return createTimeMention( jCas, getCalendar( text ) );
   }

   /**
    * @param jCas     ye olde ...
    * @param calendar some calendar with actual date information
    * @return TimeMention with normalized date and time and no character offsets
    */
   static public TimeMention createTimeMention( final JCas jCas,
                                                final Calendar calendar ) {
      final Date date = createTypeDate( jCas, calendar );
      final Time time = createTypeTime( jCas, calendar );
      final TimeMention timeMention = new TimeMention( jCas );
      timeMention.setDate( date );
      timeMention.setTime( time );
      // Right now there is only one time class.
      timeMention.setTimeClass( TIME_CLASS_DATE );
      return timeMention;
   }

   /**
    * @param jCas ye olde ...
    * @param text some text representing actual date information
    * @return Type System Date with filled day, month, year values
    */
   static public Date createTypeDate( final JCas jCas, final String text ) {
      final Calendar calendar = getCalendar( text );
      return createTypeDate( jCas, calendar );
   }

   /**
    * @param jCas     ye olde ...
    * @param calendar some calendar with actual date information
    * @return Type System Date with filled day, month, year values
    */
   static public Date createTypeDate( final JCas jCas, final Calendar calendar ) {
      final Date date = new Date( jCas );
      date.setDay( "" + calendar.get( DAY_OF_MONTH ) );
      date.setMonth( "" + (calendar.get( Calendar.MONTH ) + 1) );
      date.setYear( "" + calendar.get( Calendar.YEAR ) );
      return date;
   }

   /**
    * @param jCas ye olde ...
    * @param text some text representing actual time information
    * @return Type System Time with filled day, month, year values
    */
   static public Time createTypeTime( final JCas jCas, final String text ) {
      final Calendar calendar = getCalendar( text );
      return createTypeTime( jCas, calendar );
   }

   /**
    * @param jCas     ye olde ...
    * @param calendar some calendar with actual time information
    * @return Type System Time with filled day, month, year values
    */
   static public Time createTypeTime( final JCas jCas, final Calendar calendar ) {
      final Time time = new Time( jCas );
      time.setNormalizedForm( calendar.get( HOUR_OF_DAY )
                              + ":" + calendar.get( MINUTE )
                              + ":" + calendar.get( SECOND )
                              + " " + calendar.get( AM_PM ) );
      return time;
   }

   static public Calendar getCalendar( final Annotation annotation ) {
      if ( annotation instanceof TimeMention ) {
         return getTimeMentionCalendar( (TimeMention)annotation );
      }
      return getCalendar( annotation.getCoveredText() );
   }

   /**
    * @param text -
    * @return Calendar parsed from text, or {@link #NULL_CALENDAR}.
    */
   static public Calendar getCalendar( final String text ) {
      final Calendar calendar = Calendar.getInstance();
      for ( DateFormat format : DATE_FORMATS ) {
         try {
            java.util.Date date = format.parse( text );
            calendar.setTime( date );
            return calendar;
         } catch ( ParseException pE ) {
            // Continue
         }
      }
      for ( DateFormat format : DATE_TIME_FORMATS ) {
         try {
            java.util.Date date = format.parse( text );
            calendar.setTime( date );
            return calendar;
         } catch ( ParseException pE ) {
            // Continue
         }
      }
      return NULL_CALENDAR;
   }

   /**
    * @param timeMention -
    * @return Calendar created using preset date information in the TimeMention or its covered text, or {@link #NULL_CALENDAR}.
    */
   static private Calendar getTimeMentionCalendar( final TimeMention timeMention ) {
      if ( timeMention == null ) {
         return NULL_CALENDAR;
      }
      final Date typeDate = timeMention.getDate();
      final Calendar typeCalendar = getCalendar( typeDate );
      if ( !NULL_CALENDAR.equals( typeCalendar ) ) {
         return typeCalendar;
      }
      return CalendarUtil.getCalendar( timeMention.getCoveredText() );
   }

   /**
    * @param typeDate Type System Date, usually in a {@link TimeMention}.
    * @return Calendar created using preset date information, or {@link #NULL_CALENDAR}.
    */
   static private Calendar getCalendar( final Date typeDate ) {
      if ( typeDate == null ) {
         return NULL_CALENDAR;
      }
      final int year = CalendarUtil.parseInt( typeDate.getYear() );
      final int month = CalendarUtil.parseInt( typeDate.getMonth() );
      final int day = CalendarUtil.parseInt( typeDate.getDay() );
      if ( year == Integer.MIN_VALUE && month == Integer.MIN_VALUE && day == Integer.MIN_VALUE ) {
         return NULL_CALENDAR;
      }
      final List<Integer> fields = new ArrayList<>( 6 );
      if ( year != Integer.MIN_VALUE ) {
         fields.add( Calendar.YEAR );
         fields.add( year );
      }
      if ( month != Integer.MIN_VALUE ) {
         fields.add( Calendar.MONTH );
         fields.add( month - 1 );
      }
      if ( day != Integer.MIN_VALUE ) {
         fields.add( Calendar.DAY_OF_MONTH );
         fields.add( day );
      }
      final int[] array = new int[ fields.size() ];
      for ( int i = 0; i < array.length; i++ ) {
         array[ i ] = fields.get( i );
      }
      return new Builder().setFields( array ).build();
   }


   /**
    * @param text -
    * @return positive int value of text or {@link Integer#MIN_VALUE} if not possible.
    */
   static private int parseInt( final String text ) {
      if ( text == null || text.isEmpty() ) {
         return Integer.MIN_VALUE;
      }
      for ( char c : text.toCharArray() ) {
         if ( !Character.isDigit( c ) ) {
            return Integer.MIN_VALUE;
         }
      }
      try {
         return Integer.parseInt( text );
      } catch ( NumberFormatException nfE ) {
         return Integer.MIN_VALUE;
      }
   }


}

