package org.apache.ctakes.dockhand.build;

import org.apache.ctakes.gui.wizard.util.SystemUtil;

import java.io.File;
import java.nio.file.Paths;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/29/2020
 */ // TODO pass vararg of TargetFileSpec to BaseInstaller.  Call its copyToDisk method.
final public class CopyFileSpec {
   private final String _source;
   private final String _target;

   public CopyFileSpec( final String name ) {
      this( name, name );
   }

   public CopyFileSpec( final String source, final String target ) {
      _source = source;
      _target = target;
   }

   public void copyToDisk( final String targetRoot ) {
      final String targetPath = targetRoot + "/" + _target;
      new File( targetPath ).getParentFile().mkdirs();
      SystemUtil.copyToDisk( getClass().getResourceAsStream( _source ), Paths.get( targetPath ) );
   }

}
