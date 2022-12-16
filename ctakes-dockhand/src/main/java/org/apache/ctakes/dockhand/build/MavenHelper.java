package org.apache.ctakes.dockhand.build;

import org.apache.ctakes.gui.wizard.util.DialogUtil;
import org.apache.ctakes.gui.wizard.util.RunnerUtil;

import java.io.File;

import static org.apache.ctakes.gui.wizard.util.SystemUtil.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/3/2019
 */
public enum MavenHelper {
   INSTANCE;

   static public MavenHelper getInstance() {
      return INSTANCE;
   }

   static private final String MAVEN_TITLE = "Apache Maven";

   static private final String MAVEN_URL
         = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.3.1/apache-maven-3.3.1-bin.zip";


   private File _mavenDir;

   public File getMavenDir() {
      if ( _mavenDir != null ) {
         final String mavenDirPath = _mavenDir.getAbsolutePath();
         if ( !mavenDirPath.endsWith( File.pathSeparator + "bin" + File.pathSeparator ) ) {
            return new File( _mavenDir, "/bin" );
         }
         return _mavenDir;
      }
      return NO_FILE;
   }


//   public void installMaven() {
//      if ( !getMavenPath().equals( FILE_NOT_FOUND ) ) {
//         return;
//      }
//      DialogUtil.initInstall();
//      final boolean doInstall = DialogUtil.chooseSecondaryInstall( "Apache Maven" );
//      if ( !doInstall ) {
//         DialogUtil.showInstallCanceled( "Apache Maven" );
//         return;
//      }
//      final File mavenInstallDir = MavenHelper.getInstance().chooseMavenDir();
//      if ( mavenInstallDir.equals( NO_FILE ) ) {
//         DialogUtil.showInstallCanceled( "Apache Maven" );
//         return;
//      }
//      // TODO Progress Dialog.
//      try {
//         final File mavenZip = SystemUtil.downloadZipFile( MAVEN_URL );
//         unzipit( mavenZip, mavenInstallDir );
//      } catch ( IOException ioE ) {
//         DialogUtil.showError( ioE.getMessage() );
//         return;
//      }
//      _mavenDir = mavenInstallDir;
//      DialogUtil.showInstallComplete( "Apache Maven", mavenInstallDir.getAbsolutePath() );
//   }

   public boolean installMaven() {
      if ( (_mavenDir != null && _mavenDir.isDirectory()) || !getMavenPath().equals( FILE_NOT_FOUND ) ) {
         return true;
      }
      // TODO : Make this a 3 option dialog:  "Maven Not Found.  Want to 1) Select existing 2) Install maven 3) cancel
      final boolean doInstall = DialogUtil.chooseSecondaryInstall( MAVEN_TITLE );
      if ( !doInstall ) {
         final File mavenDir
               = DialogUtil.chooseExistingInstall( "Choose Existing " + MAVEN_TITLE + " Installation Directory" );
         if ( mavenDir == null ) {
            DialogUtil.showCanceledDialog( MAVEN_TITLE );
            return false;
         }
         _mavenDir = mavenDir;
         return true;
      }
      final File mavenInstallDir = MavenHelper.getInstance().chooseMavenDir();
      if ( mavenInstallDir.equals( NO_FILE ) ) {
         DialogUtil.showCanceledDialog( MAVEN_TITLE );
         return false;
      }
      try {
         final FileDownloader downloader = new FileDownloader( MAVEN_URL );
         final File mavenZip = RunnerUtil.runWithProgress( "Downloading " + MAVEN_TITLE + " ...", downloader );

         final FileUnzipper unzipper = new FileUnzipper( mavenZip, mavenInstallDir );
         final File installedDir = RunnerUtil.runWithProgress( "Unzipping " + MAVEN_TITLE + " ...", unzipper );
      } catch ( Exception e ) {
         DialogUtil.showError( e.getMessage() );
         return false;
      }
      _mavenDir = new File( mavenInstallDir, "apache-maven-3.3.1" );
      DialogUtil.showInstalledDialog( MAVEN_TITLE, mavenInstallDir.getAbsolutePath() );
      return true;
   }


   public File chooseMavenDir() {
      final File mavenDir
            = DialogUtil.chooseSaveDir( "Choose new " + MAVEN_TITLE + " Installation Directory" );
      if ( mavenDir == null ) {
         return NO_FILE;
      }
      return mavenDir;
   }


   static private String getMavenPath() {
      final String os = System.getProperty( "os.name" );
      if ( os.toLowerCase().contains( "windows" ) ) {
         return findExecutableOnPath( "mvn.cmd" );
      }
      return findExecutableOnPath( "mvn" );
   }


   public boolean packagePom( final String pomDirectory ) {
      return packagePom( "Running " + MAVEN_TITLE + " ...", pomDirectory );
   }

   public boolean packagePom( final String title, final String pomDirectory ) {
      try {
         new File( pomDirectory + "/logs" ).mkdirs();
         final CommandRunner packager
               = new CommandRunner( pomDirectory,
               pomDirectory + "/logs/MavenOutput.txt",
               pomDirectory + "/logs/MavenError.txt",
               getMavenDir().getAbsolutePath() + "/mvn clean package" );
         return RunnerUtil.runWithProgress( title, packager );
      } catch ( Exception e ) {
         DialogUtil.showError( e.getMessage() );
         return false;
      }
   }


}

