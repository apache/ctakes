package org.apache.ctakes.core.concurrent;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.internal.ExtendedLogger;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * To take advantage of singletons for thread safety and enums for singletons,
 * utilize jdk 8+ interface default methods so that enums can implement AnalysisComponent without
 * boilerplate code for every method.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/20/2017
 */
public interface ThreadSafeWrapper<AE extends JCasAnnotator_ImplBase> extends AnalysisComponent {

   /**
    * @return an object upon which to lock the ae
    */
   Object getLock();

   /**
    * @return The annotator wrapped by this object
    */
   AE getDelegate();

   /**
    * @return true if initialized
    */
   boolean isInitialized();

   /**
    * @param initialized true if initialized
    */
   void setInitialized( final boolean initialized );

   /**
    * Calls initialize on the single instance if and only if it has not already been initialized
    */
   @Override
   default void initialize( final UimaContext context ) throws ResourceInitializationException {
      synchronized (getLock()) {
         if ( !isInitialized() ) {
            getDelegate().initialize( context );
            setInitialized( true );
         }
      }
   }

   /**
    * Calls process on the single instance if it is not already processing
    */
   default void process( final JCas jCas ) throws AnalysisEngineProcessException {
      synchronized (getLock()) {
         getDelegate().process( jCas );
      }
   }

   /**
    * from uimafit JCasAnnotator_ImplBase
    *
    * @return -
    */
   default ExtendedLogger getLogger() {
      return getDelegate().getLogger();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default void reconfigure() throws ResourceConfigurationException, ResourceInitializationException {
      synchronized (getLock()) {
         getDelegate().reconfigure();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default void batchProcessComplete() throws AnalysisEngineProcessException {
      synchronized (getLock()) {
         getDelegate().batchProcessComplete();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default void collectionProcessComplete() throws AnalysisEngineProcessException {
      synchronized (getLock()) {
         getDelegate().collectionProcessComplete();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default void destroy() {
      synchronized (getLock()) {
         getDelegate().destroy();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default void process( final AbstractCas aCas ) throws AnalysisEngineProcessException {
      synchronized (getLock()) {
         getDelegate().process( aCas );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default boolean hasNext() throws AnalysisEngineProcessException {
      synchronized (getLock()) {
         return getDelegate().hasNext();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default AbstractCas next() throws AnalysisEngineProcessException {
      synchronized (getLock()) {
         return getDelegate().next();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default Class<JCas> getRequiredCasInterface() {
      return getDelegate().getRequiredCasInterface();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default int getCasInstancesRequired() {
      return getDelegate().getCasInstancesRequired();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   default void setResultSpecification( final ResultSpecification resultSpec ) {
      synchronized (getLock()) {
         getDelegate().setResultSpecification( resultSpec );
      }
   }

}
