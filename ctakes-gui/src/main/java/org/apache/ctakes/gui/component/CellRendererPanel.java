package org.apache.ctakes.gui.component;


import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/31/2017
 */
final public class CellRendererPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "CellRendererPanel" );


   public CellRendererPanel( final LayoutManager layoutManager ) {
      super( layoutManager );
      setBackground( null );
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    *
    * @return <code>true</code> if the background is completely opaque
    * and differs from the JList's background;
    * <code>false</code> otherwise
    * @since 1.5
    */
   @Override
   public boolean isOpaque() {
      Color back = getBackground();
      Component p = getParent();
      if ( p != null ) {
         p = p.getParent();
      }
      // p should now be the JList.
      boolean colorMatch = (back != null) && (p != null) &&
                           back.equals( p.getBackground() ) &&
                           p.isOpaque();
      return !colorMatch && super.isOpaque();
   }


   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    *
    * @since 1.5
    */
   @Override
   public void repaint() {
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   public void revalidate() {
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   public void repaint( long tm, int x, int y, int width, int height ) {
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   public void repaint( Rectangle r ) {
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   protected void firePropertyChange( String propertyName, Object oldValue, Object newValue ) {
      // Strings get interned...
      if ( propertyName.equals( "text" )
           || ((propertyName.equals( "font" ) || propertyName.equals( "foreground" ))
               && oldValue != newValue
               && getClientProperty( javax.swing.plaf.basic.BasicHTML.propertyKey ) != null) ) {

         super.firePropertyChange( propertyName, oldValue, newValue );
      }
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   public void firePropertyChange( String propertyName, byte oldValue, byte newValue ) {
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   public void firePropertyChange( String propertyName, char oldValue, char newValue ) {
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   public void firePropertyChange( String propertyName, short oldValue, short newValue ) {
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   public void firePropertyChange( String propertyName, int oldValue, int newValue ) {
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   public void firePropertyChange( String propertyName, long oldValue, long newValue ) {
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   public void firePropertyChange( String propertyName, float oldValue, float newValue ) {
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   public void firePropertyChange( String propertyName, double oldValue, double newValue ) {
   }

   /**
    * Overridden for performance reasons.
    * See the <a href="#override">Implementation Note</a>
    * for more information.
    */
   @Override
   public void firePropertyChange( String propertyName, boolean oldValue, boolean newValue ) {
   }


}
