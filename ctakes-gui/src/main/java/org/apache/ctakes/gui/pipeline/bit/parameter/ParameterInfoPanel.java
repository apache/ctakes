package org.apache.ctakes.gui.pipeline.bit.parameter;

import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/20/2017
 */
public class ParameterInfoPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "ParameterInfoPanel" );


   private JLabel _name;
   private JLabel _type;
   private JLabel _description;
   private JLabel _mandatory;
   protected JComponent _values;

   private ParameterHolder _holder;

   public ParameterInfoPanel() {
      super( new GridLayout( 5, 1 ) );

      _name = new JLabel();
      final JComponent namePanel = createNamePanel( "Parameter Name:", _name );
      _type = new JLabel();
      final JComponent typePanel = createNamePanel( "Parameter Type:", _type );
      _description = new JLabel();
      final JComponent descPanel = createNamePanel( "Description:", _description );
      _mandatory = new JLabel();
      final JComponent mandatoryPanel = createNamePanel( "Mandatory:", _mandatory );
      _values = createValuesEditor();
      final JComponent valuesPanel = createNamePanel( getValueLabelPrefix() + " Values:", _values );

      add( namePanel );
      add( typePanel );
      add( descPanel );
      add( mandatoryPanel );
      add( valuesPanel );
   }

   protected String getValueLabelPrefix() {
      return "Default";
   }

   protected JComponent createValuesEditor() {
      return new JLabel();
   }

   protected void setParameterValues( final String values ) {
      ((JLabel)_values).setText( values );
   }

   private JComponent createNamePanel( final String name, final JComponent namedLabel ) {
      final JPanel panel = new JPanel( new BorderLayout( 10, 10 ) );
      panel.setBorder( new EmptyBorder( 2, 10, 2, 10 ) );
      final JLabel label = new JLabel( name );
      label.setPreferredSize( new Dimension( 90, 20 ) );
      label.setHorizontalAlignment( SwingConstants.TRAILING );
      final Border emptyBorder = new EmptyBorder( 0, 10, 0, 0 );
      final Border border
            = new CompoundBorder( UIManager.getLookAndFeelDefaults().getBorder( "TextField.border" ), emptyBorder );
      namedLabel.setBorder( border );
      panel.add( label, BorderLayout.WEST );
      panel.add( namedLabel, BorderLayout.CENTER );
      return panel;
   }

   private void clear() {
      clear( _name );
      clear( _type );
      clear( _description );
      clear( _mandatory );
      setParameterValues( "" );
   }

   private void clear( final JLabel label ) {
      setLabelText( label, "" );
   }

   private void setLabelText( final JLabel label, final String text ) {
      setLabelText( label, text, text );
   }

   private void setLabelText( final JLabel label, final String text, final String toolTip ) {
      label.setText( text );
      label.setToolTipText( toolTip );
   }

   public void setParameterHolder( final ParameterHolder holder ) {
      _holder = holder;
      clear();
   }

   private void setParameterIndex( final int index ) {
      if ( index < 0 || _holder == null ) {
         clear();
         return;
      }
      setLabelText( _name, _holder.getParameterName( index ) );
      setLabelText( _type, _holder.getParameterClass( index ) );
      setLabelText( _description, _holder.getParameterDescription( index ) );
      final boolean mandatory = _holder.isParameterMandatory( index );
      setLabelText( _mandatory, Boolean.toString( mandatory ),
            "A parameter Value is " + (mandatory ? "" : "not ") + "mandatory for the Pipe Bit to operate." );
      final String values = Arrays.stream( _holder.getParameterValue( index ) )
            .filter( v -> !ConfigurationParameter.NO_DEFAULT_VALUE.equals( v ) )
            .collect( Collectors.joining( " , " ) );
      setParameterValues( values );
   }

   public void setParameterTable( final JTable parameterTable ) {
      ListSelectionModel selectionModel = parameterTable.getSelectionModel();
      selectionModel.addListSelectionListener( new ParameterTableListener( parameterTable ) );
   }

   private class ParameterTableListener implements ListSelectionListener {
      private final JTable __parameterTable;

      private ParameterTableListener( final JTable parameterTable ) {
         __parameterTable = parameterTable;
      }

      @Override
      public void valueChanged( final ListSelectionEvent event ) {
         if ( event.getValueIsAdjusting() ) {
            return;
         }
         final int[] selectedRows = __parameterTable.getSelectedRows();
         if ( selectedRows.length == 0 ) {
            setParameterIndex( -1 );
         } else {
            setParameterIndex( selectedRows[ 0 ] );
         }
      }
   }


}
