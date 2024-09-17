package org.apache.ctakes.dockhand.build;

import org.apache.ctakes.dockhand.gui.feature.GoalPom;
import org.apache.ctakes.dockhand.gui.wizard.DhWizardController;
import org.apache.ctakes.gui.wizard.util.DialogUtil;
import org.apache.ctakes.gui.wizard.util.RunnerUtil;
import org.apache.ctakes.gui.wizard.util.SystemUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/26/2019
 */
final public class LocalInstallBuilder implements Runnable {

   static private final String INSTALL_TYPE = "Apache cTAKES Local";

   static private final String BINARY_DIR = "/org/apache/ctakes/dockhand/binary/";
   static private final String SOURCE_URLDIR = "/org/apache/ctakes/dockhand/goal/local/";

   static private final String INSTALL_DIRNAME = "apache-ctakes-7.0.0-SNAPSHOT";

   private final DhWizardController _wizardController;

   public LocalInstallBuilder( final DhWizardController wizardController ) {
      _wizardController = wizardController;
   }

   public void run() {
      final boolean maven = MavenHelper.getInstance().installMaven();
      if ( !maven ) {
         return;
      }

      final GoalPom goalPom = _wizardController.getGoalPom();
      final Collection<String> piperCommands = _wizardController.getPiperCommands();

      final Collection<CopyFileSpec> extraFiles = new ArrayList<>();
      extraFiles.add( new CopyFileSpec( SOURCE_URLDIR + "pom/build.xml", "build.xml" ) );
      extraFiles.add( new CopyFileSpec( BINARY_DIR + "RunPiperGui.sh", "RunPiperGui.sh" ) );
      extraFiles.add( new CopyFileSpec( BINARY_DIR + "RunPiperGui.bat", "RunPiperGui.bat" ) );
      final BaseInstaller baseInstaller
            = new BaseInstaller( INSTALL_TYPE, SOURCE_URLDIR, goalPom.getPomFile(), piperCommands, extraFiles );

      final boolean install = baseInstaller.install();
      if ( !install ) {
         return;
      }

      final String installPath = InstallHelper.getInstance().getInstallDir().getAbsolutePath();
      final boolean packaged
            = MavenHelper.getInstance().packagePom( "Installing " + INSTALL_TYPE + " ...", installPath );
      if ( !packaged ) {
         return;
      }
      final boolean finished = cleanup( installPath );
      if ( finished ) {
         DialogUtil.showInstalledDialog( INSTALL_TYPE, installPath );
      }
//      if ( DialogUtil.showStartGui() ) {
//         startGui( "Running Apache cTAKES ...<BR>   RunPiperGui", installPath + "/" + INSTALL_DIRNAME );
//      }
   }

   static private boolean cleanup( final String installPath ) {
      final File installDir = new File( installPath );
      final File[] files = installDir.listFiles();
      if ( files == null ) {
         return false;
      }
      for ( File file : files ) {
         if ( file.isFile() ) {
            if ( !deleteFile( file ) ) {
               return false;
            }
         } else if ( file.getName().equals( "target" ) ) {
            if ( !deleteDirectory( file ) ) {
               return false;
            }
         }
      }
      return true;
   }

   static private boolean deleteDirectory( final File directory ) {
      final File[] files = directory.listFiles();
      if ( files == null ) {
         return false;
      }
      for ( File file : files ) {
         if ( file.isFile() ) {
            if ( !deleteFile( file ) ) {
               return false;
            }
         } else {
            if ( !deleteDirectory( file ) ) {
               DialogUtil.showError( "Could not delete directory " + file.getAbsolutePath() );
               return false;
            }
         }
      }
      return directory.delete();
   }

   static private boolean deleteFile( final File file ) {
      if ( !file.delete() ) {
         DialogUtil.showError( "Could not delete file " + file.getAbsolutePath() );
         return false;
      }
      return true;
   }


   public boolean startGui( final String title, final String installDirectory ) {
      try {
         new File( installDirectory + "/logs" ).mkdirs();
         final SystemUtil.CommandRunner runner
               = new SystemUtil.CommandRunner( installDirectory,
               installDirectory + "/logs/RunOutput.txt",
               installDirectory + "/logs/RunError.txt",
               "RunPiperGui" );
         return RunnerUtil.runWithProgress( title, runner );
      } catch ( Exception e ) {
         DialogUtil.showError( e.getMessage() );
         return false;
      }
   }


   // TODO  Local src/bin Installation, Local Rest WAR, Installation Docker, Rest Docker.


   // TODO  For local source installations, instead of doing svn etc., ask which ide.
   //  Then go step-by-step opening the web page with help on that step and indicating svn repo address.
   //  :
   //  Follow the instructions at:
   //    https://www.jetbrains.com/help/idea/checking-out-files-from-subversion-repository.html
   //  using the svn repository
   //    https://svn.apache.org/repos/asf/ctakes/trunk/
   //    or
   //    https://www.eclipse.org/subversive/


}
