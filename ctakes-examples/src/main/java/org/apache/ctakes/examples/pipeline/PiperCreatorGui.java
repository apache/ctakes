package org.apache.ctakes.examples.pipeline;


import org.apache.ctakes.gui.pipeline.PiperCreator;

/**
 * This is an alternative entry to the PiperCreator in ctakes-gui.
 * In a developer environment the PiperCreator may not have all the available Pipe Bits in the classpath.
 * The ctakes-examples classpath should make them all available.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/9/2019
 */
final public class PiperCreatorGui {

   private PiperCreatorGui() {
   }

   public static void main( final String... args ) {
      PiperCreator.main( args );
   }

}
