package org.apache.ctakes.dockhand.build;

import org.apache.ctakes.gui.wizard.util.DialogUtil;

import java.io.File;

import static org.apache.ctakes.gui.wizard.util.SystemUtil.NO_FILE;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/3/2019
 */
public enum InstallHelper {
   INSTANCE;

   static public InstallHelper getInstance() {
      return INSTANCE;
   }


   private File _installDir = new File( System.getProperty( "user.dir" ) );

   public File getInstallDir() {
      return _installDir;
   }


   public File chooseInstallDir() {
      final File installDir = DialogUtil.chooseSaveDir( "Choose Apache cTAKES Installation Directory" );
      if ( installDir == null ) {
         return NO_FILE;
      }
      _installDir = installDir;
      return _installDir;
   }


}

