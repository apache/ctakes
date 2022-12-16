package org.apache.ctakes.temporal.utils;


import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.Options;
import com.mdimension.jchronic.tags.Pointer;
import com.mdimension.jchronic.utils.Span;
import org.apache.ctakes.core.util.StringUtil;
import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.textsem.DateAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Calendar.*;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/8/2019
 * @deprecated use CalendarUtil in core.util
 */
final public class CalendarUtil {

   private CalendarUtil() {
   }

   static public final Calendar NULL_CALENDAR = new Calendar.Builder().setDate( 1, 1, 1 ).build();
   static private final Options PAST_OPTIONS = new Options( Pointer.PointerType.PAST );

   static private final Pattern SLASH_DATE = Pattern.compile( "[0-9]{0,2}/[0-9]{0,2}/[0-9]{2,4}" );
   static private final Pattern DASH_DATE = Pattern.compile( "[0-9]{1,4}-[0-9]{1,2}-[0-9]{1,4}" );
   static private final Pattern HALF_SLASH_DATE = Pattern.compile( "(?:1?[0-9]/)?[1-2][0,9][0-9]{2}" );


   static public Calendar createTimexCalendar( final Annotation annotation ) {
      if ( annotation instanceof TimeMention ) {
         final Date typeDate = ((TimeMention)annotation).getDate();
         final Calendar typeCalendar = CalendarUtil.getCalendar( typeDate );
         if ( !NULL_CALENDAR.equals( typeCalendar ) ) {
            return typeCalendar;
         }
      }
      final Calendar calendar = getTextCalendar( annotation.getCoveredText() );
      if ( !NULL_CALENDAR.equals( calendar ) ) {
         return calendar;
      }
      return getCalendar( annotation.getCoveredText() );
   }

   /**
    * @param timeMention -
    * @return Calendar created using preset date information in the TimeMention or its covered text, or {@link #NULL_CALENDAR}.
    */
   static public Calendar getCalendar( final TimeMention timeMention ) {
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
   static public Calendar getCalendar( final Date typeDate ) {
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
      return new Calendar.Builder().setFields( array ).build();
   }

   /**
    * @param dateAnnotation -
    * @return Calendar parsed from text, or {@link #NULL_CALENDAR}.
    */
   static public Calendar getCalendar( final DateAnnotation dateAnnotation ) {
      return getCalendar( dateAnnotation.getCoveredText() );
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
    * @param calendar -
    * @return ugly format date consisting only of digits with twelve o'clock : YYYYMMDD1200
    */
   static public String createDigitDateText( final Calendar calendar ) {
      return String.format( "%04d%02d%02d1200",
            calendar.get( YEAR ),
            calendar.get( MONTH ) + 1,
            calendar.get( DAY_OF_MONTH ) );
   }

   /**
    * @param text -
    * @return Calendar parsed from text, or {@link #NULL_CALENDAR}.
    */
   static public Calendar getCalendar( final String text ) {
      try {
         final Span span = Chronic.parse( text, PAST_OPTIONS );
         if ( span == null ) {
            return NULL_CALENDAR;
         }
         return span.getEndCalendar();
      } catch ( RuntimeException rtE ) {
         Logger.getLogger( "CalendarUtil" ).error( rtE.getMessage() );
         return NULL_CALENDAR;
      }
   }

   static public Calendar getTextCalendar( final String text ) {
      if ( text.length() < 4 || text.length() > 10 ) {
         return NULL_CALENDAR;
      }
      if ( SLASH_DATE.matcher( text ).matches() ) {
         return CalendarUtil.getSlashCalendar( text );
      } else if ( HALF_SLASH_DATE.matcher( text ).matches() ) {
         return CalendarUtil.getHalfSlashCalendar( text );
      } else if ( DASH_DATE.matcher( text ).matches() ) {
         return CalendarUtil.getDashCalendar( text );
      }
      return NULL_CALENDAR;
   }

   /**
    * @param text something with 0-2 month digits, 0-2 day digits and 2-4 year digits all divided by slash
    * @return Calendar parsed from text, or {@link #NULL_CALENDAR}.
    */
   static public Calendar getSlashCalendar( final String text ) {
      final String[] splits = StringUtil.fastSplit( text, '/' );
      int month = 1;
      final int monthI = parseInt( splits[ 0 ] );
      if ( monthI > 0 ) {
         month = monthI;
      }
      int day = 1;
      final int dayI = parseInt( splits[ 1 ] );
      if ( dayI > 0 ) {
         day = dayI;
      }
      final int year = parseInt( splits[ 2 ] );
      return createCalendar( month, day, year );
   }

   /**
    * @param text something with 4 year digits, possibly preceded by 1-2 month digits and a slash
    * @return Calendar parsed from text, or {@link #NULL_CALENDAR}.
    */
   static public Calendar getHalfSlashCalendar( final String text ) {
      final String[] splits = StringUtil.fastSplit( text, '/' );
      if ( splits.length == 1 ) {
         final int year = parseInt( splits[ 0 ] );
         return createCalendar( 1, 1, year );
      }
      int month = 1;
      final int monthI = parseInt( splits[ 0 ] );
      if ( monthI > 0 ) {
         month = monthI;
      }
      final int year = parseInt( splits[ 1 ] );
      return createCalendar( month, 1, year );
   }

   /**
    * @param text something with 1-2 month digits, 1-2 day digits and 4 year digits all divided by dash
    * @return Calendar parsed from text, or {@link #NULL_CALENDAR}.
    */
   static public Calendar getDashCalendar( final String text ) {
      final String[] splits = StringUtil.fastSplit( text, '-' );
      final int monthOrYear = parseInt( splits[ 0 ] );
      int dayOrMonth = parseInt( splits[ 1 ] );
      final int yearOrDay = parseInt( splits[ 2 ] );
      if ( splits[ 0 ].length() == 4 ) {
         // year and month are reversed
         return createCalendar( dayOrMonth, yearOrDay, monthOrYear );
      }
      return createCalendar( monthOrYear, dayOrMonth, yearOrDay );
   }

   static private Calendar createCalendar( final int month, final int day, final int year ) {
      int y2kYear = year;
      if ( year < 100 ) {
         // silly kludge for 2 digit years
         if ( year < 30 ) {
            y2kYear += 2000;
         } else {
            y2kYear += 1900;
         }
      }
      return new GregorianCalendar( y2kYear, month - 1, day );
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

