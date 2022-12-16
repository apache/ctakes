package org.apache.ctakes.gui.pipeline.piper;

import org.apache.ctakes.gui.util.IconLoader;

import javax.swing.*;
import javax.swing.filechooser.FileView;
import java.io.File;

/**
 * FileView that provides icon and description for piper files.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/10/2017
 */
public final class PiperFileView extends FileView {
   private Icon _piperIcon = null;

   public PiperFileView() {
      SwingUtilities.invokeLater( new FileIconLoader() );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getTypeDescription( final File file ) {
      final String name = file.getName();
      if ( name.endsWith( ".piper" ) ) {
         return "Pipeline Definition (Piper) file.";
      }
      return super.getTypeDescription( file );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Icon getIcon( final File file ) {
      final String name = file.getName();
      if ( name.endsWith( ".piper" ) && _piperIcon != null ) {
         return _piperIcon;
      }
      return super.getIcon( file );
   }

   /**
    * Simple Runnable that loads an icon
    */
   private final class FileIconLoader implements Runnable {
      @Override
      public void run() {
         final String dir = "org/apache/ctakes/gui/pipeline/icon/";
         final String piperPng = "PiperFile.png";
         _piperIcon = IconLoader.loadIcon( dir + piperPng );
      }
   }
}
