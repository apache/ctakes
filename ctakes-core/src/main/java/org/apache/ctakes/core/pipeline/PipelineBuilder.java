package org.apache.ctakes.core.pipeline;


import org.apache.ctakes.core.cc.FileTreeXmiWriter;
import org.apache.ctakes.core.cc.pretty.html.HtmlTextWriter;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.cr.FileTreeReader;
import org.apache.ctakes.core.util.PropertyAeFactory;
import org.apache.ctakes.core.util.external.SystemUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Creates a pipeline using a small set of simple methods.
 * <p>
 * Some methods are order-specific and calls will directly impact ordering within the pipeline.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/9/2016
 */
final public class PipelineBuilder {

   static private final Logger LOGGER = Logger.getLogger( "PipelineBuilder" );

   private CollectionReaderDescription _readerDesc;
   // TODO replace pairs of 3 lists with 2 instances of a single class.  Put a build() (sub) method in class?
   private final List<String> _aeNameList;
   private final List<String[]> _aeViewList;
   private final List<AnalysisEngineDescription> _descList;
   private final List<String> _aeEndNameList;
   private final List<String[]> _aeEndViewList;
   private final List<AnalysisEngineDescription> _descEndList;

   // Allow the pipeline to be changed even after it has been built once.
   private AnalysisEngineDescription _analysisEngineDesc;
   private boolean _pipelineChanged;
   private int _threadCount = 1;

   public PipelineBuilder() {
      _aeNameList = new ArrayList<>();
      _aeViewList = new ArrayList<>();
      _descList = new ArrayList<>();
      _aeEndNameList = new ArrayList<>();
      _aeEndViewList = new ArrayList<>();
      _descEndList = new ArrayList<>();
      _threadCount = 1;
   }

   public void clear() {
      _aeNameList.clear();
      _aeViewList.clear();
      _descList.clear();
      _aeEndNameList.clear();
      _aeEndViewList.clear();
      _descEndList.clear();
      _threadCount = 1;
   }

   /**
    * Use of this method is order-specific
    *
    * @param parameters add ae parameter name value pairs
    * @return this PipelineBuilder
    */
   public PipelineBuilder set( final Object... parameters ) {
      PropertyAeFactory.getInstance().addParameters( parameters );
      _pipelineChanged = true;
      return this;
   }

   /**
    * Use of this method is order-specific.  If any given parameter is already set it is ignored.
    *
    * @param parameters add ae parameter name value pairs
    * @return this PipelineBuilder
    */
   public PipelineBuilder setIfEmpty( final Object... parameters ) {
      PropertyAeFactory.getInstance()
                       .addIfEmptyParameters( parameters );
      _pipelineChanged = true;
      return this;
   }

   public PipelineBuilder env( final Object... parameters ) {
      SystemUtil.addEnvironmentVariables( parameters );
      // Pipeline was not changed
      return this;
   }

   /**
    * Use of this method is not order-specific
    *
    * @param description Collection Reader Description to place at the beginning of the pipeline
    * @return this PipelineBuilder
    */
   public PipelineBuilder reader( final CollectionReaderDescription description ) {
      _readerDesc = description;
      _pipelineChanged = true;
      return this;
   }

   /**
    * Use of this method is not order-specific
    *
    * @param readerClass Collection Reader class to place at the beginning of the pipeline
    * @param parameters reader parameter name value pairs.  May be empty.
    * @return this PipelineBuilder
    */
   public PipelineBuilder reader( final Class<? extends CollectionReader> readerClass, final Object... parameters )
         throws UIMAException {
      reader( PropertyAeFactory.getInstance().createReaderDescription( readerClass, parameters ) );
      return this;
   }

   /**
    * Adds a Collection reader to the beginning of the pipeline that will read files in a directory tree.
    * Relies upon {@link org.apache.ctakes.core.config.ConfigParameterConstants#PARAM_INPUTDIR} having been specified
    * Use of this method is not order-specific.
    *
    * @return this PipelineBuilder
    * @throws UIMAException if the collection reader cannot be created
    */
   public PipelineBuilder readFiles() throws UIMAException {
      return reader( CollectionReaderFactory.createReaderDescription( FileTreeReader.class ) );
   }

   /**
    * Adds a Collection reader to the beginning of the pipeline that will read files in a directory tree.
    * Use of this method is not order-specific
    *
    * @param inputDirectory directory with input files
    * @return this PipelineBuilder
    * @throws UIMAException if the collection reader cannot be created
    */
   public PipelineBuilder readFiles( final String inputDirectory ) throws UIMAException {
      return reader( FileTreeReader.class, ConfigParameterConstants.PARAM_INPUTDIR, inputDirectory );
   }

   /**
    *
    * @return the Collection Reader for the pipeline or null if none has been specified
    */
   public CollectionReaderDescription getReader() {
      return _readerDesc;
   }

   /**
    * Use of this method is order-specific.
    *
    * @param component ae or cc component class to add to the pipeline
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the component cannot be created
    */
   public PipelineBuilder add( final Class<? extends AnalysisComponent> component ) throws ResourceInitializationException {
      return add( component, Collections.emptyList() );
   }

   /**
    * Use of this method is order-specific.
    *
    * @param component  ae or cc component class to add to the pipeline
    * @param views cas views to use for the component
    * @param parameters ae or cc parameter name value pairs.  May be empty.
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the component cannot be created
    */
   public PipelineBuilder add( final Class<? extends AnalysisComponent> component,
                               final Collection<String> views,
                               final Object... parameters ) throws ResourceInitializationException {
      _aeNameList.add( component.getName() );
      _aeViewList.add( toStringArray( views ) );
      _descList.add( PropertyAeFactory.getInstance().createDescription( component, parameters ) );
      _pipelineChanged = true;
      return this;
   }

   /**
    * Adds an ae or cc wrapped with "Starting processing" and "Finished processing" log messages
    * Use of this method is order-specific.
    *
    * @param component  ae or cc component class to add to the pipeline
    * @param views cas views to use for the component
    * @param parameters ae or cc parameter name value pairs.  May be empty.
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the component cannot be created
    */
   public PipelineBuilder addLogged( final Class<? extends AnalysisComponent> component,
                                     final Collection<String> views,
                                     final Object... parameters ) throws ResourceInitializationException {
      _aeNameList.add( component.getName() );
      _aeViewList.add( toStringArray( views ) );
      _descList.add( PropertyAeFactory.getInstance().createLoggedDescription( component, parameters ) );
      _pipelineChanged = true;
      return this;
   }

   /**
    * Use of this method is order-specific.
    *
    * @param description ae or cc component class description to add to the pipeline
    * @return this PipelineBuilder
    */
   public PipelineBuilder addDescription( final AnalysisEngineDescription description ) {
      return addDescription( description, Collections.emptyList() );
   }

   /**
    * Use of this method is order-specific.
    *
    * @param description ae or cc component class description to add to the pipeline
    * @param views       cas views to use for the component
    * @return this PipelineBuilder
    */
   public PipelineBuilder addDescription( final AnalysisEngineDescription description,
                                          final Collection<String> views ) {
      _aeNameList.add( description.getAnnotatorImplementationName() );
      _aeViewList.add( toStringArray( views ) );
      _descList.add( description );
      _pipelineChanged = true;
      return this;
   }

   /**
    * Adds an ae or cc component t othe very end of the pipeline.  Use of this method is order-specific.
    *
    * @param component  ae or cc component class to add to the end of the pipeline
    * @param views cas views to use for the component
    * @param parameters ae or cc parameter name value pairs.  May be empty.
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the component cannot be created
    */
   public PipelineBuilder addLast( final Class<? extends AnalysisComponent> component,
                                   final Collection<String> views,
                                   final Object... parameters ) throws ResourceInitializationException {
      _aeEndNameList.add( component.getName() );
      _aeEndViewList.add( toStringArray( views ) );
      _descEndList.add( PropertyAeFactory.getInstance().createDescription( component, parameters ) );
      _pipelineChanged = true;
      return this;
   }

   /**
    *
    * @return an ordered list of the annotation engines in the pipeline
    */
   public List<String> getAeNames() {
      final List<String> allNames = new ArrayList<>( _aeNameList );
      allNames.addAll( _aeEndNameList );
      return Collections.unmodifiableList( allNames );
   }

   /**
    * Adds ae that maintains CUI information throughout the run.
    * CUI information can later be accessed using the {@link CuiCollector} singleton
    * Use of this method is order-specific.
    *
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the CuiCollector engine cannot be created
    */
   public PipelineBuilder collectCuis() throws ResourceInitializationException {
      return add( CuiCollector.CuiCollectorEngine.class );
   }

   /**
    * Adds ae that maintains simple Entity information throughout the run.
    * Entity information can later be accessed using the {@link EntityCollector} singleton
    * Use of this method is order-specific.
    *
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the EntityCollector engine cannot be created
    */
   public PipelineBuilder collectEntities() throws ResourceInitializationException {
      return add( EntityCollector.EntityCollectorEngine.class );
   }

   /**
    * Adds ae that writes an xmi file at the end of the pipeline.
    * Relies upon {@link ConfigParameterConstants#PARAM_OUTPUTDIR} having been specified
    * Use of this method is order-specific.
    *
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the Xmi writer engine cannot be created
    */
   public PipelineBuilder writeXMIs() throws ResourceInitializationException {
      return addLast( FileTreeXmiWriter.class, Collections.emptyList() );
   }

   /**
    * Adds ae that writes an xmi file at the end of the pipeline.
    * Use of this method is order-specific.
    *
    * @param outputDirectory directory in which xmi files should be written
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the Xmi writer engine cannot be created
    */
   public PipelineBuilder writeXMIs( final String outputDirectory ) throws ResourceInitializationException {
      return addLast( FileTreeXmiWriter.class, Collections.emptyList(),
            ConfigParameterConstants.PARAM_OUTPUTDIR, outputDirectory );
   }

   /**
    * Adds ae that writes an html file at the end of the pipeline.
    * Relies upon {@link ConfigParameterConstants#PARAM_OUTPUTDIR} having been specified
    * Use of this method is order-specific.
    *
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the html writer engine cannot be created
    */
   public PipelineBuilder writeHtml() throws ResourceInitializationException {
      return addLast( HtmlTextWriter.class, Collections.emptyList() );
   }

   /**
    * Adds ae that writes an html file at the end of the pipeline.
    * Use of this method is order-specific.
    *
    * @param outputDirectory directory in which html files should be written
    * @return this PipelineBuilder
    * @throws ResourceInitializationException if the html writer engine cannot be created
    */
   public PipelineBuilder writeHtml( final String outputDirectory ) throws ResourceInitializationException {
      return addLast( HtmlTextWriter.class, Collections.emptyList(),
            ConfigParameterConstants.PARAM_OUTPUTDIR, outputDirectory );
   }

   public PipelineBuilder threads( final int threadCount ) {
      if ( threadCount <= 1 ) {
         if ( threadCount < 1 ) {
            LOGGER.warn( "Thread count (" + threadCount + ") cannot be below 1.  Using 1 thread for processing." );
         }
         _threadCount = 1;
         return this;
      }
      final int coreCount = Runtime.getRuntime().availableProcessors();
      if ( threadCount > coreCount ) {
         LOGGER.warn( "Thread count (" + threadCount + ") is greater than core count ("
               + coreCount + ").  Using core count for processing." );
         _threadCount = coreCount;
         return this;
      }
      _threadCount = threadCount;
      return this;
   }

   public int getThreadCount() {
      return _threadCount;
   }

   /**
    * Initialize a pipeline that can be used repeatedly using {@link #run} and {@link #run(String)}.
    * A pipeline can be extended between builds, but the full pipeline will be rebuilt on each call.
    * Use of this method is order-specific.
    * @return this PipelineBuilder
    * @throws IOException   if the pipeline could not be built
    * @throws UIMAException if the pipeline could not be built
    */
   public PipelineBuilder build() throws IOException, UIMAException {
      if ( _analysisEngineDesc == null || _pipelineChanged ) {
         final AggregateBuilder builder = new AggregateBuilder();
         for ( int i = 0; i < _descList.size(); i++ ) {
            builder.add( _descList.get( i ), _aeViewList.get( i ) );
         }
         for ( int i = 0; i < _descEndList.size(); i++ ) {
            builder.add( _descEndList.get( i ), _aeEndViewList.get( i ) );
         }
         _analysisEngineDesc = builder.createAggregateDescription();
      }
      _pipelineChanged = false;
      return this;
   }

   /**
    * Run the pipeline using some specified collection reader.
    * Use of this method is order-specific.
    * This method will call {@link #build()} if the pipeline has not already been initialized.
    *
    * @return this PipelineBuilder
    * @throws IOException   if the pipeline could not be run
    * @throws UIMAException if the pipeline could not be run
    */
   public PipelineBuilder run() throws IOException, UIMAException {
      if ( _readerDesc == null ) {
         LOGGER.error( "No Collection Reader specified." );
         return this;
      }
      build();
      if ( _threadCount == 1 ) {
         SimplePipeline.runPipeline( _readerDesc, _analysisEngineDesc );
      } else {
         final CpeBuilder cpeBuilder = new CpeBuilder();
         try {
            cpeBuilder.setReader( _readerDesc );
            cpeBuilder.setAnalysisEngine( _analysisEngineDesc );
            cpeBuilder.setMaxProcessingUnitThreadCount( _threadCount );
            final CollectionProcessingEngine cpe = cpeBuilder.createCpe( null );
            cpe.process();
         } catch ( CpeDescriptorException | SAXException multE ) {
            LOGGER.error( multE.getMessage(), multE );
            throw new UIMAException( multE );
         }
      }
      return this;
   }

   /**
    * Run the pipeline on the given text.
    * Use of this method is order-specific.
    * This method will call {@link #build()} if the pipeline has not already been initialized.
    *
    * @param text text upon which to run this pipeline
    * @return this PipelineBuilder
    * @throws IOException   if the pipeline could not be run
    * @throws UIMAException if the pipeline could not be run
    */
   public PipelineBuilder run( final String text ) throws IOException, UIMAException {
      if ( _readerDesc != null ) {
         LOGGER.error( "Collection Reader specified, ignoring." );
         return this;
      }
      final JCas jcas = JCasFactory.createJCas();
      jcas.setDocumentText( text );
      return run( jcas );
   }

   /**
    * Run the pipeline on the given jcas.
    * Use of this method is order-specific.
    * This method will call {@link #build()} if the pipeline has not already been initialized.
    *
    * @param jCas ye olde ...
    * @return this PipelineBuilder
    * @throws IOException   if the pipeline could not be run
    * @throws UIMAException if the pipeline could not be run
    */
   public PipelineBuilder run( final JCas jCas ) throws IOException, UIMAException {
      if ( _readerDesc != null ) {
         LOGGER.error( "Collection Reader specified, ignoring." );
         return this;
      }
      build();
      SimplePipeline.runPipeline( jCas, _analysisEngineDesc );
      return this;
   }

   /**
    * @return an analysis engine description, for use in creating xml descriptor files, etc.
    * @throws IOException   if the description could not be built
    * @throws UIMAException if the description could not be built
    */
   public AnalysisEngineDescription getAnalysisEngineDesc() throws IOException, UIMAException {
      build();
      return _analysisEngineDesc;
   }

   static private String[] toStringArray( final Collection<String> things ) {
      return new ArrayList<>( things ).toArray( new String[ things.size() ] );
   }

}
