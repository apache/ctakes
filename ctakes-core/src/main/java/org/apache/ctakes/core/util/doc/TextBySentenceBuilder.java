package org.apache.ctakes.core.util.doc;


import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a new cas or populates an existing jcas with sections, their names and sentence text.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/22/2019
 */
final public class TextBySentenceBuilder {

   static private final String DEFAULT_SEGMENT_ID = "SIMPLE_SEGMENT";

   private final List<SentenceSection> _sentenceSections = new ArrayList<>();
   private SentenceSection _currentSection;

   /**
    * @param name name of the section.  This will be placed in the text and used to create an id: name_#
    * @return this builder.
    */
   public TextBySentenceBuilder startSection( final String name ) {
      if ( _currentSection != null ) {
         _sentenceSections.add( _currentSection );
      }
      _currentSection = new SentenceSection( name );
      return this;
   }

   /**
    * Places the given sentence text in the current section.
    *
    * @param text text content of the sentence.
    * @return this builder.
    */
   public TextBySentenceBuilder addSentence( final String text ) {
      if ( _currentSection == null ) {
         _currentSection = new SentenceSection( DEFAULT_SEGMENT_ID );
      }
      _currentSection.addSentence( text );
      return this;
   }

   /**
    * @return a jcas created from scratch and populated with the sections and sentences added in this builder.
    * @throws UIMAException is the fresh jcas cannot be created.
    */
   public JCas build() throws UIMAException {
      return populate( JCasFactory.createJCas() );
   }

   /**
    * @param jCas ye olde ...
    * @return the given jcas populated with the sections and sentences added in this builder.
    */
   public JCas populate( final JCas jCas ) {
      _sentenceSections.add( _currentSection );
      final StringBuilder sb = new StringBuilder();
      int sectionNum = 1;
      int sentenceNum = 1;
      for ( SentenceSection sentenceSection : _sentenceSections ) {
         final String name = sentenceSection._name;
         final Segment section = new Segment( jCas );
         section.setTagText( name );
         section.setPreferredText( name );
         final String sectionId = name + '_' + sectionNum;
         section.setId( sectionId );
         section.setBegin( sb.length() );
         if ( !name.equals( DEFAULT_SEGMENT_ID ) ) {
            sb.append( name ).append( "\n" );
         }
         for ( String sentenceText : sentenceSection._sentences ) {
            final Sentence sentence = new Sentence( jCas );
            sentence.setSegmentId( sectionId );
            sentence.setSentenceNumber( sentenceNum );
            sentence.setBegin( sb.length() );
            sb.append( sentenceText ).append( "\n" );
            sentence.setEnd( sb.length() );
            sentence.addToIndexes( jCas );
            sentenceNum++;
         }
         if ( _sentenceSections.size() > 1 ) {
            sb.append( "\n\n" );
         }
         section.setEnd( sb.length() );
         section.addToIndexes( jCas );
         sectionNum++;
      }
      jCas.setDocumentText( sb.toString() );
      return jCas;
   }

   /**
    * internal storage device.
    */
   static private final class SentenceSection {
      private final String _name;
      private final List<String> _sentences = new ArrayList<>();

      private SentenceSection( final String name ) {
         _name = name;
      }

      private void addSentence( final String text ) {
         _sentences.add( text );
      }
   }

}
