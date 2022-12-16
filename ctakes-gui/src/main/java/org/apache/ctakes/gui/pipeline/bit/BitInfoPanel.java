package org.apache.ctakes.gui.pipeline.bit;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.gui.component.FileTableCellEditor;
import org.apache.ctakes.gui.component.SmoothTipTable;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterCellRenderer;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterInfoPanel;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterTableModel;
import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/19/2017
 */
abstract public class BitInfoPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "BitInfoPanel" );

   protected PipeBitInfo _pipeBitInfo;
   protected Class<?> _pipeBitClass;

   protected JComponent _name;
   protected JLabel _description;
   protected JLabel _dependencies;
   protected JLabel _usables;
   protected JLabel _products;

   protected ParameterTableModel _parameterTableModel;
   protected ParameterInfoPanel _parameterInfoPanel;

   public BitInfoPanel() {
      super( new BorderLayout( 5, 5 ) );

      add( createMainPanel(), BorderLayout.NORTH );

      _parameterTableModel = new ParameterTableModel();
      final JTable parameterTable = createParameterTable( _parameterTableModel );
      add( new JScrollPane( parameterTable ), BorderLayout.CENTER );

      _parameterInfoPanel = createParameterInfoPanel();
      _parameterInfoPanel.setParameterTable( parameterTable );
      add( _parameterInfoPanel, BorderLayout.SOUTH );
   }

   final public PipeBitInfo getPipeBitInfo() {
      return _pipeBitInfo;
   }

   final public Class<?> getPipeBitClass() {
      return _pipeBitClass;
   }

   final public String getBitName() {
      return ((JLabel)_name).getText();
   }

   final public String getDescription() {
      return _description.getText();
   }

   final public ParameterTableModel getParameterModel() {
      return _parameterTableModel;
   }


   abstract protected String getNameLabelPrefix();

   abstract protected JComponent createNameEditor();

   abstract protected void setBitName( final String text, final String toolTip );

   abstract protected ParameterInfoPanel createParameterInfoPanel();

   protected void clear() {
      _pipeBitInfo = null;
      _pipeBitClass = null;
      setBitName( "", "" );
      clear( _description );
      clear( _dependencies );
      clear( _usables );
      clear( _products );
      _parameterTableModel.setParameterHolder( null );
      _parameterInfoPanel.setParameterHolder( null );
   }

   protected void clear( final JLabel label ) {
      setLabelText( label, "", "" );
   }

   protected void setLabelText( final JLabel label, final String text, final String toolTip ) {
      label.setText( text );
      label.setToolTipText( toolTip );
   }

   private JComponent createMainPanel() {
      _name = createNameEditor();
      final JComponent namePanel = createNamePanel( getNameLabelPrefix() + " Bit Name:", _name );
      _description = new JLabel();
      final JComponent descPanel = createNamePanel( "Description:", _description );
      _dependencies = new JLabel();
      final JComponent inPanel = createNamePanel( "Dependencies:", _dependencies );
      _usables = new JLabel();
      final JComponent usablePanel = createNamePanel( "Usable:", _usables );
      _products = new JLabel();
      final JComponent outPanel = createNamePanel( "Products:", _products );

      final JPanel panel = new JPanel( new GridLayout( 5, 1 ) );
      panel.add( namePanel );
      panel.add( descPanel );
      panel.add( inPanel );
      panel.add( usablePanel );
      panel.add( outPanel );
      return panel;
   }

   static private JTable createParameterTable( final TableModel model ) {
      final JTable table = new SmoothTipTable( model );
      table.setRowHeight( 20 );
      table.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
      table.getColumnModel().getColumn( 0 ).setPreferredWidth( 100 );
      table.getColumnModel().getColumn( 2 ).setMaxWidth( 25 );
      final TableRowSorter<TableModel> sorter = new TableRowSorter<>( model );
      table.setAutoCreateRowSorter( true );
      table.setRowSorter( sorter );
      table.setRowSelectionAllowed( true );
      table.setCellSelectionEnabled( true );
      table.setDefaultRenderer( ConfigurationParameter.class, new ParameterCellRenderer() );
      final FileTableCellEditor fileEditor = new FileTableCellEditor();
      fileEditor.getFileChooser().setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
      table.setDefaultRenderer( File.class, fileEditor );
      table.setDefaultEditor( File.class, fileEditor );
      ListSelectionModel selectionModel = table.getSelectionModel();
      selectionModel.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
      return table;
   }

   static private JComponent createNamePanel( final String name, final JComponent nameLabel ) {
      final JPanel panel = new JPanel( new BorderLayout( 10, 10 ) );
      panel.setBorder( new EmptyBorder( 2, 10, 2, 10 ) );
      final JLabel label = new JLabel( name );
      label.setPreferredSize( new Dimension( 90, 20 ) );
      label.setHorizontalAlignment( SwingConstants.TRAILING );
      final Border emptyBorder = new EmptyBorder( 0, 10, 0, 0 );
      final Border border
            = new CompoundBorder( UIManager.getLookAndFeelDefaults().getBorder( "TextField.border" ), emptyBorder );
      nameLabel.setBorder( border );
      panel.add( label, BorderLayout.WEST );
      panel.add( nameLabel, BorderLayout.CENTER );
      return panel;
   }

}
