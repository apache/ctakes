package org.apache.ctakes.core.util.external;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.UimaContext;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author SPF , chip-nlp
 * @since {5/4/2022}
 */
final public class SystemUtil {


   static private final Logger LOGGER = LogManager.getLogger( "SystemUtil" );


   private SystemUtil() {
   }

   static private final String ENV_VAR_PREFIX = "ctakes.env.";
   static public final File NO_FILE = new File( "" );
   static public final String FILE_NOT_FOUND = "FILE_NOT_FOUND";


   static public String subVariableParameters( final String command, final UimaContext context ) {
      if ( context == null || command == null || !command.contains( "$" ) ) {
         return command;
      }
      final StringBuilder sb = new StringBuilder();
      int previousVarEnd = 0;
      int varBegin = command.indexOf( '$', previousVarEnd );
      while ( varBegin > -1 ) {
         // copy everything from the command from the end of the previous variable to the beginning of this variable.
         sb.append( command, previousVarEnd, varBegin );
         // get the end of the current variable name.
         int varEnd = command.indexOf( ' ', varBegin );
         varEnd = varEnd < 0 ? command.length() : varEnd;
         // get the name of the variable (without $).
         final String varName = command.substring( varBegin + 1, varEnd );
         // get the value from the uima context.
         final Object value = context.getConfigParameterValue( varName );
         if ( value == null ) {
            LOGGER.warn( "No value for $" + varName + " in known parameter values." );
            sb.append( "$" )
              .append( varName );
         } else {
            LOGGER.info( "Substituting Parameter Value " + value + " for $" + varName );
            sb.append( value );
         }
         if ( varEnd == command.length() ) {
            break;
         }
         sb.append( ' ' );
         previousVarEnd = varEnd;
         varBegin = command.indexOf( '$', varEnd );
         if ( varBegin < 0 ) {
            sb.append( command, varEnd, command.length() );
         }
      }
      return sb.toString();
   }


   /**
    * Add key value pairs to a set of environment variables for use in external processes.
    *
    * @param variables ket value pairs
    */
   static public void addEnvironmentVariables( final Object... variables ) {
      if ( variables.length == 0 ) {
         LOGGER.warn( "No variables specified." );
         return;
      }
      if ( variables.length % 2 != 0 ) {
         LOGGER.error( "Odd number of variables provided.  Should be key value pairs." );
         return;
      }
      for ( int i = 0; i < variables.length; i += 2 ) {
         if ( variables[ i ] instanceof String ) {
            System.setProperty( ENV_VAR_PREFIX + variables[ i ], variables[ i + 1 ].toString() );
         } else {
            LOGGER.warn( "Variable" + i + " not a String, using " + variables[ i ].toString() );
            System.setProperty( ENV_VAR_PREFIX + variables[ i ].toString(), variables[ i + 1 ].toString() );
         }
      }
   }


   static public boolean copyDir( final String source, final String target ) {
      final File sourceDir = new File( source );
      if ( !sourceDir.isDirectory() ) {
         LOGGER.error( "Source is not a directory " + source );
         return false;
      }
      final File[] files = sourceDir.listFiles();
      if ( files == null ) {
         LOGGER.error( "Cannot list files in " + source );
         return false;
      }
      new File( target ).mkdirs();
      Arrays.stream( files )
            .filter( f -> !f.isDirectory() )
            .sorted()
            .map( File::getName )
            .forEach( n -> copyFile( source + "/" + n, target + "/" + n  ) );
      Arrays.stream( files )
            .filter( File::isDirectory )
            .sorted()
            .map( File::getName )
            .forEach( n -> copyDir( source + "/" + n, target + "/" + n  ) );
      return true;
   }


   static public boolean copyFile( final String source, final String target ) {
      final InputStream sourceStream = FileLocator.getStreamQuiet( source );
      if ( sourceStream == null ) {
         LOGGER.error( "Cannot access source " + source );
         return false;
      }
//      final File targetFile = FileLocator.getFileQuiet( target );
//      if ( targetFile == null ) {
//         LOGGER.error( "Cannot access target file " + target );
//         return false;
//      }
      Path targetPath;
      try {
         targetPath = Paths.get( target );
      } catch ( InvalidPathException ipE ) {
         LOGGER.error( "Cannot access target path " + target );
         return false;
      }
      return SystemUtil.copyToDisk( sourceStream, targetPath );
   }

   static public boolean copyToDisk( final InputStream source, final Path target ) {
      try {
         Files.copy( source, target, StandardCopyOption.REPLACE_EXISTING );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
         return false;
      }
      return true;
   }

   static public String findExecutable( final String name ) {
      final String executable = findExecutableInCtakes( name );
      if ( !FILE_NOT_FOUND.equals( executable ) ) {
         return executable;
      }
      return findExecutableOnPath( name );
   }

   static public String findExecutableInCtakes( final String name ) {
      final File executable = FileLocator.getFileQuiet( name );
      if ( executable != null ) {
         if ( executable.canExecute() ) {
            return executable.getAbsolutePath();
         }
      }
      return FILE_NOT_FOUND;
   }

   static public String findExecutableOnPath( final String name ) {
      for ( String dirname : System.getenv( "PATH" )
                                   .split( File.pathSeparator ) ) {
         final File testFile = new File( dirname, name );
         if ( testFile.isFile() && testFile.canExecute() ) {
            return testFile.getAbsolutePath();
         }
      }
      return FILE_NOT_FOUND;
   }


   static public class FileDownloader implements Callable<File> {

      private final String _url;
      private final String _tempPrefix;
      private final String _tempSuffix;

      public FileDownloader( final String url ) {
         this( url, "Prefix", "suffix" );
      }

      public FileDownloader( final String url, final String tempPrefix, final String tempSuffix ) {
         _url = url;
         _tempPrefix = tempPrefix;
         _tempSuffix = tempSuffix;
      }

      public File call() throws IOException {
         final File tempZip = File.createTempFile( _tempPrefix, _tempSuffix );
         tempZip.deleteOnExit();
         URL url = new URL( _url );
         try ( ReadableByteChannel readableByteChannel = Channels.newChannel( url.openStream() );
               FileOutputStream fileOutputStream = new FileOutputStream( tempZip );
               FileChannel fileChannel = fileOutputStream.getChannel() ) {
            fileChannel.transferFrom( readableByteChannel, 0, Long.MAX_VALUE );
         }
         return tempZip;
      }

   }


   static private boolean unzipit( final String zippedFile, final File unzipDir ) throws IOException {
      final InputStream zippedStream = FileLocator.getStreamQuiet( zippedFile );
      if ( zippedStream == null ) {
         LOGGER.error( "Could not access " + zippedFile );
         return false;
      }
      return unzipit( zippedStream, unzipDir );
   }

   static private boolean unzipit( final File zippedFile, final File unzipDir ) throws IOException {
      return unzipit( zippedFile.getPath(), unzipDir );
   }

   static private boolean unzipit( final InputStream zippedStream, final File unzipDir ) throws IOException {
      final byte[] buffer = new byte[ 1024 ];
      final ZipInputStream zis = new ZipInputStream( zippedStream );
      ZipEntry zipEntry = zis.getNextEntry();
      while ( zipEntry != null ) {
         if ( zipEntry.isDirectory() ) {
            final File newUnzipDir = new File( unzipDir, zipEntry.getName() );
            newUnzipDir.mkdirs();
         } else {
            final File newUnzipFile = newUnzipFile( unzipDir, zipEntry );
            final FileOutputStream fos = new FileOutputStream( newUnzipFile );
            int len;
            while ( ( len = zis.read( buffer ) ) > 0 ) {
               fos.write( buffer, 0, len );
            }
            fos.close();
         }
         zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
      zis.close();
      return true;
   }

   static public class FileUnzipper implements Callable<File> {

      private final File _zip;
      private final File _unzipDir;

      public FileUnzipper( final File zip, final File unzipDir ) {
         _zip = zip;
         _unzipDir = unzipDir;
      }

      public File call() throws IOException {
         unzipit( _zip, _unzipDir );
         return _unzipDir;
      }

   }


   static private File newUnzipFile( final File unzipDirPath, final ZipEntry zipEntry ) throws IOException {
      final File unzippedFile = new File( unzipDirPath, zipEntry.getName() );

      final String destDirPath = unzipDirPath.getCanonicalPath();
      final String destFilePath = unzippedFile.getCanonicalPath();

      if ( !destFilePath.startsWith( destDirPath + File.separator ) ) {
         throw new IOException( "Entry is outside of the target dir: " + zipEntry.getName() );
      }
      unzippedFile.getParentFile().mkdirs();
      return unzippedFile;
   }


   static public void openWebPage( final String page ) {
      LOGGER.info( "Opening Web Page " + page + " ..." );
      String command = "start \"Browser\" /max " + page;
      final String os = System.getProperty( "os.name" );
      if ( !os.toLowerCase()
              .contains( "windows" ) ) {
         command = "open " + page
                   + " || xdg-open " + page
                   + " || sensible-browser " + page;
      }
      try {
         SystemUtil.run( command );
      } catch ( IOException e ) {
         LOGGER.error( e.getMessage() );
      }

   }

   static public boolean run( final String command ) throws IOException {
      final CommandRunner runner = new CommandRunner( command );
      return run( runner );
   }

   static public boolean run( final CommandRunner runner ) throws IOException {
      boolean ok = false;
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      try {
         final Future<Boolean> future = executor.submit( runner );
         ok = future.get();
      } catch ( InterruptedException | ExecutionException multE ) {
         throw new IOException( multE );
      }
      return ok;
   }

   static public class CommandRunner implements Callable<Boolean> {

      static private final String LOG_DIR = "temp";
      private final Map<String, String> _userEnvVars = new HashMap<>();
      private final String _command;
      private String _dir;
      private String _outLog;
      private boolean _keepTempLog = false;
      private Logger _logger;
      private boolean _wait;
      private boolean _stopOnExit;
      private InputFeeder _inputFeeder;
      private boolean _setJavaHome = true;
      private String _venv;

      public CommandRunner( final String command ) {
         _command = command;
      }

      public void setDirectory( final String directory ) {
         _dir = directory;
      }

      public void setLogger( final Logger logger ) {
         _logger = logger;
      }

      public void setLogFiles( final String outLog ) {
         _outLog = outLog;
      }

      public void keepTempLog( final boolean keep ) {
         _keepTempLog = keep;
      }

      public void wait( final boolean wait ) {
         _wait = wait;
      }

      public void addEnvVar( final String name, final String value ) {
         _userEnvVars.put( name, value );
      }

      public void stopOnExit( final boolean stopOnExit ) {
         _stopOnExit = stopOnExit;
      }

      public InputFeeder createInputFeeder() {
         _inputFeeder = new InputFeeder();
         return _inputFeeder;
      }

      public void setSetJavaHome( final boolean setJavaHome ) {
         _setJavaHome = setJavaHome;
      }

      public void setVenv( final String venv ) {
         _venv = venv;
      }

      private String getDefaultLogFile() {
         final String ext = String.valueOf( new Random().nextLong() );
         final int spaceIndex = _command.indexOf( ' ' );
         final String cmd = spaceIndex < 0 ? _command : _command.substring( 0, spaceIndex );
         File tempFile;
         try {
            tempFile = File.createTempFile( cmd + ".ctakes.log.", ext );
         } catch ( IOException ioE ) {
            new File( LOG_DIR ).mkdirs();
            tempFile = new File( LOG_DIR, cmd + ".ctakes.log." + ext );
         }
         if ( !_keepTempLog ) {
            tempFile.deleteOnExit();
         }
         return tempFile.getPath();
      }

      private void ensureEnvironment( final ProcessBuilder processBuilder ) {
         final Map<String, String> env = processBuilder.environment();
         // If the user set a variable in a piper file using "env" then add that to the environment.
         System.getProperties()
               .stringPropertyNames()
               .stream()
               .filter( n -> n.startsWith( ENV_VAR_PREFIX ) )
               .forEach( n -> env.put( n.substring( ENV_VAR_PREFIX.length() ), System.getProperty( n ) ) );
         if ( _setJavaHome ) {
            env.put( "JAVA_HOME", System.getProperty( "java.home" ) );
         }
         if ( !env.containsKey( "CTAKES_HOME" ) ) {
            String cTakesHome = System.getenv( "CTAKES_HOME" );
            if ( cTakesHome == null || cTakesHome.isEmpty() ) {
               cTakesHome = System.getProperty( "user.dir" );
            }
            env.put( "CTAKES_HOME", cTakesHome );
         }
         if ( !env.containsKey( "CLASSPATH" ) ) {
            final String classpath = System.getProperty( "java.class.path" );
            if ( classpath != null && !classpath.isEmpty() ) {
               env.put( "CLASSPATH", classpath );
            }
         }
         env.putAll( _userEnvVars );
         if ( _venv != null && !_venv.trim().isEmpty() ) {
            env.put( "VIRTUAL_ENV", _venv );
            final String WinPath = env.get( "Path" );
            if ( WinPath != null ) {
               env.put( "Path", getVenvPath() + WinPath );
            }
            final String UxPath = env.get( "PATH" );
            if ( UxPath != null ) {
               env.put( "PATH", getVenvPath() + UxPath );
            }
         }
      }

      static private final String[] VENV_EXTENSIONS
            = { "Library/mingw-w64/bin", "Library/usr/bin", "Library/bin", "Scripts", "bin", "lib/site-packages" };

      private String getVenvPath() {
         final StringBuilder sb = new StringBuilder( _venv + File.pathSeparator );
         for ( String extension : VENV_EXTENSIONS ) {
            final String envPath = _venv + File.separator + extension.replace( '/', File.separatorChar );
            if ( new File( envPath ).isDirectory() ) {
               sb.append( envPath )
                   .append( File.pathSeparator );
            }
         }
         return sb.toString();
      }

      public Boolean call() throws IOException, InterruptedException {
         String command = _command;
         if ( _logger == null ) {
            if ( _outLog != null && !_outLog.isEmpty() ) {
               final File log = new File( _outLog );
               if ( log.getParentFile() != null ) {
                  log.getParentFile()
                     .mkdirs();
               }
               command += " > " + _outLog + " 2>&1";
            } else {
               command += " > " + getDefaultLogFile() + " 2>&1";
            }
         }
         String cmd = "cmd.exe";
         String cmdOpt = "/c";
         final String os = System.getProperty( "os.name" );
         if ( !os.toLowerCase()
                 .contains( "windows" ) ) {
            cmd = "bash";
            cmdOpt = "-c";
//            if ( !_wait ) {
//               command += " &";
//            }
         }
         final ProcessBuilder processBuilder = new ProcessBuilder( cmd, cmdOpt, command );
         if ( _dir != null && !_dir.isEmpty() ) {
            final File dir = new File( _dir );
            if ( !dir.exists() ) {
               dir.mkdirs();
            }
            processBuilder.directory( dir );
         }
         ensureEnvironment( processBuilder );
         final Process process = processBuilder.start();
         if ( _stopOnExit ) {
            registerShutdownHook( process );
         }
         if ( _logger != null ) {
            final ExecutorService executors = Executors.newFixedThreadPool( 2 );
            executors.submit( new OutputLogger( process, _logger ) );
            executors.submit( new ErrorLogger( process, _logger ) );
         }
         if ( _inputFeeder != null ) {
            _inputFeeder.setProcess( process );
         }
         if ( _wait ) {
            return process.waitFor() == 0;
         }
         return true;
      }

      /**
       * Registers a shutdown hook for the process so that it shuts down when the VM exits.
       * This includes kill signals and user actions like "Ctrl-C".
       */
      private void registerShutdownHook( final Process process ) {
         Runtime.getRuntime()
                .addShutdownHook( new Thread( () -> {
                   try {
                      if ( process.isAlive() ) {
                         process.destroy();
                         // if destroy doesn't work then waitFor may hang this app.
                         process.waitFor();
                      }
                   } catch ( InterruptedException multE ) {
                      LOGGER.error( "Could not stop process.", multE );
                   }
                } ) );
      }

   }


   static private class InputFeeder {

      private OutputStream _input;

      private void setProcess( final Process process ) {
         _input = process.getOutputStream();
      }

      public void feedInput( final String input ) {
         if ( _input == null ) {
            LOGGER.error( "Process not started, cannot send input." );
         }
         try {
            _input.write( input.getBytes() );
         } catch ( IOException ioE ) {
            LOGGER.error( "Could not send input to process. " + ioE.getMessage() );
         }
      }

   }


   static private class OutputLogger implements Runnable {

      final private InputStream _output;
      final private Logger _logger;

      private OutputLogger( final Process process, final Logger logger ) {
         _output = process.getInputStream();
         _logger = logger;
      }

      public void run() {
         try ( BufferedReader reader = new BufferedReader( new InputStreamReader( _output ) ) ) {
            reader.lines()
                  .forEach( _logger::info );
         } catch ( IOException ioE ) {
            _logger.error( ioE.getMessage() );
         }
      }

   }

   static private class ErrorLogger implements Runnable {

      final private InputStream _error;
      final private Logger _logger;

      private ErrorLogger( final Process process, final Logger logger ) {
         _error = process.getErrorStream();
         _logger = logger;
      }

      public void run() {
         try ( BufferedReader reader = new BufferedReader( new InputStreamReader( _error ) ) ) {
            reader.lines()
                  .forEach( _logger::error );
         } catch ( IOException ioE ) {
            _logger.error( ioE.getMessage() );
         }
      }

   }


}
