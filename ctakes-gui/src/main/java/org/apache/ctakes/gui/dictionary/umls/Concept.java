package org.apache.ctakes.gui.dictionary.umls;


import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.core.util.collection.HashSetMap;
import org.apache.ctakes.gui.dictionary.util.TextTokenizer;

import java.util.*;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 11/20/13
 */
final public class Concept {

   static public final String PREFERRED_TERM_UNKNOWN = "Unknown Preferred Term";

   private String _preferredText = null;
   private boolean _hasDose = false;

   final private Map<String, Integer> _textCounts;
   final private HashSetMap<String, String> _codes;
   final private Collection<SemanticTui> _tuis;

   public Concept() {
      _textCounts = new HashMap<>( 1 );
      _codes = new HashSetMap<>( 0 );
      _tuis = EnumSet.noneOf( SemanticTui.class );
   }

   public int addTexts( final Collection<String> texts ) {
      int oldSize = _textCounts.size();
      texts.forEach( t -> _textCounts.merge( t, 1, ( i1, i2 ) -> i1 + i2 ) );
      return _textCounts.size() - oldSize;
   }

   public int getCount( final String text ) {
      return _textCounts.getOrDefault( text, 0 );
   }

   public int getSynonymCount() {
      return _textCounts.size();
   }

   public void removeTexts( final Collection<String> texts ) {
      _textCounts.keySet().removeAll( texts );
   }

   public void removeText( final String text ) {
      _textCounts.keySet().remove( text );
   }

   public Collection<String> getTexts() {
      return _textCounts.keySet();
   }

   public void cullExtensions() {
      if ( _preferredText != null && !_preferredText.isEmpty() && !_preferredText.equals( PREFERRED_TERM_UNKNOWN ) ) {
         final String tokenizedPrefText = TextTokenizer.getTokenizedText( _preferredText.toLowerCase() );
         if ( !_textCounts.containsKey( tokenizedPrefText ) && tokenizedPrefText.length() < 255 ) {
            final boolean nonAlpha = tokenizedPrefText.chars()
                  .filter( c -> !Character.isAlphabetic( c ) )
                  .findFirst().isPresent();
            if ( !nonAlpha ) {
               _textCounts.put( tokenizedPrefText, 1 );
            }
         }
      }
      if ( _textCounts.size() < 2 ) {
         return;
      }
      final List<String> textList = new ArrayList<>( _textCounts.keySet() );
      final Collection<String> extensionTexts = new HashSet<>();
      for ( int i = 0; i < textList.size() - 1; i++ ) {
         final String iText = textList.get( i );
         for ( int j = i + 1; j < textList.size(); j++ ) {
            final String jText = textList.get( j );
            if ( textContained( jText, iText ) ) {
               extensionTexts.add( jText );
            } else if ( textContained( iText, jText ) ) {
               extensionTexts.add( iText );
            }
         }
      }
      removeTexts( extensionTexts );
   }

   static private boolean textContained( final String containerText, final String containedText ) {
      final int index = containerText.indexOf( containedText );
      return index >= 0
             && (index == 0 || containerText.charAt( index - 1 ) == ' ')
             && (index + containedText.length() == containerText.length() ||
                 containerText.charAt( index + containedText.length() ) == ' ');
   }


   public void setPreferredText( final String text ) {
      _preferredText = text;
   }

   public String getPreferredText() {
      if ( _preferredText != null ) {
         return _preferredText;
      }
      return PREFERRED_TERM_UNKNOWN;
   }

   public void addCode( final String source, final String code ) {
      if ( _codes.placeValue( source, code ) ) {
         VocabularyStore.getInstance().addVocabulary( source, code );
      }
   }

   public Collection<String> getVocabularies() {
      return _codes.keySet();
   }

   public Collection<String> getCodes( final String source ) {
      final Collection<String> codes = _codes.getCollection( source );
      if ( codes == null ) {
         return Collections.emptyList();
      }
      return codes;
   }

   public void addTui( final SemanticTui tui ) {
      _tuis.add( tui );
   }

   public Collection<SemanticTui> getTuis() {
      return _tuis;
   }

   public boolean hasTui( final Collection<SemanticTui> tuis ) {
      return _tuis.stream().anyMatch( tuis::contains );
   }

   public boolean isEmpty() {
      return _textCounts.isEmpty();
   }

}
