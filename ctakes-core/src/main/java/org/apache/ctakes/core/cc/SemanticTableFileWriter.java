package org.apache.ctakes.core.cc;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/14/2018
 */
@PipeBitInfo(
      name = "Semantic Table Writer",
      description = "Writes a table of Annotation information to file, grouped by Semantic Type.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, IDENTIFIED_ANNOTATION },
      usables = { DOCUMENT_ID_PREFIX }
)
public class SemanticTableFileWriter extends AbstractTableFileWriter {

   static private final Logger LOGGER = LoggerFactory.getLogger( "SemanticTableFileWriter" );


   /**
    * {@inheritDoc}
    */
   @Override
   protected List<String> createHeaderRow( final JCas jCas ) {
      return Arrays.asList(
            " Semantic Group ",
            " Semantic Type ",
            " Section ",
            " Span ",
            " Negated ",
            " Uncertain ",
            " Generic ",
            " CUI ",
            " Preferred Text ",
            " Document Text " );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected List<List<String>> createDataRows( final JCas jCas ) {
      final Collection<AnnotationInfo> infos = new ArrayList<>();

      final Map<EventMention, List<Segment>> eventSectionMap
            = JCasUtil.indexCovering( jCas, EventMention.class, Segment.class );
      for ( EventMention annotation : eventSectionMap.keySet() ) {
         final Collection<SemanticTui> tuis = SemanticTui.getTuis( annotation );
         for ( SemanticTui tui : tuis ) {
            infos.add( new AnnotationInfo( tui, eventSectionMap.get( annotation ), annotation ) );
         }
      }
      final Map<AnatomicalSiteMention, List<Segment>> siteSectionMap
            = JCasUtil.indexCovering( jCas, AnatomicalSiteMention.class, Segment.class );
      for ( AnatomicalSiteMention annotation : siteSectionMap.keySet() ) {
         final Collection<SemanticTui> tuis = SemanticTui.getTuis( annotation );
         for ( SemanticTui tui : tuis ) {
            infos.add( new AnnotationInfo( tui, siteSectionMap.get( annotation ), annotation ) );
         }
      }
      return infos.stream()
//                  .sorted( Comparator.comparing( AnnotationInfo::getGroup )
                  .sorted( Comparator.comparingInt( AnnotationInfo::getBegin )
                                     .thenComparingInt( AnnotationInfo::getEnd )
                                     .thenComparing( AnnotationInfo::getGroup )
                                     .thenComparing( AnnotationInfo::getTui )
                                     .thenComparing( AnnotationInfo::getSection )
                                     .thenComparing( AnnotationInfo::isNegated )
                                     .thenComparing( AnnotationInfo::isUncertain )
                                     .thenComparing( AnnotationInfo::isGeneric )
                                     .thenComparing( AnnotationInfo::getCui )
                                     .thenComparing( AnnotationInfo::getPrefText )
                                     .thenComparing( AnnotationInfo::getDocText ) )
                  .map( AnnotationInfo::getColumns )
                  .collect( Collectors.toList() );
   }


   /**
    * Simple container for annotation information.
    */
   static private class AnnotationInfo {

      private final String _group;
      private final int _begin;
      private final int _end;
      private final String _tui;
      private final String _section;
      private final String _cui;
      private final boolean _negated;
      private final boolean _uncertain;
      private final boolean _generic;
      private final String _prefText;
      private final String _docText;

      private AnnotationInfo( final SemanticTui tui, final Collection<Segment> section,
                              final IdentifiedAnnotation annotation ) {
         _group = tui.getGroup()
                     .getName();
         _begin = annotation.getBegin();
         _end = annotation.getEnd();
         _tui = tui.getSemanticType();
         final String sectionText = ( section == null || section.isEmpty() )
                                    ? "NULL"
                                    : new ArrayList<>( section ).get( 0 )
                                                                .getPreferredText();
         _section = sectionText == null
                    ? "NULL"
                    : sectionText;
         _cui = IdentifiedAnnotationUtil.getCuis( annotation )
                                        .stream()
                                        .sorted()
                                        .collect( Collectors.joining( ";" ) );
         _negated = IdentifiedAnnotationUtil.isNegated( annotation );
         _uncertain = IdentifiedAnnotationUtil.isUncertain( annotation );
         _generic = IdentifiedAnnotationUtil.isGeneric( annotation );
         _prefText = IdentifiedAnnotationUtil.getPreferredTexts( annotation )
                                             .stream()
                                             .sorted()
                                             .collect( Collectors.joining( ";" ) );
         _docText = annotation.getCoveredText();
      }

      public List<String> getColumns() {
         return Arrays.asList(
               getGroup(),
               getTui(),
               getSection(),
               getBegin() + "," + getEnd(),
               isNegated() + "",
               isUncertain() + "",
               isGeneric() + "",
               getCui(),
               getPrefText(),
               getDocText() );
      }

      public String getGroup() {
         return _group;
      }

      public String getTui() {
         return _tui;
      }

      public String getSection() {
         return _section;
      }

      public int getBegin() {
         return _begin;
      }

      public int getEnd() {
         return _end;
      }

      public String getCui() {
         return _cui;
      }

      public boolean isNegated() {
         return _negated;
      }

      public boolean isUncertain() {
         return _uncertain;
      }

      public boolean isGeneric() {
         return _generic;
      }

      public String getPrefText() {
         return _prefText;
      }

      public String getDocText() {
         return _docText;
      }

   }

}

