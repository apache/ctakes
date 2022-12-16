package org.apache.ctakes.gui.wizard;

import org.apache.ctakes.gui.wizard.util.DialogUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/10/2019
 */
final class TocPanel {


   public JComponent createPanel( final WizardController wizardController ) {
      final JList<WizardStep> stepList = new JList<>( wizardController );
      stepList.setEnabled( false );
      stepList.setSelectionModel( wizardController );
      stepList.setBorder( new EmptyBorder( 50, 5, 5, 5 ) );
      final Color selectColor = stepList.getSelectionBackground();
      final Color transColor = new Color( selectColor.getRed(), selectColor.getGreen(), selectColor.getBlue(), 125 );
      stepList.setSelectionBackground( transColor );

      final JScrollPane scrollPane = new JScrollPane();
      scrollPane.setBorder( null );
      scrollPane.setViewportView( stepList );

      final JPanel panel = new JPanel( new BorderLayout( 10, 10 ) );
      panel.setBackground( Color.WHITE );
      try {
         final InputStream imageStream = getClass().getResourceAsStream( "/org/apache/ctakes/image/ctakes_logo.jpg" );
         if ( imageStream != null ) {
            panel.add( new JLabel( new ImageIcon( ImageIO.read( imageStream ), "Apache cTAKES" ) ), BorderLayout.NORTH );
//         } else {
//            LOGGER.warning( "No Stream" );
         }
      } catch ( IOException ioE ) {
         DialogUtil.showError( ioE.getMessage() );
      }
      panel.add( scrollPane, BorderLayout.CENTER );

//      panel.add( ProgressNote.getInstance().getComponent(), BorderLayout.SOUTH );

      return panel;
   }


}
