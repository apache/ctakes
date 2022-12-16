package org.apache.ctakes.gui.component;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A Panel that makes everything on the GUI "disabled".
 * Useful when there is a resource-heavy thread running and GUI interaction is undesirable.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/29/2016
 */
public final class DisablerPane extends JPanel implements MouseListener,
                                                          MouseMotionListener,
                                                          FocusListener {

   static public DisablerPane getInstance() {
      return InstanceHolder.INSTANCE;
   }

   static private final class InstanceHolder {
      static private final DisablerPane INSTANCE = new DisablerPane();
   }

   static private final Logger LOGGER = Logger.getLogger( "DisablerPane" );

   private JMenuBar _menuBar;
   private Container _contentPane;

   private boolean _dragging = false;
   private boolean _needToRedispatch = false;

   private DisablerPane() {
      addMouseListener( this );
      addMouseMotionListener( this );
      addFocusListener( this );
      setOpaque( false );
      setVisible( false );
   }

   /**
    * The DisablerPane must be initialized before it can be used
    *
    * @param frame -
    */
   public void initialize( final JFrame frame ) {
      _menuBar = frame.getJMenuBar();
      _contentPane = frame.getContentPane();
      frame.setGlassPane( this );
   }

   /**
    * We only need to redispatch if we're not visible, but having full control
    * over this might prove handy.
    */
   public void setNeedToRedispatch( final boolean need ) {
      _needToRedispatch = need;
   }

   /**
    * Paint partially transparent grey over the frame
    * {@inheritDoc}
    */
   @Override
   protected void paintComponent( final Graphics g ) {
      if ( !isVisible() ) {
         return;
      }
      g.setColor( new Color( 127, 127, 127, 83 ) );
      g.fillRect( 0, 0, getWidth(), getHeight() );
   }

   /**
    * Blocks input if visible
    * {@inheritDoc}
    */
   @Override
   public void setVisible( final boolean visible ) {
      // Make sure we grab the focus so that key events don't go astray.
      if ( visible ) {
         requestFocus();
      }
      super.setVisible( visible );
   }

   /**
    * Once we have focus, keep it if we're visible
    * {@inheritDoc}
    */
   @Override
   public void focusLost( final FocusEvent event ) {
      if ( isVisible() ) {
         requestFocus();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void focusGained( final FocusEvent event ) {
   }

   /*
    * (Based on code from the Java Tutorial) We must forward at least the mouse
    * drags that started with mouse presses over the check box. Otherwise, when
    * the user presses the check box then drags off, the check box isn't
    * disarmed -- it keeps its dark gray background or whatever its L&F uses to
    * indicate that the button is currently being pressed.
    */
   @Override
   public void mouseDragged( final MouseEvent event ) {
      if ( _needToRedispatch ) {
         forwardMouseEvent( event );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void mouseMoved( final MouseEvent event ) {
      if ( _needToRedispatch ) {
         forwardMouseEvent( event );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void mouseClicked( final MouseEvent event ) {
      if ( _needToRedispatch ) {
         forwardMouseEvent( event );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void mouseEntered( final MouseEvent event ) {
      if ( _needToRedispatch ) {
         forwardMouseEvent( event );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void mouseExited( final MouseEvent event ) {
      if ( _needToRedispatch ) {
         forwardMouseEvent( event );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void mousePressed( final MouseEvent event ) {
      if ( _needToRedispatch ) {
         forwardMouseEvent( event );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void mouseReleased( final MouseEvent event ) {
      if ( _needToRedispatch ) {
         forwardMouseEvent( event );
         _dragging = false;
      }
   }

   /**
    * If there is a mouse event, forward to an underlying component
    *
    * @param event -
    */
   private void forwardMouseEvent( final MouseEvent event ) {
      if ( _contentPane == null || _menuBar == null ) {
         LOGGER.error( "DisablerPane has not been initialized with a Frame" );
         return;
      }
      boolean inMenuBar = false;
      final Point point = event.getPoint();
      Component component;
      Container container = _contentPane;
      Point containerPoint = SwingUtilities.convertPoint( this, point, _contentPane );
      final int eventID = event.getID();

      if ( containerPoint.y < 0 ) {
         inMenuBar = true;
         container = _menuBar;
         containerPoint = SwingUtilities.convertPoint( this, point, _menuBar );
         isDragging( eventID );
      }
      component = SwingUtilities.getDeepestComponentAt( container, containerPoint.x, containerPoint.y );
      if ( component == null ) {
         return;
      } else {
         isDragging( eventID );
      }
      if ( inMenuBar || _dragging ) {
         final Point componentPoint = SwingUtilities.convertPoint( this, point, component );
         component.dispatchEvent( new MouseEvent( component, eventID, event.getWhen(), event.getModifiers(),
               componentPoint.x, componentPoint.y, event.getClickCount(), event.isPopupTrigger() ) );
      }
   }

   private void isDragging( final int eventID ) {
      if ( eventID == MouseEvent.MOUSE_PRESSED ) {
         _dragging = true;
      }
   }


}
