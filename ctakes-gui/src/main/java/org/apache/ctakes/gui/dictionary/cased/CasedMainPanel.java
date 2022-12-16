package org.apache.ctakes.gui.dictionary.cased;

import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.gui.component.DisablerPane;
import org.apache.ctakes.gui.component.FileChooserPanel;
import org.apache.ctakes.gui.component.LoggerPanel;
import org.apache.ctakes.gui.component.PositionedSplitPane;
import org.apache.ctakes.gui.dictionary.cased.table.SemanticTuiModel;
import org.apache.ctakes.gui.dictionary.cased.table.TextTypeModel;
import org.apache.ctakes.gui.dictionary.cased.term.CuiTerm;
import org.apache.ctakes.gui.dictionary.cased.umls.UmlsParser;
import org.apache.ctakes.gui.dictionary.cased.umls.file.Tty;
import org.apache.ctakes.gui.dictionary.umls.LanguageTableModel;
import org.apache.ctakes.gui.dictionary.umls.MrconsoIndex;
import org.apache.ctakes.gui.dictionary.umls.MrsabIndex;
import org.apache.ctakes.gui.dictionary.umls.SourceTableModel;
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


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/10/2015
 */
final class CasedMainPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "CasedMainPanel" );

   private String _umlsDirPath = System.getProperty( "user.dir" );
   private String _ctakesPath = System.getProperty( "user.dir" );
   private final SemanticTuiModel _tuiModel = new SemanticTuiModel();
   private final SourceTableModel _sourceModel = new SourceTableModel();
   private final TextTypeModel _textTypeModel = new TextTypeModel();
   private final LanguageTableModel _languageModel = new LanguageTableModel();

   CasedMainPanel() {
      super( new BorderLayout() );
      final JComponent sourceDirPanel = new JPanel( new GridLayout( 2, 1 ) );
      sourceDirPanel.add( new FileChooserPanel( "cTAKES Installation:", _ctakesPath, true, new CtakesDirListener() ) );
      sourceDirPanel.add( new FileChooserPanel( "UMLS Installation:", _umlsDirPath, true, new UmlsDirListener() ) );
      add( sourceDirPanel, BorderLayout.NORTH );

      add( createCenterPanel( _sourceModel, _tuiModel, _textTypeModel, _languageModel ), BorderLayout.CENTER );
   }

   private JComponent createCenterPanel( final TableModel sourceModel,
                                         final TableModel tuiModel,
                                         final TableModel textTypeModel,
                                         final TableModel languageModel ) {
      final JTabbedPane tabbedPane = new JTabbedPane();
      tabbedPane.addTab( "Vocabularies", createTable( sourceModel ) );
      tabbedPane.addTab( "Semantic Types", createTable( tuiModel ) );
      tabbedPane.addTab( "Text Types", create50_100Table( textTypeModel ) );
      tabbedPane.addTab( "Languages", createLangTable( languageModel ) );

      final JPanel umlsPanel = new JPanel( new BorderLayout() );
      umlsPanel.add( tabbedPane, BorderLayout.CENTER );
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
      table.getColumnModel().getColumn( 0 ).setMaxWidth( 70 );
      table.getColumnModel().getColumn( 1 ).setMaxWidth( 100 );
      return new JScrollPane( table );
   }

   static private JComponent create50_100Table( final TableModel model ) {
      final JTable table = new JTable( model );
      table.setCellSelectionEnabled( false );
      table.setShowVerticalLines( false );
      table.setAutoCreateRowSorter( true );
      table.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
      table.getColumnModel().getColumn( 0 ).setMaxWidth( 70 );
      table.getColumnModel().getColumn( 1 ).setMaxWidth( 100 );
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
      label.setPreferredSize( new Dimension( 150, 0 ) );
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
         final JFrame frame = (JFrame)SwingUtilities.getRoot( CasedMainPanel.this );
         frame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
         DisablerPane.getInstance().setVisible( true );
         final File mrConso = new File( __umlsDirPath + "/META", "MRCONSO.RRF" );
         final String mrConsoPath = mrConso.getPath();
         LOGGER.info( "Parsing vocabulary types from " + mrConsoPath );
         final Collection<String> sources = new HashSet<>();
         final Collection<String> languages = new HashSet<>();
         final Map<Tty, java.util.List<String>> ttyExamples = new HashMap<>();
         try ( final BufferedReader reader = FileUtil.createReader( mrConsoPath ) ) {
            java.util.List<String> tokens = FileUtil.readBsvTokens( reader, mrConsoPath );
            while ( tokens != null ) {
               if ( tokens.size() > MrconsoIndex.TEXT._index ) {
                  sources.add( tokens.get( MrconsoIndex.SOURCE._index ) );
                  languages.add( tokens.get( MrconsoIndex.LANGUAGE._index ) );
                  makeExample( tokens.get( MrconsoIndex.TERM_TYPE._index ), tokens.get( MrconsoIndex.TEXT._index ),
                               ttyExamples );
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
                     final String oldCounts = sourceCuiCounts.getOrDefault( sab, "" );
                     final String newCounts = tokens.get( MrsabIndex.CFR._index );
                     if ( newCounts.length() > oldCounts.length() ) {
                        sourceVersions.put( sab, tokens.get( MrsabIndex.SVER._index ) );
                        sourceCuiCounts.put( sab, newCounts );
                     }
                  }
               }
               if ( lineCount % 100000 == 0 ) {
                  LOGGER.info( "File Line " + lineCount + "\t Vocabularies " + sources.size() );
               }
               tokens = FileUtil.readBsvTokens( reader, mrConsoPath );
            }
            LOGGER.info( "Parsed " + sources.size() + " vocabulary names" );
            _sourceModel.setSourceInfo( sourceNames, sourceVersions, sourceCuiCounts );
            _textTypeModel.setTypeExamples( ttyExamples );
         } catch ( IOException ioE ) {
            error( "Vocabulary Parse Error", ioE.getMessage() );
         }

         DisablerPane.getInstance()
                     .setVisible( false );
         frame.setCursor( Cursor.getDefaultCursor() );
      }

      private void makeExample( final String tty, final String text,
                                final Map<Tty, java.util.List<String>> ttyExamples ) {
         if ( Math.random() < 0.99d ) {
            return;
         }
         final Tty type = Tty.getType( tty );
         final java.util.List<String> examples = ttyExamples.computeIfAbsent( type, t -> new ArrayList<>() );
         if ( examples.size() < 10 ) {
            examples.add( text );
         }
      }

   }

   private void buildDictionary( final String dictionaryName ) {
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute( new CasedDictionaryBuilder( _umlsDirPath,
            _ctakesPath, dictionaryName,
            _sourceModel.getWantedSources(),
            _sourceModel.getWantedTargets(),
            _tuiModel.getWantedTuis(),
            _textTypeModel.getWantedTypes(),
            _languageModel.getWantedLanguages() ) );
   }

   private void error( final String title, final String message ) {
      LOGGER.error( message );
      JOptionPane.showMessageDialog( CasedMainPanel.this, message, title, JOptionPane.ERROR_MESSAGE );
   }

   private class CasedDictionaryBuilder implements Runnable {
      private final String _consoPath;
      private final String _styPath;
      private final String _rankPath;
      private final String _hsqlPath;
      private final String _dictionaryName;
      private final Collection<String> _wantedVocabularies;
      private final Collection<String> _writtenSchema;
      private final Collection<SemanticTui> _wantedTuis;
      private final Collection<Tty> _wantedTextTypes;
      private final Collection<String> _wantedLanguages;

      public CasedDictionaryBuilder( final String umlsPath,
                                     final String ctakesPath,
                                     final String dictionaryName,
                                     final Collection<String> wantedVocabularies,
                                     final Collection<String> writtenSchema,
                                     final Collection<SemanticTui> wantedTuis,
                                     final Collection<Tty> wantedTermTypes,
                                     final Collection<String> wantedLanguages ) {
         this( umlsPath + "/META/MRCONSO.RRF",
               umlsPath + "/META/MRSTY.RRF",
               umlsPath + "/META/MRRANK.RRF",
               ctakesPath + "/resources/org/apache/ctakes/dictionary/lookup/cased",
               dictionaryName,
               wantedVocabularies,
               writtenSchema,
               wantedTuis,
               wantedTermTypes,
               wantedLanguages );
      }

      public CasedDictionaryBuilder( final String consoPath,
                                     final String styPath,
                                     final String rankPath,
                                     final String hsqlPath,
                                     final String dictionaryName,
                                     final Collection<String> wantedVocabularies,
                                     final Collection<String> writtenSchema,
                                     final Collection<SemanticTui> wantedTuis,
                                     final Collection<Tty> wantedTermTypes,
                                     final Collection<String> wantedLanguages ) {
         _consoPath = consoPath;
         _styPath = styPath;
         _rankPath = rankPath;
         _hsqlPath = hsqlPath;
         _dictionaryName = dictionaryName;
         _wantedVocabularies = wantedVocabularies;
         _writtenSchema = writtenSchema;
         _wantedTuis = wantedTuis;
         _wantedTextTypes = wantedTermTypes;
         _wantedLanguages = wantedLanguages;
      }

      public void run() {
         SwingUtilities.getRoot( CasedMainPanel.this ).setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
         DisablerPane.getInstance().setVisible( true );
         final Collection<CuiTerm> cuiTerms
               = UmlsParser.createCuiTerms( _consoPath,
               _styPath,
               _rankPath,
               _wantedTuis,
               _wantedVocabularies,
               _wantedTextTypes,
               _wantedLanguages,
               _writtenSchema );
         if ( cuiTerms.isEmpty() ) {
            final String message = "No Terms fit your parameters for the dictionary";
            LOGGER.error( message );
            JOptionPane
                  .showMessageDialog( CasedMainPanel.this, message, "Cannot Build Dictionary", JOptionPane.ERROR_MESSAGE );
         } else {
            if ( HsqlWriter.writeHsql( _hsqlPath, _dictionaryName, _writtenSchema, cuiTerms ) ) {
               final String message = "Dictionary " + _dictionaryName + " successfully built in " + _hsqlPath;
               LOGGER.info( message );
               JOptionPane
                     .showMessageDialog( CasedMainPanel.this, message, "Dictionary Built",
                                         JOptionPane.INFORMATION_MESSAGE );
            } else {
               error( "Build Failure", "Dictionary " + _dictionaryName + " could not be built in " + _hsqlPath );
            }
            if ( CasedPiperWriter.writePiper( _hsqlPath, _dictionaryName, _writtenSchema ) ) {
               final String message = "Dictionary Piper " + _dictionaryName + " successfully built in " + _hsqlPath;
               LOGGER.info( message );
               JOptionPane
                     .showMessageDialog( CasedMainPanel.this, message, "Dictionary Piper Built",
                                         JOptionPane.INFORMATION_MESSAGE );
            } else {
               error( "Build Failure", "Dictionary Piper " + _dictionaryName + " could not be built in " + _hsqlPath );
            }
            if ( CasedReadmeWriter.writeReadme( _hsqlPath,
                                                _dictionaryName,
                                                _consoPath,
                                                _styPath,
                                                _rankPath,
                                                _wantedTuis,
                                                _wantedVocabularies,
                                                _wantedTextTypes,
                                                _wantedLanguages,
                                                _writtenSchema ) ) {
               final String message = "Dictionary Readme " + _dictionaryName + " successfully built in " + _hsqlPath;
               LOGGER.info( message );
               JOptionPane
                     .showMessageDialog( CasedMainPanel.this, message, "Dictionary Piper Built",
                                         JOptionPane.INFORMATION_MESSAGE );
            } else {
               error( "Build Failure", "Dictionary Readme " + _dictionaryName + " could not be built in " + _hsqlPath );
            }
         }
         DisablerPane.getInstance().setVisible( false );
         SwingUtilities.getRoot( CasedMainPanel.this ).setCursor( Cursor.getDefaultCursor() );
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
         if ( _textTypeModel.getWantedTypes().isEmpty() ) {
            error( "Text Types not selected", "Please specify one or more source text types." );
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
