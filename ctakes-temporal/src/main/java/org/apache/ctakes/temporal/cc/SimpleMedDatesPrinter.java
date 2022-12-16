package org.apache.ctakes.temporal.cc;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.refsem.Date;
import org.apache.ctakes.typesystem.type.textsem.MedicationEventMention;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/10/2019
 */
@PipeBitInfo(
      name = "SimpleMedDatesPrinter",
      description = "Finds start and stop dates for medication events.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION
)
final public class SimpleMedDatesPrinter extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "SimpleMedDatesPrinter" );

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Printing Medication Dates ..." );

      final Collection<MedicationEventMention> medEvents = JCasUtil.select( jCas, MedicationEventMention.class );
      for ( MedicationEventMention med : medEvents ) {
         printDate( med.getCoveredText(), "Start", med.getStartDate() );
         printDate( med.getCoveredText(), " Stop", med.getEndDate() );
      }

      final Collection<MedicationMention> meds = JCasUtil.select( jCas, MedicationMention.class );
      for ( MedicationMention med : meds ) {
         printTimeMention( med.getCoveredText(), "Start", med.getStartDate() );
         printTimeMention( med.getCoveredText(), " Stop", med.getEndDate() );
      }

   }

   static private void printTimeMention( final String med, final String dateType, final TimeMention timeMention ) {
      if ( timeMention == null ) {
         printDate( med, dateType, null );
      } else {
         printDate( med, dateType, timeMention.getDate() );
      }
   }

   static private void printDate( final String med, final String dateType, final Date date ) {
      if ( date == null ) {
         LOGGER.info( med + " " + dateType + " = NO DATE" );
         return;
      }
      final StringBuilder sb = new StringBuilder();
      final String month = date.getMonth();
      if ( month != null && !month.isEmpty() ) {
         sb.append( " Month: " ).append( month );
      }
      final String day = date.getDay();
      if ( day != null && !day.isEmpty() ) {
         sb.append( " Day: " ).append( day );
      }
      final String year = date.getYear();
      if ( year != null && !year.isEmpty() ) {
         sb.append( " Year: " ).append( year );
      }
      LOGGER.info( med + " " + dateType + " = " + sb.toString() );
   }


}
