package org.apache.ctakes.gui.component;


import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/24/2017
 */
public class CellRendererLabel extends JLabel {

   static private final Logger LOGGER = Logger.getLogger( "CellRendererLabel" );

   public CellRendererLabel() {
      super();
   }

   public CellRendererLabel( final Icon icon ) {
      super( icon );
   }

   public CellRendererLabel( final String text ) {
      super( text );
   }

   @Override
   public void validate() {
   }

   @Override
   public void invalidate() {
   }

   @Override
   public void repaint() {
   }

   @Override
   public void revalidate() {
   }

   @Override
   public void repaint( long tm, int x, int y, int width, int height ) {
   }

   @Override
   public void repaint( Rectangle r ) {
   }

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

   @Override
   public void firePropertyChange( String propertyName, byte oldValue, byte newValue ) {
   }

   @Override
   public void firePropertyChange( String propertyName, char oldValue, char newValue ) {
   }

   @Override
   public void firePropertyChange( String propertyName, short oldValue, short newValue ) {
   }

   @Override
   public void firePropertyChange( String propertyName, int oldValue, int newValue ) {
   }

   @Override
   public void firePropertyChange( String propertyName, long oldValue, long newValue ) {
   }

   @Override
   public void firePropertyChange( String propertyName, float oldValue, float newValue ) {
   }

   @Override
   public void firePropertyChange( String propertyName, double oldValue, double newValue ) {
   }

   @Override
   public void firePropertyChange( String propertyName, boolean oldValue, boolean newValue ) {
   }


}
