package org.apache.ctakes.gui.pipeline.bit;


import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.SubclassMatchProcessor;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_component.Annotator_ImplBase;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.collection.CollectionReader_ImplBase;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Finds Collection Readers, Annotators, Cas Consumers (Writers), and their metadata
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/22/2016
 */
public enum PipeBitFinder {
   INSTANCE;

   static public PipeBitFinder getInstance() {
      return INSTANCE;
   }

   private final Logger LOGGER = Logger.getLogger( "PipeBitFinder" );

   private final Collection<Class<?>> _pipeBits = new ArrayList<>();
   private boolean _didScan = false;

   synchronized public void reset() {
      _pipeBits.clear();
      _didScan = false;
   }

   synchronized public Collection<Class<?>> getPipeBits() {
      scan();
      return _pipeBits;
   }

   static private boolean isPipeBit( final Class<?> clazz ) {
      final String className = clazz.getName();
      return !className.startsWith( "org.apache.uima.tutorial" )
             && !className.startsWith( "org.apache.uima.examples" )
             && !Modifier.isAbstract( clazz.getModifiers() )
             && clazz.getEnclosingClass() == null;
   }

   synchronized public void scan() {
      if ( _didScan ) {
         return;
      }
      final SubclassMatchProcessor<CollectionReader_ImplBase> readerAdder = r -> {
         if ( isPipeBit( r ) ) {
            _pipeBits.add( r );
         }
      };
      final SubclassMatchProcessor<Annotator_ImplBase> annotatorAdder = a -> {
         if ( isPipeBit( a ) ) {
            _pipeBits.add( a );
         }
      };
      final SubclassMatchProcessor<CasConsumer_ImplBase> writerAdder = w -> {
         if ( isPipeBit( w ) ) {
            _pipeBits.add( w );
         }
      };
      // Don't want super-primitive cleartk or uima classes
      final FastClasspathScanner scanner = new FastClasspathScanner();
      LOGGER.info( "Starting Scan for Pipeline Bits" );
      try ( DotLogger dotter = new DotLogger() ){
         scanner.matchSubclassesOf( CollectionReader_ImplBase.class, readerAdder )
               .matchSubclassesOf( Annotator_ImplBase.class, annotatorAdder )
               .matchSubclassesOf( CasConsumer_ImplBase.class, writerAdder );
         final ScanResult result = scanner.scan( 1 );
//      } catch ( ScanInterruptedException | IOException siE ) {
      } catch ( RuntimeException | IOException siE ) {
         LOGGER.error( siE.getMessage() );
      }
      // Get rid of the abstract classes
      _pipeBits.remove( CollectionReader_ImplBase.class );
      _pipeBits.remove( JCasAnnotator_ImplBase.class );
      _pipeBits.remove( CasConsumer_ImplBase.class );
      // FastClassPathScanner blacklisting doesn't work, so we need to remove unwanted packages manually
      _pipeBits.removeIf( c -> c.getPackage().getName().startsWith( "org.cleartk" )
                               || c.getPackage().getName().startsWith( "org.apache.uima" ) );
      LOGGER.info( "Scan Finished" );
      _didScan = true;
   }

}
