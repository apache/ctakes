package org.apache.ctakes.gui.action;


import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/29/2016
 */
final public class GlobalHotKeyManager extends EventQueue implements HotKeyManager {
   static private final HotKeyManager INSTANCE = new GlobalHotKeyManager();

   static public HotKeyManager getInstance() {
      return INSTANCE;
   }

   static {
      // here we register ourselves as a new link in the chain of
      // responsibility
      Toolkit.getDefaultToolkit().getSystemEventQueue().push( (EventQueue)INSTANCE );
   }

   private GlobalHotKeyManager() {
   }

   static private final Logger LOGGER = Logger.getLogger( "HotKeyManager" );

   private final InputMap _keyStrokes = new InputMap();
   private final ActionMap _actions = new ActionMap();


   /**
    * {@inheritDoc}
    */
   @Override
   public void addHotKey( final String name, final KeyStroke keyStroke, final Action action ) {
      _keyStrokes.put( keyStroke, name );
      _actions.put( name, action );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void dispatchEvent( final AWTEvent event ) {
      if ( event instanceof KeyEvent ) {
         // KeyStroke.getKeyStrokeForEvent converts an ordinary KeyEvent
         // to a keystroke, as stored in the InputMap.  Keep in mind that
         // Numpad keystrokes are different to ordinary keys, i.e. if you
         // are listening to
         final KeyStroke ks = KeyStroke.getKeyStrokeForEvent( (KeyEvent)event );
         String actionKey = (String)_keyStrokes.get( ks );
         if ( actionKey != null ) {
            final Action action = _actions.get( actionKey );
            if ( action != null && action.isEnabled() ) {
               // I'm not sure about the parameters
               action.actionPerformed(
                     new ActionEvent( event.getSource(), event.getID(),
                           actionKey, ((InputEvent)event).getModifiers() ) );
               return; // consume event
            }
         }
      }
      super.dispatchEvent( event ); // let the next in chain handle event
   }


}
