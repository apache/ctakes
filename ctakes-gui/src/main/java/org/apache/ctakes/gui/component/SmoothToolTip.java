package org.apache.ctakes.gui.component;


import sun.swing.SwingUtilities2;

import javax.swing.*;
import javax.swing.plaf.ToolTipUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicToolTipUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/23/2016
 */
public class SmoothToolTip extends JToolTip {

   private static final String UI_CLASS_ID = "SmoothToolTipUI";

   /**
    * This is set to true for the life of the <code>setUI</code> call.
    */
   private boolean _settingUI;

   public SmoothToolTip() {
      setBorder( null );
      setOpaque( false );
   }


   /**
    * @return the <code>TimelineUI</code> object that renders this component
    */
   @Override
   final public ToolTipUI getUI() {
      return (ToolTipUI)ui;
   }

   final public void setUI( final ToolTipUI ui ) {
      if ( _settingUI ) {
         return;
      }
      if ( this.ui != ui ) {
         _settingUI = true;
         try {
            super.setUI( ui );
         } finally {
            _settingUI = false;
         }
      }
   }

   /**
    * Notification from the <code>UIManager</code> that the L&F has changed.
    * Replaces the current UI object with the latest version from the
    * <code>UIManager</code>.
    *
    * @see JComponent#updateUI
    */
   @Override
   final public void updateUI() {
      setUI( new SmoothToolTipUI() );
   }


   /**
    * Returns the name of the L&F class that renders this component.
    *
    * @return the string "TimelineUI"
    * @see JComponent#getUIClassID
    * @see UIDefaults#getUI
    */
   @Override
   public String getUIClassID() {
      return UI_CLASS_ID;
   }

   @Override
   public void paintBorder( final Graphics g ) {
   }


   static private final class SmoothToolTipUI extends BasicToolTipUI {

      @Override
      public void update( Graphics g, JComponent c ) {
         paint( g, c );
      }

      @Override
      public void paint( final Graphics g, final JComponent comp ) {
         final Font font = comp.getFont();
         final FontMetrics metrics = SwingUtilities2.getFontMetrics( comp, g, font );
         final Dimension size = comp.getSize();

         final Color background = Color.YELLOW;//c.getBackground();
//         Color transground = new Color( background.getRed(), background.getGreen(), background.getBlue(), 127 );
         Color transground = new Color( background.getRed(), background.getGreen(), background.getBlue(), 195 );
         g.setColor( transground );
         g.fillRoundRect( 0, 0, size.width, size.height, 6, 3 );

         final Color foreground = comp.getForeground();
         transground = new Color( foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 195 );
         g.setColor( transground );

         // fix for bug 4153892
         String tipText = ((JToolTip)comp).getTipText();
         if ( tipText == null ) {
            tipText = "";
         }

         final Insets insets = comp.getInsets();
         final RoundRectangle2D.Float paintTextR = new RoundRectangle2D.Float(
               insets.left + 3,
               insets.top,
               size.width - (insets.left + insets.right) - 6,
               size.height - (insets.top + insets.bottom), 10, 10 );
         g.setFont( font );
//         SwingUtilities2.drawString( comp, g, tipText, (int)paintTextR.getX(),
//               (int)paintTextR.getY() + metrics.getAscent() );
         BasicGraphicsUtils.drawString( g, tipText, '\0', (int)paintTextR.getX(),
                                     (int)paintTextR.getY() + metrics.getAscent() );
         transground = new Color( foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 63 );
         g.setColor( transground );
         g.drawRoundRect( 0, 0, size.width - 1, size.height - 1, 10, 10 );
      }
   }

}
