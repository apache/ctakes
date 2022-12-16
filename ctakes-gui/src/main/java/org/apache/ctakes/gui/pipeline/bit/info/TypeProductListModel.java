package org.apache.ctakes.gui.pipeline.bit.info;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.util.logging.Logger;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/25/2017
 */
final public class TypeProductListModel implements ListModel<TypeProduct> {

   static private final Logger LOGGER = Logger.getLogger( "TypeProductListModel" );

   private final TypeProduct[] _typeProducts;

   public TypeProductListModel() {
      _typeProducts = new TypeProduct[ TypeProduct.values().length - 1 ];
      int i = 0;
      for ( TypeProduct typeProduct : TypeProduct.values() ) {
         if ( typeProduct != TypeProduct.TOP ) {
            _typeProducts[ i ] = typeProduct;
            i++;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getSize() {
      return _typeProducts.length;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public TypeProduct getElementAt( final int index ) {
      return _typeProducts[ index ];
   }

   /**
    * does nothing.
    *
    * @param l -
    */
   @Override
   public void addListDataListener( final ListDataListener l ) {
   }

   /**
    * does nothing.
    *
    * @param l -
    */
   @Override
   public void removeListDataListener( final ListDataListener l ) {
   }

}
