package org.apache.ctakes.gui.wizard.util;


import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/10/2019
 */
final public class SystemUtil {


   private SystemUtil() {
   }

   static public final File NO_FILE = new File( "" );
   static public final String FILE_NOT_FOUND = "FILE_NOT_FOUND";


   static public void copyToDisk( final InputStream source, final Path target ) {
      try {
         Files.copy( source, target, StandardCopyOption.REPLACE_EXISTING );
      } catch ( IOException ioE ) {
         DialogUtil.showError( ioE.getMessage() );
      }
   }


   static public String findExecutableOnPath( final String name ) {
      for ( String dirname : System.getenv( "PATH" ).split( File.pathSeparator ) ) {
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


   static private void unzipit( final File zippedFile, final File unzipDir ) throws IOException {
      final byte[] buffer = new byte[ 1024 ];
      final ZipInputStream zis = new ZipInputStream( new FileInputStream( zippedFile ) );
      ZipEntry zipEntry = zis.getNextEntry();
      while ( zipEntry != null ) {
         if ( zipEntry.isDirectory() ) {
            final File newUnzipDir = new File( unzipDir, zipEntry.getName() );
            newUnzipDir.mkdirs();
         } else {
            final File newUnzipFile = newUnzipFile( unzipDir, zipEntry );
            final FileOutputStream fos = new FileOutputStream( newUnzipFile );
            int len;
            while ( (len = zis.read( buffer )) > 0 ) {
               fos.write( buffer, 0, len );
            }
            fos.close();
         }
         zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
      zis.close();
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
      return unzippedFile;
   }


   static private boolean runLocally( final String directory,
                                      final String outLog,
                                      final String errLog,
                                      final String command )
         throws IOException, InterruptedException {
      final String os = System.getProperty( "os.name" );
      if ( os.toLowerCase().contains( "windows" ) ) {
         return runOnWindows( directory, outLog, errLog, command );
      } else {
         return runOnLinux( directory, outLog, errLog, command );
      }
   }

   static private boolean runOnWindows( final String directory,
                                        final String outLog,
                                        final String errLog,
                                        final String command )
         throws IOException, InterruptedException {
      final ProcessBuilder processBuilder = new ProcessBuilder()
            .directory( new File( directory ) )
            .command( "cmd.exe", "/c", command.replace( '/', '\\' ) )
            .redirectOutput( new File( outLog ) )
            .redirectError( new File( errLog ) );
      final Map<String, String> env = processBuilder.environment();
      if ( !env.containsKey( "JAVA_HOME" ) ) {
         env.put( "JAVA_HOME", System.getProperty( "java.home" ) );
      }
      final Process process = processBuilder.start();
      int exitCode = process.waitFor();
      return true;
   }


   static private boolean runOnLinux( final String directory,
                                      final String outLog,
                                      final String errLog,
                                      final String command )
         throws IOException, InterruptedException {
      final ProcessBuilder processBuilder = new ProcessBuilder()
            .directory( new File( directory ) )
            .command( "bash", "-c", command )
            .redirectOutput( new File( outLog ) )
            .redirectError( new File( errLog ) );
      final Process process = processBuilder.start();
      int exitCode = process.waitFor();
      return true;
   }


   static public class CommandRunner implements Callable<Boolean> {
      private final String _dir;
      private final String _outLog;
      private final String _errLog;
      private final String _command;

      public CommandRunner( final String directory,
                            final String outLog,
                            final String errLog,
                            final String command ) {
         _dir = directory;
         _outLog = outLog;
         _errLog = errLog;
         _command = command;
      }

      public Boolean call() throws IOException, InterruptedException {
         return runLocally( _dir, _outLog, _errLog, _command );
      }
   }


}
