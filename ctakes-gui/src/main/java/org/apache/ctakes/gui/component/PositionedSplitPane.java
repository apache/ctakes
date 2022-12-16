package org.apache.ctakes.gui.component;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/29/2016
 */
final public class PositionedSplitPane extends JSplitPane {

   static private final Logger LOGGER = Logger.getLogger( "PositionedSplitPane" );

   private final Object LOCKER = new Object();
   private boolean _isLocationSet;
   private int _pixelLocation = -1;
   private double _proportionalLocation = -1d;

   /**
    * {@inheritDoc}
    */
   public PositionedSplitPane() {
      super();
   }

   /**
    * {@inheritDoc}
    */
   public PositionedSplitPane( final int orientation ) {
      super( orientation );
   }

   /**
    * {@inheritDoc}
    */
   public PositionedSplitPane( final int orientation, final boolean isContinuousLayout ) {
      super( orientation, isContinuousLayout );
   }

   /**
    * {@inheritDoc}
    */
   public PositionedSplitPane( final int orientation,
                               final Component leftComponent,
                               final Component rightComponent ) {
      super( orientation, leftComponent, rightComponent );
   }

   /**
    * {@inheritDoc}
    */
   public PositionedSplitPane( final int orientation,
                               final boolean isContinuousLayout,
                               final Component leftComponent,
                               final Component rightComponent ) {
      super( orientation, isContinuousLayout, leftComponent, rightComponent );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void setDividerLocation( final double proportionalLocation ) {
      super.setDividerLocation( proportionalLocation );
      _proportionalLocation = proportionalLocation;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setDividerLocation( final int pixelLocation ) {
      super.setDividerLocation( pixelLocation );
      _pixelLocation = pixelLocation;
   }

   /**
    * If this is the first paint, set the divider location first.  Fixes a bug (imo) in Java.
    * {@inheritDoc}
    */
   @Override
   public void paint( final Graphics g ) {
      synchronized ( LOCKER ) {
         if ( !_isLocationSet ) {
            if ( _pixelLocation > 0 ) {
               super.setDividerLocation( _pixelLocation );
            } else if ( _proportionalLocation > 0 ) {
               super.setDividerLocation( _proportionalLocation );
            }
            _isLocationSet = true;
         }
         super.paint( g );
      }
   }

}
