package org.apache.ctakes.gui.pipeline.bit.available;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.PipeBitInfoUtil;
import org.apache.ctakes.gui.pipeline.bit.info.PipeBitInfoComparator;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/13/2017
 */
final public class AvailablesListModel extends AbstractListModel<PipeBitInfo> {

   static private final Logger LOGGER = Logger.getLogger( "AvailablesListModel" );


   private final List<PipeBitInfo> _pipeBitInfos = new ArrayList<>();

   private final Map<PipeBitInfo, Class<?>> _pipeBitMap = new HashMap<>();

   /**
    * Populate the list
    *
    * @param bits -
    */
   public void setPipeBits( final Collection<Class<?>> bits ) {
      final int oldSize = _pipeBitInfos.size();
      if ( oldSize > 0 ) {
         _pipeBitInfos.clear();
         _pipeBitMap.clear();
      }
      if ( bits == null || bits.isEmpty() ) {
         if ( oldSize > 0 ) {
            fireIntervalRemoved( this, 0, oldSize - 1 );
         }
         return;
      }
      for ( Class<?> bit : bits ) {
         final PipeBitInfo info = PipeBitInfoUtil.getInfo( bit );
         _pipeBitInfos.add( info );
         _pipeBitMap.put( info, bit );
      }
      _pipeBitInfos.sort( PipeBitInfoComparator.getInstance() );
      fireContentsChanged( this, 0, _pipeBitInfos.size() );
   }

   /**
    * @param pipeBitInfo -
    * @return the actual pipeline bit class
    */
   public Class<?> getPipeBit( final PipeBitInfo pipeBitInfo ) {
      return _pipeBitMap.get( pipeBitInfo );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getSize() {
      return _pipeBitInfos.size();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public PipeBitInfo getElementAt( final int index ) {
      return _pipeBitInfos.get( index );
   }


}
