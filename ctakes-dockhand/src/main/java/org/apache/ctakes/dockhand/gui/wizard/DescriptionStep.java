package org.apache.ctakes.dockhand.gui.wizard;


import org.apache.ctakes.gui.wizard.AbstractWizardStep;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2019
 */
final public class DescriptionStep extends AbstractWizardStep implements DhWizardStep {


   static private final String TYPE_DESCRIPTION = "Type the pipeline description.";

   private JTextArea _textArea;

   public DescriptionStep() {
      super( "Pipeline Description", "Type a Description for your Pipeline." );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected JComponent createPanel() {
      _textArea = new JTextArea( 0, 60 );
      _textArea.setBorder( new CompoundBorder( new LineBorder( Color.LIGHT_GRAY, 2, true ),
            new EmptyBorder( 10, 10, 10, 10 ) ) );
      _textArea.setText( TYPE_DESCRIPTION );
      final JPanel panel = new JPanel( new BorderLayout() );
      panel.add( _textArea, BorderLayout.CENTER );
      return wrapInScrollPane( panel );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<String> getPiperCommands() {
      if ( _textArea == null ) {
         return Collections.emptyList();
      }
      final String description = _textArea.getText();
      if ( description.trim().isEmpty() ) {
         return Collections.emptyList();
      }

      final String[] lines = description.split( "\\r?\\n" );

      final Function<String, String> ensureComment
            = s -> (s.startsWith( "//" ) || s.startsWith( "#" )) ? s : "// " + s;

      return Arrays.stream( lines )
                   .map( ensureComment )
                   .collect( Collectors.toList() );
   }


   public String getSummaryInfo() {
      if ( _textArea == null ) {
         return "";
      }
      final String text = _textArea.getText();
      if ( text.equals( TYPE_DESCRIPTION ) ) {
         return "";
      }
      return "<HTML>" + text.replace( "\n", "<BR>" ) + "</HTML>";
   }


}
