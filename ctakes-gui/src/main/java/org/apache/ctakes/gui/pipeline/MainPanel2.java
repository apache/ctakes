package org.apache.ctakes.gui.pipeline;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.PiperFileReader;
import org.apache.ctakes.gui.component.*;
import org.apache.ctakes.gui.pipeline.bit.PipeBitFinder;
import org.apache.ctakes.gui.pipeline.bit.available.AvailablesListModel;
import org.apache.ctakes.gui.pipeline.bit.info.*;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterHolder;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterTableModel;
import org.apache.ctakes.gui.pipeline.piper.PiperFileView;
import org.apache.ctakes.gui.pipeline.piper.PiperTextFilter;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.JTableHeader;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static javax.swing.JOptionPane.PLAIN_MESSAGE;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/20/2016
 */
final class MainPanel2 extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "MainPanel" );

   private final JFileChooser _chooser = new JFileChooser();

   private final AvailablesListModel _availablesListModel = new AvailablesListModel();
   private JList<PipeBitInfo> _availablesList;
   private PipeBitInfoPanel _infoPanel;
   private DefaultStyledDocument _piperDocument;
   private PiperTextFilter _piperTextFilter;


   private JButton _newButton;
   private JButton _openButton;
   private JButton _saveButton;
   private JButton _validateButton;
   private JButton _runButton;
   private JButton _helpButton;

   private JTextPane _textPane;

   private JButton _addButton;
   private JButton _setButton;
   private JButton _loadButton;
   private JButton _packageButton;


   MainPanel2() {
      super( new BorderLayout() );
      final JSplitPane logSplit = new PositionedSplitPane( JSplitPane.VERTICAL_SPLIT );
      logSplit.setTopComponent( createMainPanel() );
      logSplit.setBottomComponent( LoggerPanel.createLoggerPanel() );
      logSplit.setDividerLocation( 0.6d );

      add( createToolBar(), BorderLayout.NORTH );

      add( logSplit, BorderLayout.CENTER );
      SwingUtilities.invokeLater( new ButtonIconLoader() );

      _chooser.setFileFilter( new FileNameExtensionFilter( "Pipeline Definition (Piper) File", "piper" ) );
      _chooser.setFileView( new PiperFileView() );
      createNewPiper();
   }


   private JComponent createWestPanel() {
      final JTable fakeTable = new JTable();
      final JTableHeader fakeHeader = fakeTable.getTableHeader();
      final Component header = fakeHeader.getDefaultRenderer().getTableCellRendererComponent( fakeTable,
            "Available Pipe Bits", false, false, -1, -1 );
      ((JLabel)header).setHorizontalAlignment( SwingConstants.CENTER );

      _availablesList = createPipeBitList( _availablesListModel );
      final JScrollPane scroll = new JScrollPane( _availablesList );
      scroll.setColumnHeaderView( header );
      scroll.setMinimumSize( new Dimension( 100, 10 ) );
      final JList<PipeBitInfo> rowHeaders = new JList<>( _availablesListModel );
      rowHeaders.setFixedCellHeight( 20 );
      rowHeaders.setCellRenderer( new RoleRenderer() );
      scroll.setRowHeaderView( rowHeaders );
      scroll.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

      final JSplitPane split = new JSplitPane();
      split.setLeftComponent( scroll );
      split.setRightComponent( createInfoPlusPanel() );
      split.setResizeWeight( 0.5 );
      split.setDividerLocation( 0.4d );
      return split;
   }

   private JComponent createInfoPlusPanel() {
      final JPanel panel = new JPanel( new BorderLayout() );
      _infoPanel = createBitInfoPanel( _availablesList );
      panel.add( _infoPanel, BorderLayout.CENTER );
      panel.add( createAddButtonPanel(), BorderLayout.EAST );
      return panel;
   }

   private JComponent createEastPanel() {
      _piperDocument = new DefaultStyledDocument();
      _piperTextFilter = new PiperTextFilter( _piperDocument );
      _textPane = new JTextPane( _piperDocument );
      _textPane.putClientProperty( "caretWidth", 2 );
      _textPane.setCaretColor( Color.MAGENTA );
      final JScrollPane scroll = new JScrollPane( _textPane );
      final TextLineNumber lineNumber = new TextLineNumber( _textPane, 2 );
      scroll.setRowHeaderView( lineNumber );
      scroll.setMinimumSize( new Dimension( 100, 10 ) );
      return scroll;
   }


   private JComponent createMainPanel() {
      final JComponent westPanel = createWestPanel();
      final JComponent eastPanel = createEastPanel();
      final JSplitPane mainSplit = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, westPanel, eastPanel );
      mainSplit.setDividerLocation( 0.6 );
      return mainSplit;
   }

   private JToolBar createToolBar() {
      final JToolBar toolBar = new JToolBar();
      toolBar.setFloatable( false );
      toolBar.setRollover( true );
      _newButton = addButton( toolBar, "Create New Piper File" );
      _newButton.addActionListener( new NewPiperAction() );
      _openButton = addButton( toolBar, "Open Existing Piper File" );
      _openButton.addActionListener( new OpenPiperAction() );
      _saveButton = addButton( toolBar, "Save Current Piper File" );
      _saveButton.addActionListener( new SavePiperAction() );
      toolBar.addSeparator( new Dimension( 20, 0 ) );
      _helpButton = addButton( toolBar, "Help" );
      _helpButton.addActionListener( new HelpAction() );
      toolBar.add( Box.createHorizontalGlue() );
      _validateButton = addButton( toolBar, "Validate Current Piper File" );
      _validateButton.addActionListener( new ValidateAction() );
      _runButton = addButton( toolBar, "Run Current Piper File" );
      _runButton.addActionListener( new RunAction() );
      _runButton.setEnabled( false );
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      return toolBar;
   }

   static private JButton addButton( final JToolBar toolBar, final String toolTip ) {
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      final JButton button = new JButton();
      button.setFocusPainted( false );
      // prevents first button from having a painted border
      button.setFocusable( false );
      button.setToolTipText( toolTip );
      toolBar.add( button );
      return button;
   }

   static private JButton addVerticalButton( final JToolBar toolBar, final String toolTip ) {
      toolBar.addSeparator( new Dimension( 0, 10 ) );
      final JButton button = new JButton();
      button.setFocusPainted( false );
      // prevents first button from having a painted border
      button.setFocusable( false );
      button.setToolTipText( toolTip );
      toolBar.add( button );
      return button;
   }

   static private JList<PipeBitInfo> createPipeBitList( final ListModel<PipeBitInfo> model ) {
      final JList<PipeBitInfo> bitList = new SmoothTipList<>( model );
      bitList.setCellRenderer( new PipeBitInfoRenderer() );
      bitList.setFixedCellHeight( 20 );
      return bitList;
   }

   static private PipeBitInfoPanel createBitInfoPanel( final JList<PipeBitInfo> list ) {
      final PipeBitInfoPanel pipeBitInfoPanel = new PipeBitInfoPanel();
      pipeBitInfoPanel.setPipeBitInfoList( list );
      pipeBitInfoPanel.setBorder( UIManager.getBorder( "ScrollPane.border" ) );
      return pipeBitInfoPanel;
   }

   void findPipeBits() {
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute( new PiperBitParser() );
      executor.shutdown();
   }


   private JComponent createAddButtonPanel() {
      final JToolBar toolBar = new JToolBar( SwingConstants.VERTICAL );
      toolBar.setFloatable( false );
      toolBar.setRollover( true );
      toolBar.addSeparator( new Dimension( 0, 30 ) );

      final AddAction addAction = new AddAction();
      final ParameterTableModel parameterModel = _infoPanel.getParameterModel();
      parameterModel.addTableModelListener( addAction );
      _addButton = addVerticalButton( toolBar, "Add selected Pipe Bit" );
      _addButton.addActionListener( addAction );
      _addButton.setEnabled( false );
      toolBar.addSeparator( new Dimension( 0, 60 ) );
      _setButton = addVerticalButton( toolBar, "Set Global Parameter" );
      _setButton.addActionListener( new SetAction() );

      toolBar.add( Box.createVerticalGlue() );
      _loadButton = addVerticalButton( toolBar, "Load SubPiper" );
      _loadButton.addActionListener( new LoadAction() );

      _packageButton = addVerticalButton( toolBar, "Add Package" );
      _packageButton.addActionListener( new PackageAction() );

      SwingUtilities.invokeLater( new CommandIconLoader() );
      return toolBar;
   }


   private void createNewPiper() {
      final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern( "MMMM dd, yyyy" );
      final LocalDate date = LocalDate.now();
      final String text = "//       ***  Piper File  ***\n" +
                          "//       Created by " + System.getProperty( "user.name" ) + "\n" +
                          "//       on " + date.format( dateFormatter ) + "\n\n";
      try {
         _piperDocument.remove( 0, _piperDocument.getLength() );
         _piperDocument.insertString( 0, text, null );
         _textPane.setCaretPosition( _piperDocument.getLength() );
      } catch ( BadLocationException blE ) {
         LOGGER.warn( blE.getMessage() );
      }
      _runButton.setEnabled( false );
   }


   private int getInsertCaret() throws BadLocationException {
      int caret = _textPane.getCaretPosition();
      if ( caret > 1 && _piperDocument.getText( caret - 1, 1 ).charAt( 0 ) == '\n' ) {
         return caret;
      }
      final int length = _piperDocument.getLength() - caret;
      final String docText = _piperDocument.getText( caret, length );
      boolean careted = false;
      for ( int i = 0; i < length - 1; i++ ) {
         if ( docText.charAt( i ) == '\n' ) {
            caret += i + 1;
            careted = true;
            break;
         }
      }
      if ( !careted ) {
         caret = _piperDocument.getLength();
      }
      return caret;
   }








   private class PiperBitParser implements Runnable {
      @Override
      public void run() {
         final JFrame frame = (JFrame)SwingUtilities.getRoot( MainPanel2.this );
         frame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
         DisablerPane.getInstance().setVisible( true );
         PipeBitFinder.getInstance().scan();
         _availablesListModel.setPipeBits( PipeBitFinder.getInstance().getPipeBits() );
         DisablerPane.getInstance().setVisible( false );
         frame.setCursor( Cursor.getDefaultCursor() );
      }
   }


   /**
    * Simple Runnable that loads an icon
    */
   private final class ButtonIconLoader implements Runnable {
      @Override
      public void run() {
         final String dir = "org/apache/ctakes/gui/pipeline/icon/";
         final String newPng = "NewPiper.png";
         final String openPng = "OpenPiper.png";
         final String savePng = "SavePiper.png";
         final String validatePng = "RunReady.png";
         final String runPng = "RunPiper.png";
         final String helpPng = "Help_32.png";
         final Icon newIcon = IconLoader.loadIcon( dir + newPng );
         final Icon openIcon = IconLoader.loadIcon( dir + openPng );
         final Icon saveIcon = IconLoader.loadIcon( dir + savePng );
         final Icon validateIcon = IconLoader.loadIcon( dir + validatePng );
         final Icon runIcon = IconLoader.loadIcon( dir + runPng );
         final Icon helpIcon = IconLoader.loadIcon( dir + helpPng );
         _newButton.setIcon( newIcon );
         _openButton.setIcon( openIcon );
         _saveButton.setIcon( saveIcon );
         _validateButton.setIcon( validateIcon );
         _runButton.setIcon( runIcon );
         _helpButton.setIcon( helpIcon );
      }
   }

   private final class NewPiperAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         createNewPiper();
      }
   }

   private final class OpenPiperAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         final int option = _chooser.showOpenDialog( null );
         if ( option != JFileChooser.APPROVE_OPTION ) {
            return;
         }
         String text = "";
         final File file = _chooser.getSelectedFile();
         try {
            text = Files.lines( Paths.get( file.getPath() ) ).collect( Collectors.joining( "\n" ) );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
            return;
         }
         try {
            _piperDocument.remove( 0, _piperDocument.getLength() );
            _piperDocument.insertString( 0, text, null );
         } catch ( BadLocationException blE ) {
            LOGGER.warn( blE.getMessage() );
         }
         _runButton.setEnabled( false );
      }
   }

   private final class SavePiperAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         if ( _piperDocument.getLength() == 0 ) {
            return;
         }
         final int option = _chooser.showSaveDialog( null );
         if ( option != JFileChooser.APPROVE_OPTION ) {
            return;
         }
         final File file = _chooser.getSelectedFile();
         try {
            String path = file.getPath();
            if ( !path.endsWith( ".piper" ) ) {
               path += ".piper";
            }
            final String text = _piperDocument.getText( 0, _piperDocument.getLength() );
            Files.write( Paths.get( path ), text.getBytes() );
         } catch ( BadLocationException | IOException multE ) {
            LOGGER.warn( multE.getMessage() );
         }
      }
   }

   static private final class HelpAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         final JPanel panel = new JPanel( new BorderLayout() );
         panel.add( new JLabel( "Dependency and Product Types." ), BorderLayout.NORTH );
         final JList<PipeBitInfo.TypeProduct> list = new JList<>( new TypeProductListModel() );
         list.setCellRenderer( new TypeProductRenderer() );
         panel.add( list, BorderLayout.CENTER );
         panel.add( new JLabel( "Types are associated with Pipe Bits." ), BorderLayout.SOUTH );
         JOptionPane.showMessageDialog( null, panel, "Type Products Help", PLAIN_MESSAGE, null );
      }
   }

   private final class ValidateAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         if ( _piperTextFilter == null || _runButton == null ) {
            return;
         }
         LOGGER.info( "Validating Piper File ..." );
         final boolean valid = _piperTextFilter.validateText();
         _runButton.setEnabled( valid );
         LOGGER.info( "Validation Complete." );
      }
   }

   private final class RunAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         if ( _piperTextFilter == null || _runButton == null ) {
            return;
         }
         LOGGER.info( "Running Piper File ..." );
         final ExecutorService executor = Executors.newSingleThreadExecutor();
         executor.execute( new PiperFileRunner() );
         executor.shutdown();
      }
   }

   private class PiperFileRunner implements Runnable {
      @Override
      public void run() {
         final JFrame frame = (JFrame)SwingUtilities.getRoot( MainPanel2.this );
         frame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
         DisablerPane.getInstance().setVisible( true );
         try {
            final PiperFileReader reader = new PiperFileReader();
            final String text = _piperDocument.getText( 0, _piperDocument.getLength() );
            final String[] lines = text.split( "\\n" );
            for ( String line : lines ) {
               reader.parsePipelineLine( line );
            }
            reader.getBuilder().run();
         } catch ( Throwable t ) {
            LOGGER.error( "Pipeline Run caused Exception:", t );
         }
         DisablerPane.getInstance().setVisible( false );
         frame.setCursor( Cursor.getDefaultCursor() );
      }
   }


   private final class CommandIconLoader implements Runnable {
      @Override
      public void run() {
         final String dir = "org/apache/ctakes/gui/pipeline/icon/";
         final String addPng = "PlusMark.png";
         final String setPng = "Parameters.png";
         final String loadPng = "BlueGearYellowGear.png";
         final String packagePng = "Folder_Blue.png";
         final Icon addIcon = IconLoader.loadIcon( dir + addPng );
         final Icon setIcon = IconLoader.loadIcon( dir + setPng );
         final Icon loadIcon = IconLoader.loadIcon( dir + loadPng );
         final Icon packageIcon = IconLoader.loadIcon( dir + packagePng );
         _addButton.setIcon( addIcon );
         _setButton.setIcon( setIcon );
         _loadButton.setIcon( loadIcon );
         _packageButton.setIcon( packageIcon );
      }
   }


   private final class SetAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         try {
            // TODO - hook "set" to the parameter selected in the parameter table.  Can use Value from table.
            // Would require tracking.  Or maybe a constant parse and track of variables set when the formatting is done.
            // Then the "add" button would need to be enabled accordingly ...
            // and the parameter table could display the mapped value.
            final int caret = getInsertCaret();
            _piperDocument.insertString( caret, "\n// Set a global value\nset ", null );
         } catch ( BadLocationException blE ) {
            LOGGER.error( blE.getMessage() );
         }
         _runButton.setEnabled( false );
      }
   }

   private final class LoadAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         try {
            final int caret = getInsertCaret();
            _piperDocument.insertString( caret, "\n// Load a Piper file containing a partial Pipeline\nload ", null );
         } catch ( BadLocationException blE ) {
            LOGGER.error( blE.getMessage() );
         }
         _runButton.setEnabled( false );
      }
   }

   private final class PackageAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         try {
            final int caret = getInsertCaret();
            _piperDocument
                  .insertString( caret, "\n// Add a Package that contains Pipe Bits or Piper files\npackage ", null );
         } catch ( BadLocationException blE ) {
            LOGGER.error( blE.getMessage() );
         }
         _runButton.setEnabled( false );
      }
   }

   static private final Function<String, String> maybeQuote = t -> t.contains( " " ) ? "\"" + t + "\"" : t;

   private final class AddAction implements ActionListener, TableModelListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         final String bitName = _infoPanel.getBitName();
         if ( bitName == null || bitName.isEmpty() ) {
            return;
         }
         final StringBuilder sb = new StringBuilder();
         sb.append( "\n" );
         final String description = _infoPanel.getDescription().trim();
         if ( !description.isEmpty() ) {
            sb.append( "//  " ).append( _infoPanel.getBitName() ).append( "\n" );
            sb.append( "//  " ).append( description ).append( "\n" );
         }
         final StringBuilder helpSb = new StringBuilder();
         final StringBuilder parmSb = new StringBuilder();
         final ParameterTableModel parameterModel = _infoPanel.getParameterModel();
         final ParameterHolder holder = parameterModel.getParameterHolder();
         final int count = holder.getParameterCount();
         final java.util.List<String[]> values = parameterModel.getValues();
         for ( int i = 0; i < count; i++ ) {
            final String[] value = values.get( i );
            if ( value != null && value.length > 0 &&
                 !Arrays.equals( value, holder.getParameter( i ).defaultValue() ) ) {
               helpSb.append( "#   " ).append( holder.getParameterName( i ) )
                     .append( "  " ).append( holder.getParameterDescription( i ) ).append( "\n" );
               final String valueText = Arrays.stream( value ).map( maybeQuote ).collect( Collectors.joining( "," ) );
               parmSb.append( " " ).append( holder.getParameterName( i ) ).append( "=" ).append( valueText );
            }
         }
         sb.append( helpSb.toString() );
         final PipeBitInfo info = _infoPanel.getPipeBitInfo();
         if ( info.role() == PipeBitInfo.Role.READER ) {
            sb.append( "reader " );
         } else {
            sb.append( "add " );
         }
         sb.append( _infoPanel.getPipeBitClass().getName() );
         sb.append( parmSb.toString() ).append( "\n" );
         try {
            final int caret = getInsertCaret();
            _piperDocument.insertString( caret, sb.toString(), null );
         } catch ( BadLocationException blE ) {
            LOGGER.error( blE.getMessage() );
         }
         _runButton.setEnabled( false );
      }

      @Override
      public void tableChanged( final TableModelEvent event ) {
         final ParameterTableModel parameterModel = _infoPanel.getParameterModel();
         final ParameterHolder holder = parameterModel.getParameterHolder();
         final int count = holder.getParameterCount();
         if ( count > 0 ) {
            final java.util.List<String[]> values = parameterModel.getValues();
            for ( int i = 0; i < count; i++ ) {
               final String[] value = values.get( i );
               if ( holder.isParameterMandatory( i )
                    && (value == null
                        || value.length == 0
                        || value[ 0 ].trim().isEmpty()
                        ||
                        value[ 0 ].equals( org.apache.uima.fit.descriptor.ConfigurationParameter.NO_DEFAULT_VALUE )) ) {
                  _addButton.setEnabled( false );
                  return;
               }
            }
         }
         _addButton.setEnabled( true );
      }
   }

}
