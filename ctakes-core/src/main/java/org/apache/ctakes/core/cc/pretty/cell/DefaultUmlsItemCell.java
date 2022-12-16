package org.apache.ctakes.core.cc.pretty.cell;

import org.apache.ctakes.core.cc.pretty.SemanticGroup;
import org.apache.ctakes.core.util.textspan.TextSpan;

import java.util.*;

/**
 * Item Cell that holds information about an umls entity
 */
public final class DefaultUmlsItemCell extends AbstractItemCell implements UmlsItemCell {

   static private final int CUI_SPAN = 8;
   static private final String NEGATED_TEXT = "Negated";


   final private int _semanticWidth;
   final private int _height;
   final private boolean _negated;
   final private List<String> _semanticTextLines;

   /**
    * @param semanticCuiNames -
    * @return a list of semantic type names, sorted in the order of ctakes semantic group names followed by
    * alphabetical custom names
    */
   static private Collection<String> getSortedSemanticNames( final Collection<String> semanticCuiNames ) {
      final Collection<String> semanticGroupNames = new ArrayList<>();
      for ( SemanticGroup semanticGroup : SemanticGroup.values() ) {
         semanticGroupNames.add( semanticGroup.getName() );
      }
      final List<String> sortedCuiNames = new ArrayList<>( semanticCuiNames );
      semanticGroupNames.retainAll( sortedCuiNames );
      sortedCuiNames.removeAll( semanticGroupNames );
      Collections.sort( sortedCuiNames );
      semanticGroupNames.addAll( sortedCuiNames );
      return semanticGroupNames;
   }


   public DefaultUmlsItemCell( final TextSpan textSpan, final int polarity,
                               final Map<String, Collection<String>> semanticCuis ) {
      super( textSpan );
      int width = CUI_SPAN;
      _negated = polarity < 0;
      _semanticTextLines = new ArrayList<>();
      final List<String> sortedCuis = new ArrayList<>();
      final Collection<String> semanticGroupNames = getSortedSemanticNames( semanticCuis.keySet() );
      for ( String semanticName : semanticGroupNames ) {
         final Collection<String> cuis = semanticCuis.get( semanticName );
         if ( cuis != null ) {
            width = Math.max( width, semanticName.length() );
            _semanticTextLines.add( semanticName );
            sortedCuis.addAll( cuis );
            Collections.sort( sortedCuis );
            _semanticTextLines.addAll( sortedCuis );
            sortedCuis.clear();
         }
      }
      _semanticWidth = width;
      int height = 1 + _semanticTextLines.size(); // text + cuis
      if ( _negated ) {
         height++;  // to print negation
      }
      _height = height;
   }

   /**
    * {@inheritDoc}
    *
    * @return the maximum of the document text length, the semantic type length and the cui length (8)
    */
   @Override
   public int getWidth() {
      return Math.max( getTextSpan().getWidth(), _semanticWidth );
   }

   /**
    * {@inheritDoc}
    *
    * @return the 1 for the text span representation line + number of semantic types + cuis + 1 if negated
    */
   @Override
   public int getHeight() {
      return _height;
   }

   /**
    * {@inheritDoc}
    *
    * @return {@link UmlsItemCell#ENTITY_FILL}
    */
   @Override
   public String getText() {
      return ENTITY_FILL;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isNegated() {
      return _negated;
   }

   /**
    * {@inheritDoc}
    *
    * @return {@link UmlsItemCell#ENTITY_FILL} for index 0, Semantic types and Cuis for lines after that, then negated
    */
   @Override
   public String getLineText( final int lineIndex ) {
      if ( lineIndex == 0 ) {
         return ENTITY_FILL;
      } else if ( lineIndex > 0 && lineIndex - 1 < _semanticTextLines.size() ) {
         return _semanticTextLines.get( lineIndex - 1 );
      } else if ( isNegated() && lineIndex - 1 == _semanticTextLines.size() ) {
         return NEGATED_TEXT;
      }
      return "";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals( final Object other ) {
      return other instanceof DefaultUmlsItemCell
             && getTextSpan().equals( ((DefaultUmlsItemCell)other).getTextSpan() )
             && isNegated() == ((DefaultUmlsItemCell)other).isNegated()
             && _semanticTextLines.equals( ((DefaultUmlsItemCell)other)._semanticTextLines );
   }

   public int hashCode() {
      return 2 * getTextSpan().hashCode() + 2 * _semanticTextLines.hashCode() + (isNegated() ? 1 : 0);
   }

}
