package org.apache.ctakes.gui.action;

import javax.swing.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/29/2016
 */
public interface HotKeyManager {

   void addHotKey( final String name, final KeyStroke keyStroke, final Action action );

}
