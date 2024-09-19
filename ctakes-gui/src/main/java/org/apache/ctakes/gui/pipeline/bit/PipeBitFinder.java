package org.apache.ctakes.gui.pipeline.bit;

import io.github.classgraph.*;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.log.DotLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.analysis_component.Annotator_ImplBase;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.collection.CollectionReader_ImplBase;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Finds Collection Readers, Annotators, Cas Consumers (Writers), and their
 * metadata
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

	private final Logger LOGGER = LoggerFactory.getLogger("PipeBitFinder");

	private final Collection<Class<?>> _pipeBits = new ArrayList<>();
	private boolean _didScan = false;
	private boolean _allImplementations = (System.getProperty("ctakes.gui.all-impls") != null);

	synchronized public void reset() {
		_pipeBits.clear();
		_didScan = false;
	}

	synchronized public Collection<Class<?>> getPipeBits() {
		scan();
		return _pipeBits;
	}

	static private boolean isPipeBit(final Class<?> clazz) {
		final String className = clazz.getName();
		return !className.startsWith("org.apache.uima.tutorial") && !className.startsWith("org.apache.uima.examples")
				&& !Modifier.isAbstract(clazz.getModifiers()) && clazz.getEnclosingClass() == null;
	}
	
	
	synchronized private ClassInfoList findByAnnotation(ScanResult result) {
		return result.getClassesWithAnnotation(PipeBitInfo.class.getTypeName());
	}
	
	synchronized private ClassInfoList findByType(ScanResult result) {
		ClassInfoList list = result.getSubclasses(Annotator_ImplBase.class.getTypeName());
		list.union(
				result.getSubclasses(JCasAnnotator_ImplBase.class.getTypeName()),
				result.getSubclasses(CasConsumer_ImplBase.class.getTypeName()),
				result.getSubclasses(CollectionReader_ImplBase.class.getTypeName()));
		return list;
	}

	synchronized public void scan() {
		  if ( _didScan ) {
		     return;
		  }
		
		  final ClassGraph scanner = new ClassGraph().enableAllInfo();

		String message = (_allImplementations) ? "all possible " : "official";
      LOGGER.info( "Starting Scan for ({}) Pipeline Bits", message );
		  try ( DotLogger dotter = new DotLogger(); ScanResult result = scanner.scan()) {
			ClassInfoList list = null;
			if(_allImplementations)
				list = findByType(result);
			else
				list = findByAnnotation(result);
			
			for(ClassInfo thing : list) {
				Class<?> clazz = thing.loadClass();
				if (isPipeBit(clazz)) {
					_pipeBits.add( clazz );
				}
			}
		
		  } catch ( RuntimeException | IOException siE ) {
		     LOGGER.error( siE.getMessage() );
		  }
		
		
		  // FastClassPathScanner blacklisting doesn't work, so we need to remove unwanted packages manually
		  _pipeBits.removeIf( c -> c.getPackage().getName().startsWith( "org.cleartk" )
		                       || c.getPackage().getName().startsWith( "org.apache.uima" ) );
		  LOGGER.info( "Scan Finished with " +_pipeBits.size()+ " items found." );
		  if(LOGGER.isDebugEnabled()) {
			  for(Class<?> annot : (Collection<Class<?>>)_pipeBits) {
				  if(JCasAnnotator_ImplBase.class.isAssignableFrom(annot)) {
					  PipeBitInfo p = (PipeBitInfo)annot.getAnnotation(PipeBitInfo.class);
					  if (p != null) {
						  LOGGER.info(String.format("#(%s)#%s#%s#unused\n",annot.getSuperclass().getSimpleName(), annot.getSimpleName(), p.description()));
					  } else {
						  LOGGER.info(String.format("#(%s)#%s#Unofficial Offering#unuseds\n",annot.getSuperclass().getSimpleName(), annot.getSimpleName()));
					  }
				  }
			  }
		  }
		  _didScan = true;
   }

}
