package org.apache.ctakes.gui.component;


import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Paths;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/28/2017
 */
final public class FileTableCellEditor extends AbstractCellEditor
      implements TableCellRenderer, TableCellEditor, ActionListener {

   static private final Logger LOGGER = Logger.getLogger( "FileTableCellEditor" );

   final private JButton _button;
   final private JFileChooser _chooser;
   private File _selectedFile;

   public FileTableCellEditor() {
      final Icon icon = UIManager.getIcon( "FileView.directoryIcon" );
      _button = new JButton( icon );
      _button.setFocusPainted( false );
      _button.setFocusable( false );
      _button.setToolTipText( "Select File" );
      _button.addActionListener( this );
      _chooser = new JFileChooser();
      String cwdPath = Paths.get( "" ).toAbsolutePath().toFile().getPath();
      if ( cwdPath.isEmpty() ) {
         cwdPath = System.getProperty( "user.dir" );
      }
      if ( cwdPath != null && !cwdPath.isEmpty() ) {
         _chooser.setCurrentDirectory( new File( cwdPath ) );
      }

   }

   public JFileChooser getFileChooser() {
      return _chooser;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Component getTableCellRendererComponent( final JTable table, final Object value,
                                                   final boolean isSelected, final boolean hasFocus,
                                                   final int row, final int column ) {
      return _button;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object getCellEditorValue() {
      return _selectedFile;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Component getTableCellEditorComponent( final JTable table, final Object value,
                                                 final boolean isSelected,
                                                 final int row, final int column ) {
      if ( File.class.isInstance( value ) ) {
         _selectedFile = (File)value;
      }
      return _button;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void actionPerformed( final ActionEvent event ) {
      final int option = _chooser.showOpenDialog( null );
      if ( option != JFileChooser.APPROVE_OPTION ) {
         return;
      }
      _selectedFile = _chooser.getSelectedFile();
      fireEditingStopped();
   }

}
