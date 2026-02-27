package org.apache.ctakes.core.cr;

import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.ProgressManager;
import org.apache.ctakes.core.util.AeParamUtil;
import org.apache.ctakes.core.util.BannerWriter;
import org.apache.ctakes.core.util.NumberedSuffixComparator;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author SPF , chip-nlp
 * @since {1/23/2024}
 */
@PipeBitInfo (
      name = "ZipFileTreeReader",
      description = "Reads zipped files in a directory tree.",
      role = PipeBitInfo.Role.READER
)
public class ZipFileTreeReader extends AbstractFileTreeReader {
   static private final Logger LOGGER = LoggerFactory.getLogger( "ZipFileTreeReader" );

   private final ZoneId ZONE_ID = ZoneId.systemDefault();


   // TODO add standard parameter names and descriptions to the list that contains "InputDir" and "OutputDir"
   // PatientList good for readers, writers, trainers, evals.    DateFormat for readers, writers, aes, utils, etc.
   // Add some standard value options? JSON,XML,TXT  BSV,CSV,TAB  HTML
   // TODO get rid of all-caps parameter names.


   static public final String PATIENT_LIST_PARAM = "PatientList";
   static public final String PATIENT_LIST_DESC = "Set a path to a list of desired patient IDs.";
   @ConfigurationParameter (
         name = PATIENT_LIST_PARAM,
         description = PATIENT_LIST_DESC,
         mandatory = false
   )
   private String _patientListPath;

//   static public final String DATE_FORMAT_PARAM = "DateFormat";
//   static public final String DATE_FORMAT_DESC = "A format to parse dates.  e.g. dd-MM-yyyy_HH:mm:ss";
//   @ConfigurationParameter (
//         name = DATE_FORMAT_PARAM,
//         description = DATE_FORMAT_DESC,
//         defaultValue = "yyyyMMdd",
//         mandatory = false
//   )
//   private String _dateFormat;
//
//   private DateTimeFormatter _dateFormatter;

   static public final String CAS_DATE_FORMAT_PARAM = "CasDateFormat";
   static public final String CAS_DATE_FORMAT_DESC = "Set a value for parameter CasDateFormat.";
   @ConfigurationParameter (
         name = CAS_DATE_FORMAT_PARAM,
         description = CAS_DATE_FORMAT_DESC,
         defaultValue = "MMddyyyykkmmss",
         mandatory = false
   )
   private String _casDateFormat;

   private DateTimeFormatter _casDateFormatter;


   private boolean _writeBanner_ = false;

   private List<String> _entryPaths = new ArrayList<>();
   private int _currentEntryIndex = 0;

   private int _zipPatientLevel = 0;

   private final Collection<String> _patientList = new ArrayList<>();
   private ZipFile _zipFile;

   /**
    * Does nothing.  Use readEntry(..)
    */
   @Override
   protected void readFile( final JCas jCas, final File file ) throws IOException {
      String docText = readFile( file );
      docText = handleQuotedDoc( docText );
      docText = handleTextEol( docText );
      jCas.setDocumentText( docText );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
//      _dateFormatter = DateTimeFormatter.ofPattern( _dateFormat );
      _casDateFormatter = DateTimeFormatter.ofPattern( _casDateFormat );
      // TODO - for zipPatientLevel in the zip getPatient just get the dir at that level.
      final Object zipPatientLevel = context.getConfigParameterValue( PATIENT_LEVEL );
      if ( zipPatientLevel != null ) {
         if ( zipPatientLevel instanceof Integer ) {
            _zipPatientLevel = (int)zipPatientLevel;
         }
         else {
            _zipPatientLevel = AeParamUtil.parseInt( zipPatientLevel.toString() );
         }
      }
      final Object writeBannerChoice = context.getConfigParameterValue( PARAM_WRITE_BANNER );
      if ( writeBannerChoice != null ) {
         _writeBanner_ = AeParamUtil.isTrue( writeBannerChoice.toString() );
      }
      if ( _patientListPath != null && !_patientListPath.isEmpty() ) {
         _patientList.addAll( readPatientList( _patientListPath ) );
      }
      // The super.initialize() will set up information for the zip files.
      // We will be ignoring most of what it sets up.

      // Get rid of patients named after source subdirectories.
      final Collection<String> subDirPatients = PatientNoteStore.getInstance().getStoredPatientIds();
      subDirPatients.forEach( PatientNoteStore.getInstance()::removePatient );
   }

   // TODO move to super class
   protected boolean writeBanner() {
      return _writeBanner_;
   }

   protected int getCurrentEntryIndex() {
      return _currentEntryIndex;
   }

   protected void setCurrentEntryIndex( final int index ) {
      _currentEntryIndex = index;
   }

   protected List<String> getEntryPaths() {
      return _entryPaths;
   }

   /**
    *
    * @return the actual index of the zip file.  This is 1 less than the super getCurrentIndex()
    */
   protected int getZipFileIndex() {
      return super.getCurrentIndex()-1;
   }

   protected Collection<String> readPatientList( final String patientListPath )
         throws ResourceInitializationException {
      LOGGER.info( "Reading Patient List {} ...", _patientListPath );
      final Collection<String> patientList = new HashSet<>();
      try (BufferedReader reader = new BufferedReader( new FileReader( patientListPath ) ) ) {
         String patient = reader.readLine();
         while ( patient != null ) {
            patient = patient.trim();
            if ( !patient.isEmpty() ) {
               patientList.add( patient );
            }
            patient = reader.readLine();
         }
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
      LOGGER.info( "{} patients.", patientList.size() );
      return patientList;
   }

   protected boolean hasWantedPatient( final ZipEntry entry ) {
      return isWantedPatient( getPatientId( entry ) );
   }

   // TODO move to super class
   protected  boolean isWantedPatient( final String patient ) {
      return _patientList == null || _patientList.isEmpty() || _patientList.contains( patient.trim() );
   }

   // TODO move to super class
   protected int getPatientCount() {
      return _patientList.size();
   }

   protected String getPatientId( final ZipEntry entry ) {
      return getPatientId( new File( entry.getName() ) );
   }

   protected String getPatientId( final File file ) {
      final File parentFile = file.getParentFile();
      if ( !parentFile.getPath().isEmpty() ) {
         return parentFile.getName();
      }
      return SourceMetadataUtil.UNKNOWN_PATIENT;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public boolean hasNext() {
      // The zip file index
      final boolean hasNextZipFile = super.getCurrentIndex() < super.getFiles().size();
      final boolean hasNextEntryPath = getCurrentEntryIndex() < getEntryPaths().size();
      final boolean hasNext = hasNextZipFile || hasNextEntryPath;
      if ( !hasNext ) {
         ProgressManager.getInstance()
                        .updatePatientId( ProgressManager.PROGRESS_COMPLETE );
         ProgressManager.getInstance()
                        .updateDocId( ProgressManager.PROGRESS_COMPLETE );
         ProgressManager.getInstance()
                        .updateProgress( getEntryPaths().size() );
         if ( writeBanner() ) {
            BannerWriter.writeFinished();
         }
         return false;
      }
      if ( !hasNextEntryPath ) {
         if ( super.getCurrentIndex() == 0 && writeBanner() ) {
            BannerWriter.writeProcess();
         }
         // No more zip entries.  Read next zip file.
         super.setCurrentIndex( super.getCurrentIndex() + 1 );
         final File file = getFile();
         try {
            final boolean haveZipInfo = buildZipInfo( file );
            if ( haveZipInfo ) {
               return true;
            }
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
            return false;
         }
         return hasNext();
      }
      // have another zip entry.
      return true;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void getNext( final JCas jcas ) throws IOException, CollectionException {
      final File file = getFile();
      // Add document metadata based upon file path
      final JCasBuilder builder = getJCasBuilder( file );
      if ( builder == null ) {
         final String entryPath = getCurrentEntryPath();
         throw new IOException( "Could not create metadata for " + entryPath
               + " in zip file " + file.getAbsolutePath() );
      }
      builder.populate( jcas );
      ProgressManager.getInstance()
                     .updatePatientId( SourceMetadataUtil.getPatientIdentifier( jcas ) );
      ProgressManager.getInstance()
                     .updateDocId( DocIdUtil.getDocumentID( jcas ) );
      ProgressManager.getInstance()
                     .updateProgress( getCurrentEntryIndex() );
      readFile( jcas, file );
      _currentEntryIndex++;
   }

   /**
    * Closes last zip file.
    * {@inheritDoc}
    */
   @Override
   public void close() throws IOException {
      closeZipFile();
      super.close();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Progress[] getProgress() {
      return new Progress[]{
            new ProgressImpl( _currentEntryIndex, _entryPaths.size(), Progress.ENTITIES )
      };
   }


//   protected boolean buildZipInfo( final File zipFile ) {
//      LOGGER.info( "Inspecting zip file " + zipFile.getAbsolutePath() + " ..." );
//      _entryPaths = new ArrayList<>();
//      int wantedCount = 0;
//      int entryCount = 0;
//      try ( DotLogger dotter = new DotLogger() ) {
//         ZipInputStream zipStream = new ZipInputStream( new BufferedInputStream( new FileInputStream( zipFile ) ) );
//         ZipEntry zipEntry = zipStream.getNextEntry();
//         while ( zipEntry != null ) {
//            entryCount++;
//            if ( zipEntry.isDirectory() || !hasWantedPatient( zipEntry ) ) {
//               zipStream.closeEntry();
//               zipEntry = zipStream.getNextEntry();
//               continue;
//            }
//            wantedCount++;
//            if ( wantedCount % 1000 == 0 ) {
//               LOGGER.info( "Entries: " + entryCount + " Wanted: " + wantedCount + " so far ...");
//            }
//            // ZipEntry.getName() returns the entire file path.
//            _entryPaths.add( zipEntry.getName() );
//            zipEntry = zipStream.getNextEntry();
//         }
//         zipStream.closeEntry();
//         zipStream.close();
//      } catch ( IOException ioE ) {
//         LOGGER.error( ioE.getMessage() );
//         ioE.printStackTrace();
//         return false;
//      }
//      if ( _entryPaths.isEmpty() ) {
//         LOGGER.info( "No desired entries in " + zipFile.getAbsolutePath() );
//         return false;
//      }
//      LOGGER.info( "Entries: " + entryCount + " Wanted: " + wantedCount );
//      final Map<String,Integer> patientCounts = new HashMap<>();
//      // Sorting all the file paths will sort the patient name and the filenames at once.
//      _entryPaths.sort( new NumberedSuffixComparator() );
//      for ( String path : _entryPaths ) {
//         final File file = new File( path );
//         final String patient = getPatientId( file );
//         final int count = patientCounts.getOrDefault( patient, 0 );
//         patientCounts.put( patient, (count+1) );
//      }
//      // Set information in PatientNoteStore and ProgressManager.
//      patientCounts.forEach( (k,v) -> PatientNoteStore.getInstance().setWantedDocCount(k, v) );
//      ProgressManager.getInstance().initializeProgress( zipFile.getPath(), _entryPaths.size() );
//      _currentEntryIndex = 0;
//      LOGGER.info( patientCounts.keySet().size() + " desired patients and "
//              + _entryPaths.size() + " desired files in " + zipFile.getAbsolutePath() );
//      return true;
//   }

   protected boolean buildZipInfo( final File file ) throws IOException {
      LOGGER.info( "Inspecting zip file {} ...", file.getAbsolutePath() );
      _entryPaths = new ArrayList<>();
      int wantedCount = 0;
      int entryCount = 0;
      // Close the current zip file.
      closeZipFile();
      final Enumeration<? extends ZipEntry> entries;
      try ( DotLogger dotter = new DotLogger() ) {
         _zipFile = new ZipFile( file );
         entries = _zipFile.entries();
      }
      while ( entries.hasMoreElements() ) {
         final ZipEntry zipEntry = entries.nextElement();
         entryCount++;
         if ( zipEntry.isDirectory() || !hasWantedPatient( zipEntry ) ) {
            continue;
         }
         wantedCount++;
         if ( entryCount % 1000 == 0 ) {
            LOGGER.info( "Zip Entries: {} Valid Files: {} ...", entryCount, wantedCount );
         }
         // ZipEntry.getName() returns the entire file path.
         _entryPaths.add( zipEntry.getName() );
      }
      if ( _entryPaths.isEmpty() ) {
         LOGGER.info( "No desired entries in {}", file.getAbsolutePath() );
         return false;
      }
      LOGGER.info( "Zip Entries: {} Valid Files: {}", entryCount, wantedCount );
      final Map<String,Integer> patientCounts = new HashMap<>();
      // Sorting all the file paths will sort the patient name and the filenames at once.
      _entryPaths.sort( new NumberedSuffixComparator() );
      for ( String path : _entryPaths ) {
         final File entryFile = new File( path );
         final String patient = getPatientId( entryFile );
         final int count = patientCounts.getOrDefault( patient, 0 );
         patientCounts.put( patient, (count+1) );
      }
      // Set information in PatientNoteStore and ProgressManager.
      patientCounts.forEach( (k,v) -> PatientNoteStore.getInstance().setWantedDocCount(k, v) );
      ProgressManager.getInstance().initializeProgress( file.getPath(), _entryPaths.size() );
      _currentEntryIndex = 0;
      LOGGER.info( "Valid Patients: {} in {}", patientCounts.keySet().size(), file.getAbsolutePath() );
      return true;
   }


   protected ZipFile getZipFile() {
      return _zipFile;
   }

   protected void closeZipFile() throws IOException {
      if ( _zipFile != null ) {
         _zipFile.close();
      }
   }

   // TODO move to super class
   protected File getFile() {
      return super.getFiles().get( getZipFileIndex() );
   }

   protected ZipEntry getZipEntry( final String entryPath ) throws IOException {
      return getZipFile().getEntry( entryPath );
   }

   protected String getCurrentEntryPath() {
      return getEntryPaths().get( getCurrentEntryIndex() );
   }

   // TODO move to super class
   protected Collection<String> getValidEntryExtensions() {
      return super.getValidExtensions();
   }

   protected Collection<String> getValidExtensions() {
      return Collections.singletonList( "zip" );
   }

   // TODO add throws IOException
   protected JCasBuilder getJCasBuilder( final File file ) {
      final String entryPath = getCurrentEntryPath();
      try {
         final ZipEntry entry = getZipEntry( entryPath );
         final File entryFile = new File( entry.getName() );
         final String entryName = entryFile.getName();
         final String id = createDocumentID( entryName, getValidEntryExtensions() );
         final String idPrefix = createDocumentIdPrefix( entryFile, getRootDir() );
         final String docType = createDocumentType( id );
         final String docTime = createDocumentTime( entry );
         final String patientId = getPatientId( entry );
         return new JCasBuilder()
               .setCorpusName( getCorpusName() )
               .setCorpusPatientCount( getPatientCount() )
               .setCorpusDocCount( getNoteCount() )
               .setPatientId( patientId )
               .setPatientDocCount( getNoteCount( patientId ) )
               .setDocId( id )
               .setDocIdPrefix( idPrefix )
               .setDocType( docType )
               .setDocTime( docTime )
               .setDocPath( entryPath )
               .nullDocText();
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return null;
      }
   }

   /**
    * Reads file using a Path and stream.
    *
    * @param file file to read
    * @return text in file
    * @throws IOException if the file could not be read
    */
   // TODO move to super class
   public String readFile( final File file ) throws IOException {
      final String entryPath = getCurrentEntryPath();
      LOGGER.info( "Reading {} in {} ...", entryPath, file.getAbsolutePath() );
      final ZipFile zipFile = getZipFile();
      final ZipEntry entry = zipFile.getEntry( entryPath );
      final String encoding = getValidEncoding();
      // Use 8KB as the default buffer size
      byte[] buffer = new byte[ 8192 ];
      final StringBuilder sb = new StringBuilder();
      try ( final InputStream inputStream = new BufferedInputStream( zipFile.getInputStream( entry ), buffer.length );
            DotLogger dotter = new DotLogger() ) {
         while ( true ) {
            final int length = inputStream.read( buffer );
            if ( length < 0 ) {
               break;
            }
            if ( encoding != null && !encoding.isEmpty() && !UNKNOWN.equals( encoding ) ) {
               sb.append( new String( buffer, 0, length, encoding ) );
            } else {
               sb.append( new String( buffer, 0, length ) );
            }
         }
      }
      return sb.toString();
   }


   /**
    * @param fileName            -
    * @param validExtensions -
    * @return the file name with the longest valid extension removed
    */
   // TODO move to super class
   protected String createDocumentID( final String fileName, final Collection<String> validExtensions ) {
      String maxExtension = "";
      for ( String extension : validExtensions ) {
         if ( fileName.endsWith( extension ) && extension.length() > maxExtension.length() ) {
            maxExtension = extension;
         }
      }
      int lastDot = fileName.lastIndexOf( '.' );
      if ( !maxExtension.isEmpty() ) {
         lastDot = fileName.length() - maxExtension.length();
      }
      if ( lastDot < 0 ) {
         return fileName;
      }
      return fileName.substring( 0, lastDot );
   }


   /**
    * @param file    -
    * @param rootDir -
    * @return the subdirectory path between the root directory and the file
    */
   protected String createDocumentIdPrefix( final File file, final File rootDir ) {
      return file.getParent();
   }


   /**
    * @param entry -
    * @return the file's last modification date as a string : {@link #getDateFormat()}
    */
   protected String createDocumentTime( final ZipEntry entry ) {
      return createDocumentTime( entry.getTime() );
   }

   // TODO move to abstract reader

   /**
    * {@inheritDoc}
    */
   @Override
   public DateFormat getDateFormat() {
//      return new SimpleDateFormat( _dateFormat );
      return new SimpleDateFormat( _casDateFormat );
   }


   protected String createDocumentTime( final LocalDate date ) {
      return _casDateFormatter.format( date );
   }

   protected String createDocumentTime( final LocalDateTime dateTime ) {
      return _casDateFormatter.format( dateTime );
   }

   /**
    * @param millis -
    * @return the file's last modification date as a string : {@link #getDateFormat()}
    */
   // TODO move to super class
   protected String createDocumentTime( final long millis ) {
//      _dateFormatter = DateTimeFormatter.ofPattern( _dateFormat );
//      return _dateFormatter.format(
      return _casDateFormatter.format(
            LocalDateTime.ofInstant( Instant.ofEpochMilli( millis ), ZONE_ID ) );
   }



}