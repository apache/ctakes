/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.temporal.eval;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.lexicalscope.jewel.cli.Option;
import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.chunker.ae.DefaultChunkCreator;
import org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster;
import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.context.tokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.OverlapAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.patient.PatientNoteStore;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.temporal.ae.I2B2TemporalXMLReader;
import org.apache.ctakes.temporal.ae.THYMEAnaforaXMLReader;
import org.apache.ctakes.temporal.ae.THYMEKnowtatorXMLReader;
import org.apache.ctakes.temporal.ae.THYMETreebankReader;
import org.apache.ctakes.temporal.duration.Utils;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.syntax.TreebankNode;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.utils.struct.CounterMap;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.component.ViewTextCopierAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypePrioritiesFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.XMLSerializer;
import org.cleartk.timeml.util.TimeWordsExtractor;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.apache.ctakes.core.cleartk.ae.SentenceDetectorAnnotator;
//import org.threeten.bp.temporal.TemporalUnit;

public abstract class Evaluation_ImplBase<STATISTICS_TYPE> extends
org.cleartk.eval.Evaluation_ImplBase<Integer, STATISTICS_TYPE> {

	static Logger LOGGER = Logger.getLogger( Evaluation_ImplBase.class );

	private static final String LOOKUP_PATH = "/org/apache/ctakes/temporal/badEEContainNotes.txt";

	private static boolean isTraining;

	public static HashSet<String> badNotes;

	public static final String GOLD_VIEW_NAME = "GoldView";

	public static final String PROB_VIEW_NAME = "ProbView";
	
	public static final int MAX_DOC_VIEWS = 3;

	public enum XMLFormat {Knowtator, Anafora, I2B2, AnaforaCoref}

	public enum Subcorpus {Colon, Brain, DeepPhe}

	public static interface Options {

		@Option( longName = "text", defaultToNull = true )
		public File getRawTextDirectory();

		@Option( longName = "xml" )
		public File getXMLDirectory();

		@Option( longName = "format", defaultValue = "Anafora" )
		public XMLFormat getXMLFormat();

		@Option( longName = "subcorpus", defaultValue = "Colon" )
		public Subcorpus getSubcorpus();

		@Option( longName = "xmi" )
		public File getXMIDirectory();

		@Option( longName = "patients" )
		public CommandLine.IntegerRanges getPatients();

		//      @Option( longName = "train-remainders", defaultValue = "0-2" )
		//      public CommandLine.IntegerRanges getTrainRemainders();
		//
		//      @Option( longName = "dev-remainders", defaultValue = "3" )
		//      public CommandLine.IntegerRanges getDevRemainders();
		//
		//      @Option( longName = "test-remainders", defaultValue = "4-5" )
		//      public CommandLine.IntegerRanges getTestRemainders();

		@Option( longName = "train-remainders", defaultValue = "0-3" )
		public CommandLine.IntegerRanges getTrainRemainders();

		@Option( longName = "dev-remainders", defaultValue = "4-5" )
		public CommandLine.IntegerRanges getDevRemainders();

		@Option( longName = "test-remainders", defaultValue = "6-7" )
		public CommandLine.IntegerRanges getTestRemainders();

		@Option( longName = "treebank", defaultToNull = true )
		public File getTreebankDirectory();

		@Option
		public boolean getUseGoldTrees();

		@Option
		public boolean getGrid();

		@Option
		public boolean getPrintErrors();

		@Option
		public boolean getPrintOverlappingSpans();

		@Option
		public boolean getTest();

		@Option( longName = "kernelParams", defaultToNull = true )
		public String getKernelParams();

		@Option( defaultToNull = true )
		public String getI2B2Output();

		@Option( defaultToNull = true )
		public String getAnaforaOutput();

		@Option
		public boolean getSkipTrain();

		@Option(longName = "skipWrite")
		public boolean getSkipDataWriting();
	}

	public static List<Integer> getTrainItems( Options options ) {
		List<Integer> patientSets = options.getPatients().getList();
		List<Integer> trainItems = THYMEData.getPatientSets( patientSets, options.getTrainRemainders().getList() );
		if ( options.getTest() ) {
			trainItems.addAll( THYMEData.getPatientSets( patientSets, options.getDevRemainders().getList() ) );
		}
		return trainItems;
	}

	public static List<Integer> getTestItems( Options options ) {
		List<Integer> patientSets = options.getPatients().getList();
		List<Integer> testItems;
		if ( options.getTest() ) {
			testItems = THYMEData.getPatientSets( patientSets, options.getTestRemainders().getList() );
		} else {
			testItems = THYMEData.getPatientSets( patientSets, options.getDevRemainders().getList() );
		}
		return testItems;
	}

	protected File rawTextDirectory;

	protected File xmlDirectory;

	protected XMLFormat xmlFormat;

	protected Subcorpus subcorpus;

	protected File xmiDirectory;

	private boolean xmiExists;

	protected File treebankDirectory;

	protected boolean printErrors = false;

	protected boolean printOverlapping = false;

	protected String i2b2Output = null;

	protected String anaforaOutput = null; 

	protected String[] kernelParams;

	public Evaluation_ImplBase(
			File baseDirectory,
			File rawTextDirectory,
			File xmlDirectory,
			XMLFormat xmlFormat,
			Subcorpus subcorpus,
			File xmiDirectory,
			File treebankDirectory ) {
		super( baseDirectory );
		this.rawTextDirectory = rawTextDirectory;
		this.xmlDirectory = xmlDirectory;
		this.xmlFormat = xmlFormat;
		this.subcorpus = subcorpus;
		this.xmiDirectory = xmiDirectory;
		this.xmiExists = this.xmiDirectory.exists() && this.xmiDirectory.listFiles().length > 0;
		this.treebankDirectory = treebankDirectory;

		this.isTraining = true;
		this.badNotes = new HashSet<>();
		URL url = TimeWordsExtractor.class.getResource( LOOKUP_PATH );
		try ( BufferedReader br = new BufferedReader( new FileReader( url.getFile() ) ) ) {
			String line;
			while ( (line = br.readLine()) != null ) {
				badNotes.add( line.trim() );
			}
		} catch ( FileNotFoundException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setI2B2Output( String outDir ) {
		i2b2Output = outDir;
	}

	public void prepareXMIsFor( List<Integer> patientSets ) throws Exception {
		boolean needsXMIs = false;
		for ( File textFile : this.getFilesFor( patientSets ) ) {
			if ( !getXMIFile( this.xmiDirectory, textFile ).exists() ) {
				needsXMIs = true;
				break;
			}
		}
		if ( needsXMIs ) {
			CollectionReader reader = this.getCollectionReader( patientSets );
			AnalysisEngine engine = this.getXMIWritingPreprocessorAggregateBuilder().createAggregate();
			SimplePipeline.runPipeline( reader, engine );
		}
		this.xmiExists = true;
	}

	private List<File> getFilesFor( List<Integer> patientSets ) throws FileNotFoundException {
		List<File> files = new ArrayList<>();
		if ( this.xmlFormat == XMLFormat.Anafora ) {
			Set<String> ids = new HashSet<>();
			for ( Integer set : patientSets ) {
				if ( this.subcorpus == Subcorpus.Colon ) {
					ids.add( String.format( "ID%03d", set ) );
				} else if ( this.subcorpus == Subcorpus.DeepPhe ) {
					ids.add( String.format( "patient%02d", set ) );
				} else {
					ids.add( String.format( "doc%04d", set ) );
				}
			}
			int filePrefixLen = 5; // Colon: "ID\d{3}"
			if ( this.subcorpus == Subcorpus.Brain ) {
				filePrefixLen = 7; // Brain: "doc\d{4}"
			} else if ( this.subcorpus == Subcorpus.DeepPhe ) {
				filePrefixLen = 9; // deepPhe: "patient\d{2}"
			}
			if ( this.subcorpus == Subcorpus.DeepPhe ) {
				for ( File dir : this.xmlDirectory.listFiles() ) {
					if ( dir.isDirectory() ) {
						if ( ids.contains( dir.getName().substring( 0, filePrefixLen ) ) ) {
							File file = new File( dir, dir.getName() );
							if ( file.exists() ) {
								files.add( file );
							} else {
								LOGGER.warn( "Missing note: " + file );
							}
						}
					}
				}
			} else {
				for ( String section : THYMEData.SECTIONS ) {
					File xmlSubdir = new File( this.xmlDirectory, section );
					for ( File dir : xmlSubdir.listFiles() ) {
						if ( dir.isDirectory() ) {
							if ( ids.contains( dir.getName().substring( 0, filePrefixLen ) ) ) {
								File file = new File( dir, dir.getName() );
								if ( file.exists() ) {
									files.add( file );
								} else {
									LOGGER.warn( "Missing note: " + file );
								}
							}
						}
					}
				}
			}
		} else if ( this.xmlFormat == XMLFormat.AnaforaCoref) {
			Set<String> ids = new HashSet<>();
			for (Integer set : patientSets) {
				if (this.subcorpus == Subcorpus.Colon) {
					ids.add(String.format("ID%03d", set));
				} else {
					LOGGER.warn("No coreference annotations exist for this corpus!");
				}
			}
			for (File dir : this.xmlDirectory.listFiles()) {
				// this gets us into train/dev/test subdirectory
				for (File ptDir : dir.listFiles()) {
					if (ids.contains(ptDir.getName())) {
						for (File subDir : ptDir.listFiles()) {
							if (subDir.isDirectory()) {
								// for document 001 for patient 001, directory is ID001/ID001_clinic_001
								// and text file within is ID001_clinic_001
								files.add(new File(subDir, subDir.getName()));
							}
						}
					}
				}
			}
		} else if ( this.xmlFormat == XMLFormat.I2B2 ) {
			File trainDir = new File( this.xmlDirectory, "training" );
			File testDir = new File( this.xmlDirectory, "test" );
			for ( Integer pt : patientSets ) {
				File xmlTrain = new File( trainDir, pt + ".xml" );
				File train = new File( trainDir, pt + ".xml.txt" );
				if ( train.exists() ) {
					if ( xmlTrain.exists() ) {
						files.add( train );
					} else {
						System.err.println( "Text file in training has no corresponding xml -- skipping: " + train );
					}
				}
				File xmlTest = new File( testDir, pt + ".xml" );
				File test = new File( testDir, pt + ".xml.txt" );
				if ( xmlTest.exists() ) {
					if ( test.exists() ) {
						files.add( test );
					} else {
						throw new FileNotFoundException( "Could not find the test text file -- for cTAKES usage you must copy the text files into the xml directory for the test set." );
					}
				}
				assert !(train.exists() && test.exists());
			}
		} else if ( xmlFormat == XMLFormat.Knowtator ) {
			LOGGER.warn( "This is an old annotation format -- please upgrade to using anafora files." );
			for ( Integer set : patientSets ) {
				final int setNum = set;
				for ( File file : rawTextDirectory.listFiles( new FilenameFilter() {
					@Override
					public boolean accept( File dir, String name ) {
						return name.contains( String.format( "ID%03d", setNum ) );
					}
				} ) ) {
					// skip hidden files like .svn
					if ( !file.isHidden() ) {
						files.add( file );
					}
				}
			}
		} else {
			LOGGER.error( "Unknown data format -- please specify Anafora, i2b2, or Knowtator format." );
		}
		return files;
	}

	@Override
	protected CollectionReader getCollectionReader( List<Integer> patientSets ) throws Exception {
		List<File> collectedFiles = this.getFilesFor( patientSets );
		Collections.sort(collectedFiles);

		CounterMap<String> docCounts = new CounterMap<>();
		for(File f : collectedFiles){
			String ptidPrefix = null;
			if(this.subcorpus == Subcorpus.Colon || this.subcorpus == Subcorpus.Brain){
				ptidPrefix = f.getName().split("_")[0];
			}else{
				throw new UnsupportedOperationException("No prefix extraction method implemented in Evaluation_ImplBase collection reader getter.");
			}
			docCounts.add(ptidPrefix);
		}
        docCounts.forEach( PatientNoteStore.getInstance()::setWantedDocCount );

		return UriCollectionReader.getCollectionReaderFromFiles( collectedFiles );
	}

	protected AggregateBuilder getPreprocessorAggregateBuilder() throws Exception {
		return this.xmiExists
				? this.getXMIReadingPreprocessorAggregateBuilder()
						: this.getXMIWritingPreprocessorAggregateBuilder();
	}

	protected AggregateBuilder getXMIReadingPreprocessorAggregateBuilder() throws UIMAException {
		AggregateBuilder aggregateBuilder = new AggregateBuilder();
		// TODO: Is this necessary? Doesn't the default view have the text populated in the xmis?
		aggregateBuilder.add( UriToDocumentTextAnnotator.getDescription() );
		aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
				XMIReader.class,
				XMIReader.PARAM_XMI_DIRECTORY,
				this.xmiDirectory ) );
		return aggregateBuilder;
	}

	protected AggregateBuilder getXMIWritingPreprocessorAggregateBuilder()
			throws Exception {
		AggregateBuilder aggregateBuilder = new AggregateBuilder();
		aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription( UriToDocumentTextAnnotatorCtakes.class ) );
		aggregateBuilder.add( getGoldWritingAggregate() );
		aggregateBuilder.add( getLinguisticProcessingDescription() );
		
		// write out the CAS after all the above annotations
		aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
				XMIWriter.class,
				XMIWriter.PARAM_XMI_DIRECTORY,
				this.xmiDirectory ) );

		return aggregateBuilder;
	}

	protected AnalysisEngineDescription getGoldWritingAggregate() throws Exception {
	  return getGoldWritingAggregate(GOLD_VIEW_NAME);
	}
	
	protected AnalysisEngineDescription getGoldWritingAggregate(String goldViewName) throws Exception {
    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    // read manual annotations into gold view
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        ViewCreatorAnnotator.class,
        ViewCreatorAnnotator.PARAM_VIEW_NAME,
        goldViewName ) );
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        ViewTextCopierAnnotator.class,
        ViewTextCopierAnnotator.PARAM_SOURCE_VIEW_NAME,
        CAS.NAME_DEFAULT_SOFA,
        ViewTextCopierAnnotator.PARAM_DESTINATION_VIEW_NAME,
        goldViewName ) );
    switch ( this.xmlFormat ) {
    case AnaforaCoref:
    	// do nothing: only runs at the end of the patient
		break;
    case Anafora:
      if(this.subcorpus == Subcorpus.DeepPhe){
        aggregateBuilder.add(
            AnalysisEngineFactory.createEngineDescription(THYMEAnaforaXMLReader.class,
                THYMEAnaforaXMLReader.PARAM_ANAFORA_DIRECTORY,
                this.xmlDirectory,
                THYMEAnaforaXMLReader.PARAM_ANAFORA_XML_SUFFIXES,
                new String[]{} ),
                CAS.NAME_DEFAULT_SOFA,
                goldViewName );
      }else{
        aggregateBuilder.add(
            THYMEAnaforaXMLReader.getDescription( this.xmlDirectory ),
            CAS.NAME_DEFAULT_SOFA,
            goldViewName );
      }
      break;
    case Knowtator:
      aggregateBuilder.add(
          THYMEKnowtatorXMLReader.getDescription( this.xmlDirectory ),
          CAS.NAME_DEFAULT_SOFA,
          goldViewName );
      break;
    case I2B2:
      aggregateBuilder.add(
          I2B2TemporalXMLReader.getDescription( this.xmlDirectory ),
          CAS.NAME_DEFAULT_SOFA,
          goldViewName );
      break;
    }
    return aggregateBuilder.createAggregateDescription();
	}
	
	protected AnalysisEngineDescription getLinguisticProcessingDescription() throws Exception{
	  AggregateBuilder aggregateBuilder = new AggregateBuilder();
    // identify segments
    if(this.subcorpus == Subcorpus.DeepPhe){
      aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(PittHeaderAnnotator.class));
    }else{
      aggregateBuilder
      .add( AnalysisEngineFactory.createEngineDescription( SegmentsFromBracketedSectionTagsAnnotator.class ) );
    }
    // identify sentences
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        SentenceDetector.class,
        SentenceDetector.SD_MODEL_FILE_PARAM,
        "org/apache/ctakes/core/models/sentdetect/sd-med-model.zip" ) );
    //      aggregateBuilder.add(SentenceDetectorAnnotatorBIO.getDescription(FileLocator.locateFile("org/apache/ctakes/core/sentdetect/model.jar").getPath()));

    // identify tokens
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription( TokenizerAnnotatorPTB.class ) );
    // merge some tokens
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription( ContextDependentTokenizerAnnotator.class ) );

    // identify part-of-speech tags
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        POSTagger.class,
        TypeSystemDescriptionFactory.createTypeSystemDescription(),
        TypePrioritiesFactory.createTypePriorities( Segment.class, Sentence.class, BaseToken.class ),
        POSTagger.POS_MODEL_FILE_PARAM,
        "org/apache/ctakes/postagger/models/mayo-pos.zip" ) );

    // identify chunks
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        Chunker.class,
        Chunker.CHUNKER_MODEL_FILE_PARAM,
        FileLocator.getFile( "org/apache/ctakes/chunker/models/chunker-model.zip" ),
        Chunker.CHUNKER_CREATOR_CLASS_PARAM,
        DefaultChunkCreator.class ) );

    // identify UMLS named entities

    // adjust NP in NP NP to span both
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        ChunkAdjuster.class,
        ChunkAdjuster.PARAM_CHUNK_PATTERN,
        new String[] { "NP", "NP" },
        ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN,
        1 ) );
    // adjust NP in NP PP NP to span all three
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        ChunkAdjuster.class,
        ChunkAdjuster.PARAM_CHUNK_PATTERN,
        new String[] { "NP", "PP", "NP" },
        ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN,
        2 ) );
    // add lookup windows for each NP
    aggregateBuilder
    .add( AnalysisEngineFactory.createEngineDescription( CopyNPChunksToLookupWindowAnnotations.class ) );
    // maximize lookup windows
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(
        OverlapAnnotator.class,
        "A_ObjectClass",
        LookupWindowAnnotation.class,
        "B_ObjectClass",
        LookupWindowAnnotation.class,
        "OverlapType",
        "A_ENV_B",
        "ActionType",
        "DELETE",
        "DeleteAction",
        new String[] { "selector=B" } ) );
    // add UMLS on top of lookup windows
    aggregateBuilder.add( LvgAnnotator.createAnnotatorDescription() );
    aggregateBuilder.add( DefaultJCasTermAnnotator.createAnnotatorDescription() );

    // add dependency parser
    aggregateBuilder.add( ClearNLPDependencyParserAE.createAnnotatorDescription() );

    // add semantic role labeler
//    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription( ClearNLPSemanticRoleLabelerAE.class ) );

    // add gold standard parses to gold view, and adjust gold view to correct a few annotation mis-steps
    if ( this.treebankDirectory != null ) {
      aggregateBuilder.add( THYMETreebankReader.getDescription( this.treebankDirectory ) );
      aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription( TimexAnnotationCorrector.class ) );
    } else {
      // add ctakes constituency parses to system view
      aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription( ConstituencyParser.class,
          ConstituencyParser.PARAM_MODEL_FILENAME,
          "org/apache/ctakes/constituency/parser/models/thyme.bin" ) );
    }
	  
	  return aggregateBuilder.createAggregateDescription();
	}
	
	public static <T extends Annotation> List<T> selectExact( JCas jCas, Class<T> annotationClass, Segment segment ) {
		List<T> annotations = Lists.newArrayList();
		for ( T annotation : JCasUtil.selectCovered( jCas, annotationClass, segment ) ) {
			if ( annotation.getClass().equals( annotationClass ) ) {
				annotations.add( annotation );
			}
		}
		return annotations;
	}

	@PipeBitInfo(
			name = "NP Lookup Window Creator",
			description = "Creates a Lookup Window from Noun Phrase Chunks.",
			role = PipeBitInfo.Role.SPECIAL,
			dependencies = { PipeBitInfo.TypeProduct.CHUNK }
	)
	public static class CopyNPChunksToLookupWindowAnnotations extends JCasAnnotator_ImplBase {

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			for ( Chunk chunk : JCasUtil.select( jCas, Chunk.class ) ) {
				if ( chunk.getChunkType().equals( "NP" ) ) {
					new LookupWindowAnnotation( jCas, chunk.getBegin(), chunk.getEnd() ).addToIndexes();
				}
			}
		}
	}

	@PipeBitInfo(
			name = "Overlap Lookup Window Remover",
			description = "Removes Lookup Windows that are within larger Lookup Windows.",
			role = PipeBitInfo.Role.SPECIAL
	)
	public static class RemoveEnclosedLookupWindows extends JCasAnnotator_ImplBase {

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			List<LookupWindowAnnotation> lws = new ArrayList<>( JCasUtil.select( jCas, LookupWindowAnnotation.class ) );
			// we'll navigate backwards so that as we delete things we shorten the list from the back
			for ( int i = lws.size() - 2; i >= 0; i-- ) {
				LookupWindowAnnotation lw1 = lws.get( i );
				LookupWindowAnnotation lw2 = lws.get( i + 1 );
				if ( lw1.getBegin() <= lw2.getBegin() && lw1.getEnd() >= lw2.getEnd() ) {
					/// lw1 envelops or encloses lw2
					lws.remove( i + 1 );
					lw2.removeFromIndexes();
				}
			}

		}

	}

	public static class EntityMentionRemover extends JCasAnnotator_ImplBase {

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			for ( EntityMention mention : Lists.newArrayList( JCasUtil.select( jCas, EntityMention.class ) ) ) {
				mention.removeFromIndexes();
			}
		}
	}

	public static class EventMentionRemover extends JCasAnnotator_ImplBase {

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			for ( EventMention mention : Lists.newArrayList( JCasUtil.select( jCas, EventMention.class ) ) ) {
				mention.removeFromIndexes();
			}
		}
	}

	// replace this with SimpleSegmentWithTagsAnnotator if that code ever gets fixed
	public static class SegmentsFromBracketedSectionTagsAnnotator extends JCasAnnotator_ImplBase {
		private static Pattern SECTION_PATTERN = Pattern.compile(
				"(\\[start section id=\"?(.*?)\"?\\]).*?(\\[end section id=\"?(.*?)\"?\\])",
				Pattern.DOTALL );

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			boolean foundSections = false;
			Matcher matcher = SECTION_PATTERN.matcher( jCas.getDocumentText() );
			while ( matcher.find() ) {
				Segment segment = new Segment( jCas );
				segment.setBegin( matcher.start() + matcher.group( 1 ).length() );
				segment.setEnd( matcher.end() - matcher.group( 3 ).length() );
				segment.setId( matcher.group( 2 ) );
				segment.addToIndexes();
				foundSections = true;
			}
			if ( !foundSections ) {
				Segment segment = new Segment( jCas );
				segment.setBegin( 0 );
				segment.setEnd( jCas.getDocumentText().length() );
				segment.setId( "SIMPLE_SEGMENT" );
				segment.addToIndexes();
			}
		}
	}

	/**
	 * Grabs the document time from the header
	 */
	public static class PittHeaderAnnotator extends JCasAnnotator_ImplBase {

		/**
		 * Grabs the document time from the header
		 * {@inheritDoc}
		 */
		@Override
		public void process( final JCas jcas ) throws AnalysisEngineProcessException {
			String docText = jcas.getDocumentText();
			int headerEnd = docText.indexOf("\n", docText.indexOf("[Report de-identified"));
			Segment mainSegment = new Segment(jcas, headerEnd+1, docText.length()-1);
			mainSegment.setId("SIMPLE_SEGMENT");
			mainSegment.addToIndexes();
		}
	}

	static File getXMIFile( File xmiDirectory, File textFile ) {
		String fileName = textFile.getName();
		if(!fileName.contains(".xmi")){
			fileName += ".xmi";
		}
		return new File( xmiDirectory, fileName);// + ".xmi" 
	}

	static File getXMIFile( File xmiDirectory, JCas jCas ) throws AnalysisEngineProcessException {
		return getXMIFile( xmiDirectory, new File( ViewUriUtil.getURI( jCas ).getPath() ) );
	}

	public static class XMIWriter extends JCasAnnotator_ImplBase {

		public static final String PARAM_XMI_DIRECTORY = "XMIDirectory";

		@ConfigurationParameter( name = PARAM_XMI_DIRECTORY, mandatory = true )
		private File xmiDirectory;

		@Override
		public void initialize( UimaContext context ) throws ResourceInitializationException {
			super.initialize( context );
			if ( !this.xmiDirectory.exists() ) {
				this.xmiDirectory.mkdirs();
			}
		}

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			File xmiFile = getXMIFile( this.xmiDirectory, jCas );
			try {
				FileOutputStream outputStream = new FileOutputStream( xmiFile );
				try {
					XmiCasSerializer serializer = new XmiCasSerializer( jCas.getTypeSystem() );
					ContentHandler handler = new XMLSerializer( outputStream, false ).getContentHandler();
					serializer.serialize( jCas.getCas(), handler );
				} finally {
					outputStream.close();
				}
			} catch ( SAXException e ) {
				throw new AnalysisEngineProcessException( e );
			} catch ( IOException e ) {
				throw new AnalysisEngineProcessException( e );
			}
		}
	}

	public static class XMIReader extends JCasAnnotator_ImplBase {

		public static final String PARAM_XMI_DIRECTORY = "XMIDirectory";

		@ConfigurationParameter( name = PARAM_XMI_DIRECTORY, mandatory = true )
		private File xmiDirectory;

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			File xmiFile = getXMIFile( this.xmiDirectory, jCas );
			try {
				FileInputStream inputStream = new FileInputStream( xmiFile );
				try {
					XmiCasDeserializer.deserialize( inputStream, jCas.getCas() );
				} finally {
					inputStream.close();
				}
			} catch ( SAXException e ) {
				throw new AnalysisEngineProcessException( e );
			} catch ( IOException e ) {
				throw new AnalysisEngineProcessException( e );
			}
		}
	}

	public static class TimexAnnotationCorrector extends JCasAnnotator_ImplBase {
		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			JCas goldView, systemView;
			try {
				goldView = jCas.getView( GOLD_VIEW_NAME );
				systemView = jCas.getView( CAS.NAME_DEFAULT_SOFA );
			} catch ( CASException e ) {
				e.printStackTrace();
				throw new AnalysisEngineProcessException();
			}
			for ( TimeMention mention : JCasUtil.select( goldView, TimeMention.class ) ) {
				// for each time expression, get the treebank node with the same span.
				List<TreebankNode> nodes = JCasUtil.selectCovered( systemView, TreebankNode.class, mention );
				TreebankNode sameSpanNode = null;
				for ( TreebankNode node : nodes ) {
					if ( node.getBegin() == mention.getBegin() && node.getEnd() == mention.getEnd() ) {
						sameSpanNode = node;
						break;
					}
				}
				if ( sameSpanNode != null ) {
					// look at node at the position of the timex3.
					if ( sameSpanNode.getNodeType().equals( "PP" ) ) {
						// if it is a PP it should be moved down to the NP
						int numChildren = sameSpanNode.getChildren().size();
						if ( numChildren == 2 && sameSpanNode.getChildren( 0 ).getNodeType().equals( "IN" ) &&
								sameSpanNode.getChildren( 1 ).getNodeType().equals( "NP" ) ) {
							// move the time span to this node:
							TreebankNode mentionNode = sameSpanNode.getChildren( numChildren - 1 );
							mention.setBegin( mentionNode.getBegin() );
							mention.setEnd( mentionNode.getEnd() );
						}
					}
				} else {
					// if there is no matching tree span, see if the DT to the left would help.
					// now adjust for missing DT to the left
					List<TerminalTreebankNode> precedingPreterms = JCasUtil
							.selectPreceding( systemView, TerminalTreebankNode.class, mention, 1 );
					if ( precedingPreterms != null && precedingPreterms.size() == 1 ) {
						TerminalTreebankNode leftTerm = precedingPreterms.get( 0 );
						if ( leftTerm.getNodeType().equals( "DT" ) ) {
							// now see if adding this would make it match a tree
							List<TreebankNode> matchingNodes = JCasUtil
									.selectCovered( systemView, TreebankNode.class, leftTerm.getBegin(), mention.getEnd() );
							for ( TreebankNode node : matchingNodes ) {
								if ( node.getBegin() == leftTerm.getBegin() && node.getEnd() == mention.getEnd() ) {
									sameSpanNode = node;
									break;
								}
							}
							if ( sameSpanNode != null ) {
								// adding the DT to the left of th emention made it match a tree:
									System.err.println(
											"Adding DT: " + leftTerm.getCoveredText() + " to TIMEX: " + mention.getCoveredText() );
									mention.setBegin( leftTerm.getBegin() );
							}
						}
					}
				}
			}
		}
	}


	@PipeBitInfo(
			name = "Gold Annotation Copier",
			description = "Copies an annotation type from the Gold view to the System view.",
			role = PipeBitInfo.Role.SPECIAL
	)
	public static class CopyFromGold extends JCasAnnotator_ImplBase {

		public static AnalysisEngineDescription getDescription( Class<?>... classes )
				throws ResourceInitializationException {
			return AnalysisEngineFactory.createEngineDescription(
					CopyFromGold.class,
					CopyFromGold.PARAM_ANNOTATION_CLASSES,
					classes );
		}

		public static final String PARAM_ANNOTATION_CLASSES = "AnnotationClasses";

		@ConfigurationParameter( name = PARAM_ANNOTATION_CLASSES, mandatory = true )
		private Class<? extends TOP>[] annotationClasses;

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			JCas goldView, systemView;
			try {
				goldView = jCas.getView( GOLD_VIEW_NAME );
				systemView = jCas.getView( CAS.NAME_DEFAULT_SOFA );
			} catch ( CASException e ) {
				throw new AnalysisEngineProcessException( e );
			}
			for ( Class<? extends TOP> annotationClass : this.annotationClasses ) {
				for ( TOP annotation : Lists.newArrayList( JCasUtil.select( systemView, annotationClass ) ) ) {
					if ( annotation.getClass().equals( annotationClass ) ) {
						annotation.removeFromIndexes();
					}
				}
			}
			CasCopier copier = new CasCopier( goldView.getCas(), systemView.getCas() );
			Feature sofaFeature = jCas.getTypeSystem().getFeatureByFullName( CAS.FEATURE_FULL_NAME_SOFA );
			for ( Class<? extends TOP> annotationClass : this.annotationClasses ) {
				for ( TOP annotation : JCasUtil.select( goldView, annotationClass ) ) {
					TOP copy = (TOP)copier.copyFs( annotation );
					if ( copy instanceof Annotation ) {
						copy.setFeatureValue( sofaFeature, systemView.getSofa() );
					}
					copy.addToIndexes( systemView );
				}
			}
		}
	}

	@PipeBitInfo(
			name = "System Annotation Copier",
			description = "Copies an annotation type from the System view to a Gold view.",
			role = PipeBitInfo.Role.SPECIAL
	)
	public static class CopyFromSystem extends JCasAnnotator_ImplBase {

		public static AnalysisEngineDescription getDescription( Class<?>... classes )
				throws ResourceInitializationException {
			return AnalysisEngineFactory.createEngineDescription(
					CopyFromSystem.class,
					CopyFromSystem.PARAM_ANNOTATION_CLASSES,
					classes );
		}

		public static final String PARAM_ANNOTATION_CLASSES = "AnnotationClasses";

		@ConfigurationParameter( name = PARAM_ANNOTATION_CLASSES, mandatory = true )
		private Class<? extends TOP>[] annotationClasses;

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			JCas goldView, systemView;
			try {
				goldView = jCas.getView( GOLD_VIEW_NAME );
				systemView = jCas.getView( CAS.NAME_DEFAULT_SOFA );
			} catch ( CASException e ) {
				throw new AnalysisEngineProcessException( e );
			}
			for ( Class<? extends TOP> annotationClass : this.annotationClasses ) {
				for ( TOP annotation : Lists.newArrayList( JCasUtil.select( goldView, annotationClass ) ) ) {
					if ( annotation.getClass().equals( annotationClass ) ) {
						annotation.removeFromIndexes();
					}
				}
			}
			CasCopier copier = new CasCopier( systemView.getCas(), goldView.getCas() );
			Feature sofaFeature = jCas.getTypeSystem().getFeatureByFullName( CAS.FEATURE_FULL_NAME_SOFA );
			for ( Class<? extends TOP> annotationClass : this.annotationClasses ) {
				for ( TOP annotation : JCasUtil.select( systemView, annotationClass ) ) {
					TOP copy = (TOP)copier.copyFs( annotation );
					if ( copy instanceof Annotation ) {
						copy.setFeatureValue( sofaFeature, goldView.getSofa() );
					}
					copy.addToIndexes( goldView );
				}
			}
		}
	}

	/*
	 * The following class overrides a ClearTK utility annotator class for reading
	 * a text file into a JCas. The code is copy/pasted so that one tiny modification
	 * can be made for this corpus -- replace a single odd character (0xc) with a
	 * space since it trips up xml output.
	 */
	public static class UriToDocumentTextAnnotatorCtakes extends UriToDocumentTextAnnotator {

		@Override
		public void process( JCas jCas ) throws AnalysisEngineProcessException {
			URI uri = ViewUriUtil.getURI( jCas );
			String content;
			try {
			  content = CharStreams.toString( new InputStreamReader( uri.toURL().openStream() ) );
			  content = content.replace( (char)0xc, ' ' );
			  jCas.setSofaDataString( content, "text/plain" );
			} catch ( MalformedURLException e ) {
			  throw new AnalysisEngineProcessException( e );
			} catch ( IOException e ) {
			  throw new AnalysisEngineProcessException( e );
			}
		}
	}

	public static class WriteI2B2XML extends JCasAnnotator_ImplBase {
		public static final String PARAM_OUTPUT_DIR = "PARAM_OUTPUT_DIR";
		@ConfigurationParameter( mandatory = true, description = "Output directory to write xml files to.", name = PARAM_OUTPUT_DIR )
		protected String outputDir;

		@Override
		public void process( JCas jcas ) throws AnalysisEngineProcessException {
			try {
				// get the output file name from the input file name and output directory.
				File outDir = new File( outputDir );
				if ( !outDir.exists() ) {
					outDir.mkdirs();
				}
				File inFile = new File( ViewUriUtil.getURI( jcas ) );
				String outFile = inFile.getName().replace( ".txt", "" );

				// build the xml
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				Document doc = docBuilder.newDocument();
				Element rootElement = doc.createElement( "ClinicalNarrativeTemporalAnnotation" );
				Element textElement = doc.createElement( "TEXT" );
				Element tagsElement = doc.createElement( "TAGS" );
				textElement.setTextContent( jcas.getDocumentText() );
				rootElement.appendChild( textElement );
				rootElement.appendChild( tagsElement );
				doc.appendChild( rootElement );

				Map<IdentifiedAnnotation, String> argToId = new HashMap<>();
				int id = 0;
				for ( TimeMention timex : JCasUtil.select( jcas, TimeMention.class ) ) {
					Element timexElement = doc.createElement( "TIMEX3" );
					String timexID = "T" + id;
					id++;
					argToId.put( timex, timexID );
					timexElement.setAttribute( "id", timexID );
					timexElement.setAttribute( "start", String.valueOf( timex.getBegin() + 1 ) );
					timexElement.setAttribute( "end", String.valueOf( timex.getEnd() + 1 ) );
					timexElement.setAttribute( "text", timex.getCoveredText() );
					timexElement.setAttribute( "type", "NA" );
					timexElement.setAttribute( "val", "NA" );
					timexElement.setAttribute( "mod", "NA" );
					tagsElement.appendChild( timexElement );
				}

				id = 0;
				for ( EventMention event : JCasUtil.select( jcas, EventMention.class ) ) {
					if ( event.getClass().equals( EventMention.class ) ) {
						// this ensures we are only looking at THYME events and not ctakes-dictionary-lookup events
						Element eventEl = doc.createElement( "EVENT" );
						String eventID = "E" + id;
						id++;
						argToId.put( event, eventID );
						eventEl.setAttribute( "id", eventID );
						eventEl.setAttribute( "start", String.valueOf( event.getBegin() + 1 ) );
						eventEl.setAttribute( "end", String.valueOf( event.getEnd() + 1 ) );
						eventEl.setAttribute( "text", event.getCoveredText() );
						eventEl.setAttribute( "modality", "NA" );
						eventEl.setAttribute( "polarity", "NA" );
						eventEl.setAttribute( "type", "NA" );
						tagsElement.appendChild( eventEl );
					}
				}

				id = 0;
				for ( TemporalTextRelation rel : JCasUtil.select( jcas, TemporalTextRelation.class ) ) {
					Element linkEl = doc.createElement( "TLINK" );
					String linkID = "TL" + id;
					id++;
					linkEl.setAttribute( "id", linkID );
					Annotation arg1 = rel.getArg1().getArgument();
					linkEl.setAttribute( "fromID", argToId.get( arg1 ) );
					linkEl.setAttribute( "fromText", arg1.getCoveredText() );
					Annotation arg2 = rel.getArg2().getArgument();
					if ( arg2 != null ) {
						linkEl.setAttribute( "toID", argToId.get( arg2 ) );
						linkEl.setAttribute( "toText", arg2.getCoveredText() );
					} else {
						linkEl.setAttribute( "toID", "Discharge" );
						linkEl.setAttribute( "toText", "Discharge" );
					}
					linkEl.setAttribute( "type", rel.getCategory() );
					tagsElement.appendChild( linkEl );
				}

				// boilerplate xml-writing code:
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
				transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
				DOMSource source = new DOMSource( doc );
				StreamResult result = new StreamResult( new File( outputDir, outFile ) );
				transformer.transform( source, result );
			} catch ( ParserConfigurationException e ) {
				e.printStackTrace();
				throw new AnalysisEngineProcessException( e );
			} catch ( TransformerConfigurationException e ) {
				e.printStackTrace();
				throw new AnalysisEngineProcessException( e );
			} catch ( TransformerException e ) {
				e.printStackTrace();
				throw new AnalysisEngineProcessException( e );
			}

		}

	}

	public static class WriteAnaforaXML extends JCasAnnotator_ImplBase {
		public static final String PARAM_OUTPUT_DIR = "PARAM_OUTPUT_DIR";
		@ConfigurationParameter( mandatory = true, description = "Output directory to write xml files to.", name = PARAM_OUTPUT_DIR )
		protected String outputDir;

		public static final String PARAM_PROB_VIEW = "ProbView";
		@ConfigurationParameter(name=PARAM_PROB_VIEW, mandatory=false)
		public String probViewname = null;

		@Override
		public void process( JCas jcas ) throws AnalysisEngineProcessException {
			try {
				// get the output file name from the input file name and output directory.

				File inFile = new File( ViewUriUtil.getURI( jcas ) );
				String outFile = inFile.getName().replace( ".txt", "" );
				File outDir = new File( outputDir, outFile );
				if ( !outDir.exists() ) {
					outDir.mkdirs();
				}


				// get maps from ids to entities and relations:
				JCas probView = (probViewname == null ? null : jcas.getView(probViewname));
				Map<Integer, List<EventMention>> mentions = probViewname == null? null : getMentionIdMap(jcas, probView);
				Map<String, List<TemporalTextRelation>> rels = probViewname == null ? null : getRelationIdMap(probView);

				// build the xml
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				Document doc = docBuilder.newDocument();

				Element rootElement = doc.createElement( "data" );

				//info element
				Element infoElement = doc.createElement( "info" );
				Element saveTime = doc.createElement( "savetime" );
				saveTime.setTextContent( "2015-0123-10:21" );
				Element progress = doc.createElement( "progress" );
				progress.setTextContent( "completed" );
				infoElement.appendChild( saveTime );
				infoElement.appendChild( progress );

				//schema element
				Element schema = doc.createElement( "schema" );
				schema.setAttribute( "path", "./" );
				schema.setAttribute( "protocol", "file" );
				schema.setTextContent( "temporal-schema.xml" );

				Element annoElement = doc.createElement( "annotations" );
				Map<IdentifiedAnnotation, String> argToId = new HashMap<>();
				int id = 1;
				for ( EventMention event : JCasUtil.select( jcas, EventMention.class ) ) {
					if ( event.getClass().equals( EventMention.class ) ) {
						// this ensures we are only looking at THYME events and not ctakes-dictionary-lookup events
						Element eventEl = doc.createElement( "entity" );
						String eventID = id + "@e@" + outFile + "@system";
						id++;
						argToId.put( event, eventID );
						Element idE = doc.createElement( "id" );
						idE.setTextContent( eventID );
						Element spanE = doc.createElement( "span" );
						spanE.setTextContent( String.valueOf( event.getBegin() ) + "," + String.valueOf( event.getEnd() ) );
						Element typeE = doc.createElement( "type" );
						typeE.setTextContent( "EVENT" );
						Element parentTE = doc.createElement( "parentsType" );
						parentTE.setTextContent( "TemporalEntities" );
						//add properties
						Element property = doc.createElement( "properties" );
						Element docTimeRE = doc.createElement( "DocTimeRel" );
						String dtrContent = null;
						if(probViewname == null){
							dtrContent = event.getEvent().getProperties().getDocTimeRel();
						}else{
							StringBuffer buff = new StringBuffer();
							for(EventMention probMention : mentions.get(event.getId())){
								buff.append(probMention.getEvent().getProperties().getDocTimeRel());
								buff.append(':');
								buff.append(probMention.getConfidence());
								buff.append("::");
							}
							dtrContent = buff.substring(0, buff.length()-2);
						}
						docTimeRE.setTextContent( dtrContent );
						Element eventTypeE = doc.createElement( "Type" );
						eventTypeE.setTextContent( "N/A" );
						Element degreeE = doc.createElement( "Degree" );
						degreeE.setTextContent( "N/A" );
						Element polarityE = doc.createElement( "Polarity" );
						String polarity = "UNKNOWN";
						int polarityInt = event.getPolarity();
						if ( polarityInt == CONST.NE_POLARITY_NEGATION_ABSENT ) {
							polarity = "POS";
						} else if ( polarityInt == CONST.NE_POLARITY_NEGATION_PRESENT ) {
							polarity = "NEG";
						}
						polarityE.setTextContent( polarity );
						Element ctexModE = doc.createElement( "ContextualModality" );
						ctexModE.setTextContent( event.getEvent().getProperties().getContextualModality() );
						Element ctexAspE = doc.createElement( "ContextualAspect" );
						ctexAspE.setTextContent( event.getEvent().getProperties().getContextualAspect() );
						Element permE = doc.createElement( "Permanence" );
						permE.setTextContent( "UNDETERMINED" );
						property.appendChild( docTimeRE );
						property.appendChild( polarityE );
						property.appendChild( degreeE );
						property.appendChild( eventTypeE );
						property.appendChild( ctexModE );
						property.appendChild( ctexAspE );
						property.appendChild( permE );
						eventEl.appendChild( idE );
						eventEl.appendChild( spanE );
						eventEl.appendChild( typeE );
						eventEl.appendChild( parentTE );
						eventEl.appendChild( property );
						annoElement.appendChild( eventEl );
					}
				}
				for ( TimeMention timex : JCasUtil.select( jcas, TimeMention.class ) ) {
					Element timexElement = doc.createElement( "entity" );
					String timexID = id + "@e@" + outFile + "@system";
					id++;//18@e@ID006_clinic_016@gold
					argToId.put( timex, timexID );
					Element idE = doc.createElement( "id" );
					idE.setTextContent( timexID );
					Element spanE = doc.createElement( "span" );
					spanE.setTextContent( String.valueOf( timex.getBegin() ) + "," + String.valueOf( timex.getEnd() ) );
					Element typeE = doc.createElement( "type" );
					Element parentTE = doc.createElement( "parentsType" );
					parentTE.setTextContent( "TemporalEntities" );
					//add properties
					Element property = doc.createElement( "properties" );
					String timeClass = timex.getTimeClass();

					//add normalized timex
					try{
						String value = Utils.getTimexMLValue(timex.getCoveredText());
						property.setTextContent( value );
					}catch (Exception e) {
						LOGGER.error("time norm error: "+ e.getMessage());
					}

					if ( timeClass!=null && (timeClass.equals( "DOCTIME" ) || timeClass.equals( "SECTIONTIME" ) ) ) {
						typeE.setTextContent( timeClass );
						property.setTextContent( "" );
					} else {
						typeE.setTextContent( "TIMEX3" );
						Element classE = doc.createElement( "Class" );
						classE.setTextContent( timeClass );
						property.appendChild( classE );
					}

					timexElement.appendChild( idE );
					timexElement.appendChild( spanE );
					timexElement.appendChild( typeE );
					timexElement.appendChild( property );
					annoElement.appendChild( timexElement );
				}


				id = 1;
				if(probViewname == null){
					for ( TemporalTextRelation rel : JCasUtil.select( jcas, TemporalTextRelation.class ) ) {
						Annotation arg1 = rel.getArg1().getArgument();
						Annotation arg2 = rel.getArg2().getArgument();
						String arg1Content = argToId.get( arg1 );
						String arg2Content = argToId.get( arg2 );
						String relContent = rel.getCategory();
						annoElement.appendChild(addRelationElement(doc, id, relContent, arg1Content, arg2Content, outFile));
						id++;
					}
				}else{
					// need to keep track of which relations we've printed since they don't get grouped in the CAS
					for(String key : rels.keySet()){
						String arg1Content = null;
						String arg2Content = null;
						StringBuffer buff = new StringBuffer();
						for(TemporalTextRelation probRel : rels.get(key)){
							buff.append(probRel.getCategory());
							buff.append(':');
							buff.append(probRel.getConfidence());
							buff.append("::");
							if(arg1Content == null){
								arg1Content = argToId.get(probRel.getArg1().getArgument());
								arg2Content = argToId.get(probRel.getArg2().getArgument());
							}
						}
						String relContent =  buff.substring(0, buff.length()-2);
						annoElement.appendChild(addRelationElement(doc, id, relContent, arg1Content, arg2Content, outFile));
						id++;
					}
				}

				rootElement.appendChild( infoElement );
				rootElement.appendChild( schema );
				rootElement.appendChild( annoElement );
				doc.appendChild( rootElement );

				// boilerplate xml-writing code:
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
				transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
				DOMSource source = new DOMSource( doc );
				StreamResult result = new StreamResult( new File( outDir, outFile + ".xml" ) );
				transformer.transform( source, result );
			} catch ( ParserConfigurationException e ) {
				e.printStackTrace();
				throw new AnalysisEngineProcessException( e );
			} catch ( TransformerConfigurationException e ) {
				e.printStackTrace();
				throw new AnalysisEngineProcessException( e );
			} catch ( TransformerException e ) {
				e.printStackTrace();
				throw new AnalysisEngineProcessException( e );
			} catch (CASException e) {
				e.printStackTrace();
				throw new AnalysisEngineProcessException( e );
			}

		}

		private static Element addRelationElement(Document doc, int id,  String relContent, String arg1Content, String arg2Content, String outFile){
			Element linkEl = doc.createElement( "relation" );
			String linkID = id + "@r@" + outFile + "@system";

			Element idE = doc.createElement( "id" );
			idE.setTextContent( linkID );
			Element typeE = doc.createElement( "type" );
			typeE.setTextContent( "TLINK" );
			Element parentTE = doc.createElement( "parentsType" );
			parentTE.setTextContent( "TemporalRelations" );
			//add properties
			Element property = doc.createElement( "properties" );

			Element sourceE = doc.createElement( "Source" );
			sourceE.setTextContent( arg1Content );
			Element relTypeE = doc.createElement( "Type" );

			relTypeE.setTextContent( relContent );
			Element targetE = doc.createElement( "Target" );
			targetE.setTextContent( arg2Content );

			property.appendChild( sourceE );
			property.appendChild( relTypeE );
			property.appendChild( targetE );

			linkEl.appendChild( idE );
			linkEl.appendChild( typeE );
			linkEl.appendChild( parentTE );
			linkEl.appendChild( property );
			return linkEl;        
		}

		private static Map<Integer, List<EventMention>> getMentionIdMap(JCas jcas, JCas probView){
			HashMap<Integer, List<EventMention>> map = new HashMap<>();

			for(EventMention mention : JCasUtil.select(jcas, EventMention.class)){
				List<EventMention> variations = new ArrayList<>();
				for(EventMention probMention : JCasUtil.select(probView, EventMention.class)){
					if(mention.getId() == probMention.getId()){
						variations.add(probMention);
					}
				}
				map.put(mention.getId(), variations);
			}
			return map;
		}

		private static Map<String, List<TemporalTextRelation>> getRelationIdMap(JCas probView){
			HashMap<String, List<TemporalTextRelation>> map = new HashMap<>();

			for(TemporalTextRelation probRel : JCasUtil.select(probView, TemporalTextRelation.class)){
				String idStr = getRelationId(probRel);
				if(!map.containsKey(idStr)){
					map.put(idStr, new ArrayList<TemporalTextRelation>());
				}
				List<TemporalTextRelation> variations = map.get(idStr);
				variations.add(probRel);          
			}

			return map;
		}
	}
	public static String getRelationId(TemporalTextRelation rel){
		StringBuffer buffer = new StringBuffer();
		if(rel.getArg1().getArgument().getClass().getSimpleName().equals("EventMention")){
			buffer.append('e');
		}else{
			buffer.append('t');
		}
		buffer.append(((IdentifiedAnnotation)rel.getArg1().getArgument()).getId());
		buffer.append(':');
		if(rel.getArg2().getArgument().getClass().getSimpleName().equals("EventMention")){
			buffer.append('e');
		}else{
			buffer.append('t');
		}
		buffer.append(((IdentifiedAnnotation)rel.getArg2().getArgument()).getId());
		return buffer.toString();     
	}
}

