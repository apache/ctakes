package org.apache.ctakes.dockhand.build;

import org.apache.ctakes.dockhand.gui.feature.GoalPom;
import org.apache.ctakes.dockhand.gui.wizard.DhWizardController;
import org.apache.ctakes.gui.wizard.util.DialogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/26/2019
 */
final public class RestDockerBuilder implements Runnable {

   static private final String INSTALL_TYPE = "Apache cTAKES Rest Docker";

   static private final String PIPER_DIR = "/org/apache/ctakes/dockhand/goal/rest/pipeline/";
   static private final String SOURCE_URLDIR = "/org/apache/ctakes/dockhand/goal/rest/";
   static private final String DOCKER_DIR = "/org/apache/ctakes/dockhand/goal/rest/docker/";
   static private final String SCRIPT_DIR = "/org/apache/ctakes/dockhand/goal/rest/script/";
   static private final String WEBAPP_SOURCE_DIR = "/org/apache/ctakes/dockhand/goal/rest/webapp/";
   static private final String WEBAPP_TARGET_DIR = "/webapp/";

   static private final String[] SCRIPTS = { "install_rest.sh",
                                             "start_rest.sh",
                                             "stop_rest.sh" };

   static private final String[] WEBAPPS = { "index.jsp",
                                             "css/index.css",
                                             "js/app.js",
                                             "js/jquery.js",
                                             "META-INF/MANIFEST.MF",
                                             "WEB-INF/ctakes-rest-service-servlet.xml",
                                             "WEB-INF/web.xml" };

   private final DhWizardController _wizardController;

   public RestDockerBuilder( final DhWizardController wizardController ) {
      _wizardController = wizardController;
   }

   public void run() {
      final GoalPom goalPom = _wizardController.getGoalPom();
      final Collection<String> piperCommands = _wizardController.getPiperCommands();

      final Collection<CopyFileSpec> extraFiles = new ArrayList<>();
      extraFiles.add( new CopyFileSpec( SOURCE_URLDIR + "README.txt", "README.txt" ) );
      extraFiles.add( new CopyFileSpec( DOCKER_DIR + "Dockerfile", "Dockerfile" ) );
      extraFiles.add( new CopyFileSpec( PIPER_DIR + "TinyRestPipeline.piper", "TinyRestPipeline.piper" ) );
      Arrays.stream( SCRIPTS )
            .map( f -> new CopyFileSpec( SCRIPT_DIR + f, f ) )
            .forEach( extraFiles::add );
      Arrays.stream( WEBAPPS )
            .map( f -> new CopyFileSpec( WEBAPP_SOURCE_DIR + f, WEBAPP_TARGET_DIR + f ) )
            .forEach( extraFiles::add );
      final BaseInstaller baseInstaller
            = new BaseInstaller( INSTALL_TYPE, SOURCE_URLDIR, goalPom.getPomFile(), piperCommands, extraFiles );

      final boolean finished = baseInstaller.install();
      if ( finished ) {
         final String installPath = InstallHelper.getInstance().getInstallDir().getAbsolutePath();
         DialogUtil.showInstalledDialog( INSTALL_TYPE, installPath );
      }
   }

   // TODO   Maybe the rest could output html if the css and js are specified in index.jsp ??

   // TODO  Local src/bin Installation, Local Rest WAR, Installation Docker, Rest Docker.

   // TODO set the Buildable boolean supplier in wizard controller


}
