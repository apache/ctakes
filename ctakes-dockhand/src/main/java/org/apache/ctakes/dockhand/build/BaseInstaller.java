package org.apache.ctakes.dockhand.build;


import org.apache.ctakes.gui.wizard.util.DialogUtil;
import org.apache.ctakes.gui.wizard.util.RunnerUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import static org.apache.ctakes.gui.wizard.util.SystemUtil.NO_FILE;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/10/2019
 */
final public class BaseInstaller implements Callable<File> {

   static private final String PIPER_NAME = "DockhandPipeline.piper";

   private final String _installType;
   private final String _piperCommands;
   private final Collection<CopyFileSpec> _copyFileSpecs;
   private File _installDir;

   public BaseInstaller( final String installType,
                         final String sourceDirUrl,
                         final String goalPomFile,
                         final Collection<String> piperCommands,
                         final Collection<CopyFileSpec> extraFiles ) {
      _installType = installType;
      _piperCommands = String.join( "\n", piperCommands );
      _copyFileSpecs = new ArrayList<>( extraFiles );
      _copyFileSpecs.add( new CopyFileSpec( sourceDirUrl + "pom/" + goalPomFile, "pom.xml" ) );
      _copyFileSpecs.add( new CopyFileSpec( "/log4j.xml" ) );
      _copyFileSpecs.add( new CopyFileSpec( "/LICENSE" ) );
      _copyFileSpecs.add( new CopyFileSpec( "/NOTICE" ) );
   }


   public boolean install() {
      final File installDir = InstallHelper.getInstance().chooseInstallDir();
      if ( installDir.equals( NO_FILE ) ) {
         DialogUtil.showInstallCanceled( _installType );
         return false;
      }
      _installDir = installDir;

      RunnerUtil.runWithProgress( "Installing " + _installType + " ...", this );
      return true;
   }

   public File call() {
      final String installPath = _installDir.getAbsolutePath();
      writePiperFile( installPath, _piperCommands );
      _copyFileSpecs.forEach( f -> f.copyToDisk( installPath ) );
      return _installDir;
   }

   static private void writePiperFile( final String installPath, final String piperCommands ) {
      new File( installPath ).mkdirs();
      final String piperPath = installPath + "/" + PIPER_NAME;
      try ( Writer writer = new BufferedWriter( new FileWriter( piperPath ) ) ) {
         writer.write( piperCommands + "\n" );
      } catch ( IOException ioE ) {
         DialogUtil.showError( ioE.getMessage() );
      }
   }


}
