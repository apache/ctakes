package org.apache.ctakes.core.util.doc;


import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a new cas or populates an existing jcas with sections, their names and text.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/22/2019
 */
final public class TextBySectionBuilder {

   private final List<String> _sectionNames = new ArrayList<>();
   private final List<String> _sectionTexts = new ArrayList<>();


   /**
    * @param name name of the section.  This will be placed in the text and used to create an id: name_#
    * @param text text content of the section.
    * @return this builder.
    */
   public TextBySectionBuilder addSection( final String name, final String text ) {
      _sectionNames.add( name );
      _sectionTexts.add( text );
      return this;
   }

   /**
    * @return a jcas created from scratch and populated with the sections added in this builder.
    * @throws UIMAException is the fresh jcas cannot be created.
    */
   public JCas build() throws UIMAException {
      return populate( JCasFactory.createJCas() );
   }

   /**
    * @param jCas ye olde ...
    * @return the given jcas populated with the sections added in this builder.
    */
   public JCas populate( final JCas jCas ) {
      final StringBuilder sb = new StringBuilder();
      final int sectionCount = _sectionNames.size();
      int sectionNum = 1;
      for ( int i = 0; i < sectionCount; i++ ) {
         final String name = _sectionNames.get( i );
         final Segment section = new Segment( jCas );
         section.setTagText( name );
         section.setPreferredText( name );
         section.setId( name + '_' + sectionNum );
         sb.append( name )
           .append( "\n" );
         section.setBegin( sb.length() );
         sb.append( _sectionTexts.get( i ) )
           .append( "\n\n" );
         section.setEnd( sb.length() );
         section.addToIndexes();
         sectionNum++;
      }
      jCas.setDocumentText( sb.toString() );
      return jCas;
   }

}
