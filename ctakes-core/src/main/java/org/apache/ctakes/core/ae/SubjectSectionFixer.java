package org.apache.ctakes.core.ae;

import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/11/2017
 */
final public class SubjectSectionFixer extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = LogManager.getLogger( "SubjectSectionFixer" );

   static private final Collection<String> FAMILY_HISTORY = Arrays.asList(
//         "FamilyAndSocialHistory_Section", "FamilyEnviroment_Section", "FamilyMedicalHistory_Section" );
         "Family Medical History" );


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Adjusting Subject based upon Section ..." );

      final Collection<Segment> sections = JCasUtil.select( jCas, Segment.class );
      for ( Segment section : sections ) {
         if ( !FAMILY_HISTORY.contains( section.getPreferredText() ) ) {
            continue;
         }
         final Collection<IdentifiedAnnotation> annotations
               = JCasUtil.selectCovered( jCas, IdentifiedAnnotation.class, section );
         if ( annotations == null ) {
            continue;
         }
         for ( IdentifiedAnnotation annotation : annotations ) {
            annotation.setSubject( CONST.ATTR_SUBJECT_FAMILY_MEMBER );
         }
      }
      LOGGER.info( "Finished Processing" );
   }


}
