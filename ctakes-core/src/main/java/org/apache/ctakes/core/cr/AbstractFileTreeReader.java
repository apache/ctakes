package org.apache.ctakes.core.cr;

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.pipeline.ProgressManager;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.BannerWriter;
import org.apache.ctakes.core.util.NumberedSuffixComparator;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.doc.JCasBuilder;
import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.apache.ctakes.core.util.doc.SourceMetadataUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.impl.CollectionReaderDescription_impl;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterDeclarations_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterSettings_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Abstract to read files in a tree starting in a root directory.
 * By default, filenames are sorted with {@link NumberedSuffixComparator}.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/23/2017
 */
abstract public class AbstractFileTreeReader extends JCasCollectionReader_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "AbstractFileTreeReader" );

   static public final String PARAM_WRITE_BANNER = "WriteBanner";
   @ConfigurationParameter(
         name = PARAM_WRITE_BANNER,
         description = "Write a large banner at each major step of the pipeline.",
         mandatory = false,
         defaultValue = "no"
   )
   private String _writeBannerChoice;

   /**
    * Name of configuration parameter that must be set to the path of
    * a directory containing input files.
    */
   @ConfigurationParameter(
         name = ConfigParameterConstants.PARAM_INPUTDIR,
         description = ConfigParameterConstants.DESC_INPUTDIR
   )
   private String _rootDirPath;

   /**
    * Name of configuration parameter that contains the character encoding used
    * by the input files.  If not specified, the default system encoding will
    * be used.
    */
   static public final String PARAM_ENCODING = "Encoding";
   static public final String UNICODE = "unicode";
   @ConfigurationParameter(
         name = PARAM_ENCODING,
         description = "The character encoding used by the input files.",
//         defaultValue = UNICODE,
         mandatory = false
   )
   private String _encoding;

   /**
    * Name of optional configuration parameter that specifies the extensions
    * of the files that the collection reader will read.  Values for this
    * parameter should not begin with a dot <code>'.'</code>.
    */
   static public final String PARAM_EXTENSIONS = "Extensions";
   @ConfigurationParameter(
         name = PARAM_EXTENSIONS,
         description = "The extensions of the files that the collection reader will read.",
         defaultValue = "*",
         mandatory = false
   )
   private String[] _explicitExtensions;

   /**
    * Name of configuration parameter that must be set to false to remove windows \r characters
    */
   public static final String PARAM_KEEP_CR = "KeepCR";
   @ConfigurationParameter(
         name = PARAM_KEEP_CR,
         description = "Keep windows-format carriage return characters at line endings." +
                       "  This will only keep existing characters, it will not add them.",
         mandatory = false
   )
   private boolean _keepCrChar = true;

   /**
    * Name of configuration parameter that must be set to true to replace windows "\r\n" sequnces with "\n ".
    * Useful if windows Carriage Return characters wreak havoc upon trained models but text offsets must be preserved.
    * This may not play well with components that utilize double-space sequences.
    */
   public static final String CR_TO_SPACE = "CRtoSpace";
   @ConfigurationParameter(
         name = CR_TO_SPACE,
         description = "Change windows-format CR + LF character sequences to LF + <Space>.",
         mandatory = false
   )
   private boolean _crToSpace = false;


   /**
    * The patient id for each note is set using a directory name.
    * By default this is the directory directly under the root directory (PatientLevel=1).
    * This is appropriate for files such as in rootDir=data/, file in data/patientA/Text1.txt
    * It can be set to use directory names at any level below.
    * For instance, using PatientLevel=2 for rootDir=data/, file in data/hospitalX/patientA/Text1.txt
    * In this manner the notes for the same patient from several sites can be properly collated.
    */
   public static final String PATIENT_LEVEL = "PatientLevel";
   @ConfigurationParameter(
         name = PATIENT_LEVEL,
         description = "The level in the directory hierarchy at which patient identifiers exist."
                       + "Default value is 1; directly under root input directory.",
         mandatory = false
   )
   private int _patientLevel = 1;


   /**
    * Some document text, such as that created from csv files, is enclosed in quote characters.
    * These quotes can negatively impact some AEs such as Sectionizers that work on plain text instead of tokens.
    * Spaces are used to maintain character indexes.
    */
   public static final String STRIP_QUOTES = "StripQuotes";
   @ConfigurationParameter(
         name = STRIP_QUOTES,
         description = "Replace document-enclosing quote characters with space characters.",
         mandatory = false
   )
   private boolean _stripQuotes = false;


   static protected final String UNKNOWN = "Unknown";
   //   For compatibility with sql db : Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]
   static private final DateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss" );

   static private final Pattern CR_LF = Pattern.compile( "\\r\\n" );

   private boolean _writeBanner;
   private File _rootDir;
   private Collection<String> _validExtensions;
   private List<File> _files;
   private Map<File, String> _filePatients;
   private Map<String, Integer> _patientDocCounts = new HashMap<>();
   private int _currentIndex;
   private Comparator<File> _fileComparator;


   public AbstractFileTreeReader() {
      setMetaData( createMetaData() );
      // Workaround https://github.com/apache/uima-uimaj/issues/234
      // https://github.com/ClearTK/cleartk/issues/470
      try {
         LOGGER.info( "Creating empty CAS to make certain that the typesystem is initialized ..." );
         CasCreationUtils.createCas();
      } catch ( ResourceInitializationException riE ) {
         LOGGER.error( "Could not create base CAS for initialization.\n" + riE.getMessage() );
         LOGGER.error( Arrays.stream( riE.getStackTrace() )
                             .map( StackTraceElement::toString )
                             .collect( Collectors.joining("\n" ) ) );
      }
   }

   /**
    * @param jCas unpopulated jcas
    * @param file file to be read
    * @throws IOException should anything bad happen
    */
   abstract protected void readFile( final JCas jCas, final File file ) throws IOException;

   /**
    * @return Comparator to sort Files and Directories.  The default Comparator sorts by filename with {@link NumberedSuffixComparator}.
    */
   protected Comparator<File> createFileComparator() {
      return new FileComparator();
   }

   public DateFormat getDateFormat() {
      return DATE_FORMAT;
   }

   /**
    * @return all files in the directory tree.
    */
   protected List<File> getFiles() {
      return _files;
   }

   /**
    * @return the index of the file currently being processed.
    */
   protected int getCurrentIndex() {
      return _currentIndex;
   }

   /**
    * Use with care.
    *
    * @param index of the file currently being processed.
    */
   protected void setCurrentIndex( final int index ) {
      _currentIndex = index;
   }

   /**
    * @return the patientId for that file.  By default this is the name of the directory containing the file.
    */
   protected String getPatientId( final File file ) {
      return _filePatients.getOrDefault( file, SourceMetadataUtil.UNKNOWN_PATIENT );
   }

   /**
    * Gets the total number of documents that will be returned by this
    * collection reader.
    *
    * @return the number of documents in the collection.
    */
   public int getNoteCount() {
      if ( _files == null ) {
         LOGGER.error( "Not yet initialized" );
         return 0;
      }
      return _files.size();
   }

   /**
    * @return the root input directory as a File.
    */
   protected File getRootDir() {
      if ( _rootDir == null ) {
         LOGGER.error( "Not yet initialized" );
         return null;
      }
      return _rootDir;
   }

   /**
    * @return the root input directory path as a String.
    */
   protected String getRootPath() {
      final File rootDir = getRootDir();
      if ( rootDir == null ) {
         LOGGER.error( "Not yet initialized" );
         return "Unknown";
      }
      return rootDir.getAbsolutePath();
   }

   /**
    * @return any specified valid file encodings.  If none are specified then the default is {@link #UNKNOWN}.
    */
   final protected String getValidEncoding() {
      if ( _rootDir == null ) {
         LOGGER.error( "Not yet initialized" );
         return UNKNOWN;
      }
      if ( _encoding == null || _encoding.isEmpty() ) {
         return UNKNOWN;
      }
      return _encoding;
   }

   /**
    * @return any specified valid file extensions.
    */
   protected Collection<String> getValidExtensions() {
      if ( _validExtensions == null ) {
         LOGGER.error( "Not yet initialized" );
         return Collections.emptyList();
      }
      return _validExtensions;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      _writeBanner = _writeBannerChoice.equalsIgnoreCase( "yes" )
                     || _writeBannerChoice.equalsIgnoreCase( "true" );
      if ( _writeBanner ) {
         BannerWriter.writeHello();
      }
      try {
         _rootDir = FileLocator.getFile( _rootDirPath );
      } catch ( FileNotFoundException fnfE ) {
         LOGGER.error( "No Directory found at " + _rootDirPath );
         throw new ResourceInitializationException( fnfE );
      }
      _validExtensions = createValidExtensions( _explicitExtensions );
      _currentIndex = 0;
      if ( _rootDir.isFile() ) {
         // does not check for valid extensions.  With one file just trust the user.
         final String patient = _rootDir.getParentFile().getName();
         _files = Collections.singletonList( _rootDir );
         _filePatients = Collections.singletonMap( _rootDir, patient );
         PatientNoteStore.getInstance().setWantedDocCount( patient, 1 );
      } else {
         // gather all of the files and set the document counts per patient.
         final File[] children = _rootDir.listFiles();
         if ( children == null || children.length == 0 ) {
            _filePatients = Collections.emptyMap();
            _files = Collections.emptyList();
            return;
         }
         if ( Arrays.stream( children ).noneMatch( File::isDirectory ) ) {
            _patientLevel = 0;
         }
         _filePatients = new HashMap<>();
         _fileComparator = createFileComparator();
         _files = getDescendentFiles( _rootDir, _validExtensions, 0 );
         _patientDocCounts.forEach( ( k, v ) -> PatientNoteStore.getInstance().setWantedDocCount( k, v ) );
      }
      ProgressManager.getInstance().initializeProgress( _rootDirPath, _files.size() );
   }

   /**
    * @param explicitExtensions array of file extensions as specified in the uima parameters
    * @return a collection of dot-prefixed extensions or none if {@code explicitExtensions} is null or empty
    */
   static protected Collection<String> createValidExtensions( final String... explicitExtensions ) {
      if ( explicitExtensions == null || explicitExtensions.length == 0 ) {
         return Collections.emptyList();
      }
      if ( explicitExtensions.length == 1
            && (explicitExtensions[ 0 ].equals( "*" ) || explicitExtensions[ 0 ].equals( ".*" )) ) {
         return Collections.emptyList();
      }
      final Collection<String> validExtensions = new ArrayList<>( explicitExtensions.length );
      for ( String extension : explicitExtensions ) {
         if ( extension.startsWith( "." ) ) {
            validExtensions.add( extension );
         } else {
            validExtensions.add( '.' + extension );
         }
      }
      return validExtensions;
   }

   /**
    * @param parentDir       -
    * @param validExtensions collection of valid extensions or empty collection if all extensions are valid
    * @param level           directory level beneath the root directory
    * @return List of files descending from the parent directory
    */
   private List<File> getDescendentFiles( final File parentDir,
                                          final Collection<String> validExtensions,
                                          final int level ) {
      final File[] children = parentDir.listFiles();
      if ( children == null || children.length == 0 ) {
         return Collections.emptyList();
      }
      final List<File> childDirs = new ArrayList<>();
      final List<File> files = new ArrayList<>();
      for ( File child : children ) {
         if ( child.isDirectory() ) {
            childDirs.add( child );
            continue;
         }
         if ( isExtensionValid( child, validExtensions ) && !child.isHidden() ) {
            files.add( child );
         }
      }
      childDirs.sort( _fileComparator );
      files.sort( _fileComparator );
      final List<File> descendentFiles = new ArrayList<>( files );
      for ( File childDir : childDirs ) {
         descendentFiles.addAll( getDescendentFiles( childDir, validExtensions, level + 1 ) );
      }
      if ( level == _patientLevel ) {
         final String patientId = parentDir.getName();
         final int count = _patientDocCounts.getOrDefault( patientId, 0 );
         _patientDocCounts.put( patientId, count + descendentFiles.size() );
         descendentFiles.forEach( f -> _filePatients.put( f, patientId ) );
      }
      return descendentFiles;
   }

   /**
    * @param file            -
    * @param validExtensions -
    * @return true if validExtensions is empty or contains an extension belonging to the given file
    */
   static protected boolean isExtensionValid( final File file, final Collection<String> validExtensions ) {
      if ( validExtensions.isEmpty() ) {
         return true;
      }
      final String fileName = file.getName();
      for ( String extension : validExtensions ) {
         if ( fileName.endsWith( extension ) ) {
            if ( fileName.equals( extension ) ) {
               LOGGER.warn( "File " + file.getPath()
                     + " name exactly matches extension " + extension + " so it will not be read." );
               return false;
            }
            return true;
         }
      }
      return false;
   }

   /**
    * @param file            -
    * @param validExtensions -
    * @return the file name with the longest valid extension removed
    */
   static protected String createDocumentID( final File file, final Collection<String> validExtensions ) {
      final String fileName = file.getName();
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
      final String parentPath = file.getParent();
      final String rootPath = rootDir.getPath();
      if ( parentPath.equals( rootPath ) || !parentPath.startsWith( rootPath ) ) {
         return "";
      }
      return parentPath.substring( rootPath.length() + 1 );
   }

   /**
    * @param documentId -
    * @return the file name with the longest valid extension removed
    */
   protected String createDocumentType( final String documentId ) {
      final int lastScore = documentId.lastIndexOf( '_' );
      if ( lastScore < 0 || lastScore == documentId.length() - 1 ) {
         return NoteSpecs.ID_NAME_CLINICAL_NOTE;
      }
      return documentId.substring( lastScore + 1 );
   }

   /**
    * @param file -
    * @return the file's last modification date as a string : {@link #getDateFormat()}
    */
   protected String createDocumentTime( final File file ) {
      final long millis = file.lastModified();
      return getDateFormat().format( millis );
   }

   final protected boolean isKeepCrChar() {
      return _keepCrChar;
   }

   /**
    * @param text document text
    * @return the document text with end of line characters replaced if needed
    */
   final protected String handleTextEol( final String text ) {
      String docText = text;
      if ( !isKeepCrChar() && !docText.isEmpty() && docText.contains( "\r" ) ) {
         LOGGER.debug( "Removing Carriage-Return characters ..." );
         docText = CR_LF.matcher( docText ).replaceAll( "\n" );
      }
      if ( !docText.isEmpty() && !docText.endsWith( "\n" ) ) {
         // Make sure that we end with a newline
         docText += "\n";
      }
      return docText;
   }

   /**
    * @param text document text
    * @return the document text with document begin and end quote characters replaced with space characters if needed
    */
   final protected String handleQuotedDoc( final String text ) {
      if ( !_stripQuotes || text.isEmpty() ) {
         return text;
      }
      String docText = handleQuotedDoc( text, '\"' );
      return handleQuotedDoc( docText, '\'' );
   }

   /**
    * @param text  document text
    * @param quote quote character to replace with space characters.
    * @return the document text with document begin and end quote characters replaced with space characters if needed
    */
   static private String handleQuotedDoc( final String text, final char quote ) {
      String docText = text.trim();
      final int beginDocQuote = docText.indexOf( quote );
      if ( beginDocQuote != 0 ) {
         return text;
      }
      final int endDocQuote = docText.lastIndexOf( quote );
      if ( endDocQuote != docText.length() - 1 ) {
         return text;
      }
      LOGGER.debug( "Replacing document-enclosing quote characters " + quote + " ..." );
      String unquotedText = text;
      final int beginQuote = text.indexOf( quote );
      if ( beginQuote == 0 ) {
         unquotedText = " " + unquotedText.substring( 1 );
      } else {
         unquotedText = unquotedText.substring( 0, beginQuote ) + " " + unquotedText.substring( beginQuote + 1 );
      }
      final int endQuote = unquotedText.lastIndexOf( quote );
      if ( endQuote == unquotedText.length() - 1 ) {
         unquotedText = unquotedText.substring( 0, unquotedText.length() - 1 ) + " ";
      } else {
         unquotedText = unquotedText.substring( 0, endQuote )
                        + " " + unquotedText.substring( endQuote + 1 );
      }
      return unquotedText;
   }


   protected JCasBuilder getJCasBuilder( final File file ) {
      final String id = createDocumentID( file, getValidExtensions() );
      final String idPrefix = createDocumentIdPrefix( file, getRootDir() );
      final String docType = createDocumentType( id );
      final String docTime = createDocumentTime( file );
      final String patientId = getPatientId( file );
      return new JCasBuilder()
            .setDocId( id )
            .setDocIdPrefix( idPrefix )
            .setDocType( docType )
            .setDocTime( docTime )
            .setPatientId( patientId )
            .setDocPath( file.getAbsolutePath() )
            .nullDocText();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean hasNext() {
      if ( _currentIndex == 0 && _writeBanner ) {
         BannerWriter.writeProcess();
      }
      final boolean hasNext = _currentIndex < _files.size();
      if ( !hasNext ) {
         ProgressManager.getInstance()
                        .updatePatientId( ProgressManager.PROGRESS_COMPLETE );
         ProgressManager.getInstance()
                        .updateDocId( ProgressManager.PROGRESS_COMPLETE );
         ProgressManager.getInstance()
                        .updateProgress( _files.size() );
         if ( _writeBanner ) {
            BannerWriter.writeFinished();
         }
      }
      return hasNext;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void getNext( final JCas jcas ) throws IOException, CollectionException {
      final File file = _files.get( _currentIndex );
      // Add document metadata based upon file path
      getJCasBuilder( file ).populate( jcas );
      ProgressManager.getInstance()
                     .updatePatientId( SourceMetadataUtil.getPatientIdentifier( jcas ) );
      ProgressManager.getInstance()
                     .updateDocId( DocIdUtil.getDocumentID( jcas ) );
      ProgressManager.getInstance()
                     .updateProgress( _currentIndex );
      _currentIndex++;
      readFile( jcas, file );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Progress[] getProgress() {
      return new Progress[]{
            new ProgressImpl( _currentIndex, _files.size(), Progress.ENTITIES )
      };
   }


   /**
    * @return Resource metadata for an abstract reader.  This exists to make uima automation factories happy.
    */
   static private ResourceMetaData createMetaData() {
      final ReaderMetadata metadata = new ReaderMetadata();
      metadata.setUUID( "AFTR" );
      metadata.setName( "AbstractFileTreeReader" );
      metadata.setVersion( "1" );
      metadata.setDescription( "Abstract for reader of files in a directory tree" );
      metadata.setVendor( "ctakes" );
      metadata.setCopyright( "2017" );
      return metadata;
   }

   /**
    * The following is required to prevent errors by automated Descriptor creation.
    */
   static private final class ReaderMetadata extends CollectionReaderDescription_impl implements ResourceMetaData {
      static final long serialVersionUID = 3408359518094534817L;
      private String mUUID;
      private String mName;
      private String mDescription;
      private String mVersion;
      private String mVendor;
      private String mCopyright;
      private ConfigurationParameterDeclarations mConfigurationParameterDeclarations = new ConfigurationParameterDeclarations_impl();
      private ConfigurationParameterSettings mConfigurationParameterSettings = new ConfigurationParameterSettings_impl();
      private static final XmlizationInfo XMLIZATION_INFO = new XmlizationInfo( "resourceMetaData", new PropertyXmlInfo[]{ new PropertyXmlInfo( "name", false ), new PropertyXmlInfo( "description" ), new PropertyXmlInfo( "version" ), new PropertyXmlInfo( "vendor" ), new PropertyXmlInfo( "copyright" ), new PropertyXmlInfo( "configurationParameterDeclarations", (String) null ), new PropertyXmlInfo( "configurationParameterSettings", (String) null ) } );

      public void resolveImports() throws InvalidXMLException {
      }

      public void resolveImports( ResourceManager aResourceManager ) throws InvalidXMLException {
      }

      public String getUUID() {
         return this.mUUID;
      }

      public void setUUID( String aUUID ) {
         this.mUUID = aUUID;
      }

      public String getName() {
         return this.mName;
      }

      public void setName( String aName ) {
         this.mName = aName;
      }

      public String getVersion() {
         return this.mVersion;
      }

      public void setVersion( String aVersion ) {
         this.mVersion = aVersion;
      }

      public String getDescription() {
         return this.mDescription;
      }

      public void setDescription( String aDescription ) {
         this.mDescription = aDescription;
      }

      public String getVendor() {
         return this.mVendor;
      }

      public void setVendor( String aVendor ) {
         this.mVendor = aVendor;
      }

      public String getCopyright() {
         return this.mCopyright;
      }

      public void setCopyright( String aCopyright ) {
         this.mCopyright = aCopyright;
      }

      public ConfigurationParameterSettings getConfigurationParameterSettings() {
         return this.mConfigurationParameterSettings;
      }

      public void setConfigurationParameterSettings( ConfigurationParameterSettings aSettings ) {
         this.mConfigurationParameterSettings = aSettings;
      }

      public ConfigurationParameterDeclarations getConfigurationParameterDeclarations() {
         return this.mConfigurationParameterDeclarations;
      }

      public void setConfigurationParameterDeclarations( ConfigurationParameterDeclarations aDeclarations ) {
         this.mConfigurationParameterDeclarations = aDeclarations;
      }

      public void validateConfigurationParameterSettings() throws ResourceConfigurationException {
         ConfigurationParameterDeclarations cfgParamDecls = this.getConfigurationParameterDeclarations();
         ConfigurationParameterSettings cfgParamSettings = this.getConfigurationParameterSettings();
         NameValuePair[] nvps = cfgParamSettings.getParameterSettings();
         if ( nvps.length > 0 ) {
            this.validateConfigurationParameterSettings( nvps, (String) null, cfgParamDecls );
         } else {
            Map settingsForGroups = cfgParamSettings.getSettingsForGroups();
            Set entrySet = settingsForGroups.entrySet();
            Iterator it = entrySet.iterator();

            while ( it.hasNext() ) {
               Map.Entry entry = (Map.Entry) it.next();
               String groupName = (String) entry.getKey();
               nvps = (NameValuePair[]) entry.getValue();
               if ( nvps != null ) {
                  this.validateConfigurationParameterSettings( nvps, groupName, cfgParamDecls );
               }
            }
         }

      }

      protected void validateConfigurationParameterSettings( NameValuePair[] aNVPs, String aGroupName, ConfigurationParameterDeclarations aParamDecls ) throws ResourceConfigurationException {
         for ( int i = 0; i < aNVPs.length; ++i ) {
            String name = aNVPs[ i ].getName();
            org.apache.uima.resource.metadata.ConfigurationParameter param = aParamDecls.getConfigurationParameter( aGroupName, name );
            if ( param == null ) {
               if ( aGroupName == null ) {
                  throw new ResourceConfigurationException( "nonexistent_parameter", new Object[]{ name, this.getName() } );
               }

               throw new ResourceConfigurationException( "nonexistent_parameter_in_group", new Object[]{ name, aGroupName, this.getName() } );
            }

            this.validateConfigurationParameterDataTypeMatch( param, aNVPs[ i ] );
         }

      }

      protected void validateConfigurationParameterDataTypeMatch( org.apache.uima.resource.metadata.ConfigurationParameter aParam, NameValuePair aNVP ) throws ResourceConfigurationException {
         String paramName = aParam.getName();
         String paramType = aParam.getType();
         Class valClass = aNVP.getValue().getClass();
         if ( aParam.isMultiValued() ) {
            if ( !valClass.isArray() ) {
               throw new ResourceConfigurationException( "array_required", new Object[]{ paramName, this.getName() } );
            }

            valClass = valClass.getComponentType();
            if ( Array.getLength( aNVP.getValue() ) == 0 && valClass.equals( Object.class ) ) {
               aNVP.setValue( Array.newInstance( this.getClassForParameterType( paramType ), 0 ) );
               return;
            }
         }

         if ( valClass != this.getClassForParameterType( paramType ) ) {
            throw new ResourceConfigurationException( "parameter_type_mismatch", new Object[]{ this.getName(), valClass.getName(), paramName, paramType } );
         }
      }

      protected Class getClassForParameterType( String paramType ) {
         return "String".equals( paramType ) ? String.class : ("Boolean".equals( paramType ) ? Boolean.class : ("Integer".equals( paramType ) ? Integer.class : ("Float".equals( paramType ) ? Float.class : null)));
      }
   }

   static private class FileComparator implements Comparator<File> {
      private final Comparator<String> __delegate = new NumberedSuffixComparator();

      public int compare( final File file1, final File file2 ) {
         return __delegate.compare( file1.getName(), file2.getName() );
      }
   }


}
