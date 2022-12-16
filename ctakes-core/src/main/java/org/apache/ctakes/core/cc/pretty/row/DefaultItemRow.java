package org.apache.ctakes.core.cc.pretty.row;

import org.apache.ctakes.core.cc.pretty.cell.ItemCell;
import org.apache.ctakes.core.cc.pretty.cell.UmlsItemCell;
import org.apache.ctakes.core.util.textspan.TextSpan;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/6/2015
 */
public final class DefaultItemRow implements ItemRow {

   static private final char END_CHAR = '|';
   static private final char FILL_CHAR = '=';

   final private Collection<ItemCell> _itemCells = new HashSet<>();
   private int _rowHeight = 0;

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean addItemCell( final ItemCell itemCell ) {
      final TextSpan textSpan = itemCell.getTextSpan();
      for ( ItemCell extantItemCell : _itemCells ) {
         if ( extantItemCell.getTextSpan().overlaps( textSpan ) ) {
            return false;
         }
      }
      _itemCells.add( itemCell );
      _rowHeight = Math.max( _rowHeight, itemCell.getHeight() );
      return true;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getHeight() {
      return _rowHeight;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<ItemCell> getItemCells() {
      return _itemCells;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getTextLine( final int lineIndex, final int rowWidth, final Map<Integer, Integer> offsetAdjustedMap ) {
      final char[] chars = new char[ rowWidth ];
      Arrays.fill( chars, ' ' );
      final StringBuilder sb = new StringBuilder( rowWidth );
      sb.append( chars );
      for ( ItemCell itemCell : _itemCells ) {
         final int begin = offsetAdjustedMap.get( itemCell.getTextSpan().getBegin() );
         final int end = offsetAdjustedMap.get( itemCell.getTextSpan().getEnd() );
         final int width = end - begin;
         final String annotationText = itemCell.getLineText( lineIndex );
         if ( annotationText.equals( UmlsItemCell.ENTITY_FILL ) ) {
            final char[] fill_chars = new char[ width ];
            Arrays.fill( fill_chars, FILL_CHAR );
            fill_chars[ 0 ] = END_CHAR;
            fill_chars[ fill_chars.length - 1 ] = END_CHAR;
            sb.replace( begin, begin + width, new String( fill_chars ) );
         } else {
            final int paddedOffset = getPaddedOffset( annotationText, width );
            sb.replace( begin + paddedOffset, begin + paddedOffset + annotationText.length(), annotationText );
         }
      }
      return sb.toString();
   }


   /**
    * @param text  to be printed
    * @param width of cell
    * @return required padding in characters to center text in cell
    */
   static private int getPaddedOffset( final CharSequence text, final int width ) {
      final int textWidth = text.length();
      if ( textWidth == width ) {
         return 0;
      }
      return (width - textWidth) / 2;
   }

}
