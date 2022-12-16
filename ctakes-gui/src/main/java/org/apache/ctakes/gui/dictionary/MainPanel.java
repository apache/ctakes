package org.apache.ctakes.gui.dictionary;

import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.gui.component.DisablerPane;
import org.apache.ctakes.gui.component.FileChooserPanel;
import org.apache.ctakes.gui.component.LoggerPanel;
import org.apache.ctakes.gui.component.PositionedSplitPane;
import org.apache.ctakes.gui.dictionary.umls.*;
import org.apache.ctakes.gui.dictionary.util.FileUtil;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.ctakes.gui.dictionary.DictionaryBuilder.CTAKES_APP_DB_PATH;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/10/2015
 */
final class MainPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "MainPanel" );

   private String _umlsDirPath = System.getProperty( "user.dir" );
   private String _ctakesPath = System.getProperty( "user.dir" );
   private final TuiTableModel _tuiModel = new TuiTableModel();
   private final SourceTableModel _sourceModel = new SourceTableModel();
   private final LanguageTableModel _languageModel = new LanguageTableModel();

   MainPanel() {
      super( new BorderLayout() );
      final JComponent sourceDirPanel = new JPanel( new GridLayout( 2, 1 ) );
      sourceDirPanel.add( new FileChooserPanel( "cTAKES Installation:", _ctakesPath, true, new CtakesDirListener() ) );
      sourceDirPanel.add( new FileChooserPanel( "UMLS Installation:", _umlsDirPath, true, new UmlsDirListener() ) );
      add( sourceDirPanel, BorderLayout.NORTH );

      add( createCenterPanel( _sourceModel, _tuiModel, _languageModel ), BorderLayout.CENTER );
   }

   private JComponent createCenterPanel( final TableModel sourceModel, final TableModel tuiModel,
                                         final TableModel languageModel ) {
      final JSplitPane centerSplit = new PositionedSplitPane();
      final JSplitPane leftSplit = new PositionedSplitPane();
      leftSplit.setTopComponent( createTable( sourceModel ) );
      leftSplit.setBottomComponent( createLangTable( languageModel ) );
      leftSplit.setDividerLocation( 0.8d );
      centerSplit.setLeftComponent( leftSplit );
      centerSplit.setRightComponent( createTable( tuiModel ) );
      centerSplit.setDividerLocation( 0.5d );

      final JPanel umlsPanel = new JPanel( new BorderLayout() );
      umlsPanel.add( centerSplit, BorderLayout.CENTER );
      umlsPanel.add( createGoPanel(), BorderLayout.SOUTH );

      final JSplitPane logSplit = new PositionedSplitPane( JSplitPane.VERTICAL_SPLIT );
      logSplit.setTopComponent( umlsPanel );
      logSplit.setBottomComponent( LoggerPanel.createLoggerPanel() );
      logSplit.setDividerLocation( 0.6d );

      return logSplit;
   }

   static private JComponent createTable( final TableModel model ) {
      final JTable table = new JTable( model );
      table.setCellSelectionEnabled( false );
      table.setShowVerticalLines( false );
      table.setAutoCreateRowSorter( true );
      table.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
      table.getColumnModel().getColumn( 0 ).setMaxWidth( 50 );
      table.getColumnModel().getColumn( 1 ).setMaxWidth( 50 );
      return new JScrollPane( table );
   }

   static private JComponent createLangTable( final TableModel model ) {
      final JTable table = new JTable( model );
      table.setCellSelectionEnabled( false );
      table.setShowVerticalLines( false );
      table.setAutoCreateRowSorter( true );
      table.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
      table.getColumnModel().getColumn( 0 ).setMaxWidth( 50 );
      return new JScrollPane( table );
   }

   private JComponent createGoPanel() {
      final JPanel panel = new JPanel( new BorderLayout( 10, 10 ) );
      panel.setBorder( new EmptyBorder( 2, 10, 2, 10 ) );
      final JLabel label = new JLabel( "Dictionary Name:" );
      label.setPreferredSize( new Dimension( 100, 0 ) );
      label.setHorizontalAlignment( SwingConstants.TRAILING );
      final JTextField textField = new JTextField( "custom" );
      final JButton buildButton = new JButton( new BuildDictionaryAction( textField ) );
      panel.add( label, BorderLayout.WEST );
      panel.add( textField, BorderLayout.CENTER );
      panel.add( buildButton, BorderLayout.EAST );
      return panel;
   }


   private String setUmlsDirPath( final String umlsDirPath ) {
      File mrConso = new File( umlsDirPath, "MRCONSO.RRF" );
      if ( mrConso.isFile() ) {
         _umlsDirPath = mrConso.getParentFile().getParent();
      } else {
         final String plusMetaPath = new File( umlsDirPath, "META" ).getPath();
         mrConso = new File( plusMetaPath, "MRCONSO.RRF" );
         if ( mrConso.isFile() ) {
            _umlsDirPath = umlsDirPath;
         } else {
            error( "Invalid UMLS Installation", umlsDirPath + " is not a valid path to a UMLS installation" );
         }
      }
      return _umlsDirPath;
   }

   private void loadSources() {
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute( new SourceLoadRunner( _umlsDirPath ) );
   }

   private class SourceLoadRunner implements Runnable {
      private final String __umlsDirPath;

      private SourceLoadRunner( final String umlsDirPath ) {
         __umlsDirPath = umlsDirPath;
      }

      @Override
      public void run() {
         final JFrame frame = (JFrame)SwingUtilities.getRoot( MainPanel.this );
         frame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
         DisablerPane.getInstance().setVisible( true );
         final File mrConso = new File( __umlsDirPath + "/META", "MRCONSO.RRF" );
         final String mrConsoPath = mrConso.getPath();
         LOGGER.info( "Parsing vocabulary types from " + mrConsoPath );
         final Collection<String> sources = new HashSet<>();
         final Collection<String> languages = new HashSet<>();
         try ( final BufferedReader reader = FileUtil.createReader( mrConsoPath ) ) {
            int lineCount = 0;
            java.util.List<String> tokens = FileUtil.readBsvTokens( reader, mrConsoPath );
            while ( tokens != null ) {
               lineCount++;
               if ( tokens.size() > MrconsoIndex.SOURCE._index ) {
                  sources.add( tokens.get( MrconsoIndex.SOURCE._index ) );
                  languages.add( tokens.get( MrconsoIndex.LANGUAGE._index ) );
               }
               if ( lineCount % 100000 == 0 ) {
                  LOGGER.info( "File Line " + lineCount + "\t Vocabularies " + sources.size() );
               }
               tokens = FileUtil.readBsvTokens( reader, mrConsoPath );
            }
            LOGGER.info( "Parsed " + sources.size() + " vocabulary types" );
            _sourceModel.setSources( sources );
            LOGGER.info( "Parsed " + languages.size() + " languages" );
            _languageModel.setLangauges( languages );
         } catch ( IOException ioE ) {
            error( "Vocabulary Parse Error", ioE.getMessage() );
         }
         final File mrSab = new File( __umlsDirPath + "/META", "MRSAB.RRF" );
         final String mrSabPath = mrSab.getPath();
         final Map<String, String> sourceNames = new HashMap<>();
         final Map<String, String> sourceVersions = new HashMap<>();
         final Map<String, String> sourceCuiCounts = new HashMap<>();
         LOGGER.info( "Parsing vocabulary names from " + mrSabPath );
         try ( final BufferedReader reader = FileUtil.createReader( mrSabPath ) ) {
            int lineCount = 0;
            java.util.List<String> tokens = FileUtil.readBsvTokens( reader, mrSabPath );
            while ( tokens != null ) {
               lineCount++;
               if ( tokens.size() > MrsabIndex.CFR._index ) {
                  final String sab = tokens.get( MrsabIndex.RSAB._index );
                  if ( sources.contains( sab ) ) {
                     sourceNames.put( sab, tokens.get( MrsabIndex.SON._index ) );
                     sourceVersions.put( sab, tokens.get( MrsabIndex.SVER._index ) );
                     sourceCuiCounts.put( sab, tokens.get( MrsabIndex.CFR._index ) );
                  }
               }
               if ( lineCount % 100000 == 0 ) {
                  LOGGER.info( "File Line " + lineCount + "\t Vocabularies " + sources.size() );
               }
               tokens = FileUtil.readBsvTokens( reader, mrConsoPath );
            }
            LOGGER.info( "Parsed " + sources.size() + " vocabulary names" );
            _sourceModel.setSourceInfo( sourceNames, sourceVersions, sourceCuiCounts );
         } catch ( IOException ioE ) {
            error( "Vocabulary Parse Error", ioE.getMessage() );
         }

         DisablerPane.getInstance().setVisible( false );
         frame.setCursor( Cursor.getDefaultCursor() );
      }
   }

   private void buildDictionary( final String dictionaryName ) {
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute( new DictionaryBuildRunner( _umlsDirPath, _ctakesPath, dictionaryName, _sourceModel
            .getWantedSources(),
            _sourceModel.getWantedTargets(), _tuiModel.getWantedTuis(), _languageModel.getWantedLanguages() ) );
   }

   private void error( final String title, final String message ) {
      LOGGER.error( message );
      JOptionPane.showMessageDialog( MainPanel.this, message, title, JOptionPane.ERROR_MESSAGE );
   }


   private class DictionaryBuildRunner implements Runnable {
      private final String __umlsDirPath;
      private final String __ctakesDirPath;
      private final String __dictionaryName;
      private final Collection<String> __wantedSources;
      private final Collection<String> __wantedTargets;
      private final Collection<SemanticTui> __wantedTuis;
      private final Collection<String> __wantedLanguages;

      private DictionaryBuildRunner( final String umlsDirPath,
                                     final String ctakesDirPath,
                                     final String dictionaryName,
                                     final Collection<String> wantedSources,
                                     final Collection<String> wantedTargets,
                                     final Collection<SemanticTui> wantedTuis,
                                     final Collection<String> wantedLangauges ) {
         __umlsDirPath = umlsDirPath;
         __ctakesDirPath = ctakesDirPath;
         __dictionaryName = dictionaryName;
         __wantedSources = wantedSources;
         __wantedTargets = new ArrayList<>( wantedTargets );
         __wantedTuis = new ArrayList<>( wantedTuis );
         __wantedLanguages = new ArrayList<>( wantedLangauges );
      }

      @Override
      public void run() {
         SwingUtilities.getRoot( MainPanel.this ).setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
         DisablerPane.getInstance().setVisible( true );
         if ( DictionaryBuilder.buildDictionary( __umlsDirPath, __ctakesDirPath, __dictionaryName,
//               Collections.singletonList( "ENG" ),
               __wantedLanguages,
               __wantedSources, __wantedTargets, __wantedTuis ) ) {
            final String message = "Dictionary " + __dictionaryName + " successfully built in " + __ctakesDirPath
                  + ",  " + CTAKES_APP_DB_PATH;
            LOGGER.info( message );
            JOptionPane
                  .showMessageDialog( MainPanel.this, message, "Dictionary Built", JOptionPane.INFORMATION_MESSAGE );
         } else {
            error( "Build Failure", "Dictionary " + __dictionaryName + " could not be built in " + __ctakesDirPath );
         }
         DisablerPane.getInstance().setVisible( false );
         SwingUtilities.getRoot( MainPanel.this ).setCursor( Cursor.getDefaultCursor() );
      }
   }


   private class UmlsDirListener implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         final String oldPath = _umlsDirPath;
         final String newPath = setUmlsDirPath( event.getActionCommand() );
         if ( !oldPath.equals( newPath ) ) {
            loadSources();
         }
      }
   }


   private class CtakesDirListener implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         _ctakesPath = event.getActionCommand();
      }
   }


   /**
    * Builds the dictionary
    */
   private class BuildDictionaryAction extends AbstractAction {
      private final JTextComponent __textComponent;

      private BuildDictionaryAction( final JTextComponent textComponent ) {
         super( "Build Dictionary" );
         __textComponent = textComponent;
      }

      @Override
      public void actionPerformed( final ActionEvent event ) {
         if ( _sourceModel.getRowCount() == 0 ) {
            error( "UMLS not yet loaded", "Please specify a UMLS installation." );
            return;
         }
         if ( _sourceModel.getWantedSources().isEmpty() ) {
            error( "Vocabularies not selected", "Please specify one or more source vocabularies." );
            return;
         }
         if ( _languageModel.getWantedLanguages().isEmpty() ) {
            error( "Language not selected", "Please specify one or more languages." );
            return;
         }
         final String dictionaryName = __textComponent.getText();
         if ( dictionaryName != null && !dictionaryName.isEmpty() ) {
            buildDictionary( dictionaryName.toLowerCase() );
         } else {
            error( "Invalid Dictionary Name", "Please Specify a Dictionary Name" );
         }
      }
   }


}
