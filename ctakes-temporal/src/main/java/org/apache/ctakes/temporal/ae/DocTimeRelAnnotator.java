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
package org.apache.ctakes.temporal.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.temporal.ae.feature.*;
import org.apache.ctakes.temporal.utils.SoftMaxUtil;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NumToken;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.Feature;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.extractor.*;
import org.cleartk.ml.feature.extractor.CleartkExtractor.*;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction.PatternType;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

//import java.io.IOException;
//import java.util.Map;
//import org.apache.ctakes.temporal.ae.feature.duration.DurationExpectationFeatureExtractor;

//import com.google.common.base.Charsets;

@PipeBitInfo(
		name = "DocTimeRel Annotator",
		description = "Annotates event relativity to document creation time.",
		dependencies = { PipeBitInfo.TypeProduct.SENTENCE,
				PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.EVENT }
)
public class DocTimeRelAnnotator extends CleartkAnnotator<String> {

   static private final Logger LOGGER = LogManager.getLogger( "DocTimeRelAnnotator" );

	public static AnalysisEngineDescription createDataWriterDescription(
			Class<? extends DataWriter<String>> dataWriterClass,
					File outputDirectory) throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				DocTimeRelAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				true,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
				dataWriterClass,
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
				outputDirectory);
	}

	public static AnalysisEngineDescription createAnnotatorDescription(String modelPath)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				DocTimeRelAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				modelPath);
	}

	/**
	 * @deprecated use String path instead of File.
	 * ClearTK will automatically Resolve the String to an InputStream.
	 * This will allow resources to be read within from a jar as well as File.  
	 */
	@Deprecated
	public static AnalysisEngineDescription createAnnotatorDescription(File modelDirectory)
			throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(
				DocTimeRelAnnotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING,
				false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				new File(modelDirectory, "model.jar"));
	}

	private CleartkExtractor<EventMention, BaseToken> contextExtractor;  
	private CleartkExtractor<EventMention, BaseToken> tokenVectorContext;
	private CleartkExtractor<EventMention, BaseToken> tokenVectorContext2;
	private ContinuousTextExtractor continuousText;
	private ContinuousTextExtractor continuousText2;
	private SectionHeaderExtractor sectionIDExtractor;
	private ClosestVerbExtractor closestVerbExtractor;
	private TimeXExtractor timeXExtractor;
	private EventPropertyExtractor genericExtractor;
//	private UmlsSingleFeatureExtractor umlsExtractor;
	private NearbyVerbTenseXExtractor verbTensePatternExtractor;

	private DateAndMeasurementExtractor dateExtractor;  
//	private CoveredTextToValuesExtractor disSemExtractor;
//	private DurationExpectationFeatureExtractor durationExtractor;

	public static final String PARAM_PROB_VIEW = "ProbView";
	@ConfigurationParameter(name=PARAM_PROB_VIEW, mandatory=false)
	private String probViewname = null;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
      LOGGER.info( "Initializing ..." );
      try ( DotLogger dotter = new DotLogger() ) {
         super.initialize( context );
         CombinedExtractor1<BaseToken> baseExtractor = new CombinedExtractor1<>(
               new CoveredTextExtractor<BaseToken>(),
               CharacterCategoryPatternFunction.<BaseToken>createExtractor( PatternType.ONE_PER_CHAR ),
               new TypePathExtractor<>( BaseToken.class, "partOfSpeech" ) );
         this.contextExtractor = new CleartkExtractor<>(
               BaseToken.class,
               baseExtractor,
               new Preceding( 3 ),
               new FirstCovered( 1 ),
               new LastCovered( 1 ),
               new Bag( new Covered() ),
               new Following( 3 ) );
         final String vectorFile = "org/apache/ctakes/temporal/mimic_vectors.txt";
         final String vectorFile2 = "org/apache/ctakes/temporal/thyme_word2vec_mapped_50.vec";
         try {
            this.continuousText = new ContinuousTextExtractor( vectorFile );
            this.continuousText2 = new ContinuousTextExtractor( vectorFile2 );
         } catch ( CleartkExtractorException e ) {
            System.err.println( "cannot find file: " + vectorFile );
            e.printStackTrace();
         }
         this.tokenVectorContext = new CleartkExtractor<>(
               BaseToken.class,
               continuousText,
               new Preceding( 5 ),
               new Covered(),
               new Following( 5 ) );
         this.tokenVectorContext2 = new CleartkExtractor<>(
               BaseToken.class,
               continuousText2,
               new Covered() );
         this.sectionIDExtractor = new SectionHeaderExtractor();
         this.closestVerbExtractor = new ClosestVerbExtractor();
         this.timeXExtractor = new TimeXExtractor();
         this.genericExtractor = new EventPropertyExtractor();
         //		this.umlsExtractor = new UmlsSingleFeatureExtractor();
         this.verbTensePatternExtractor = new NearbyVerbTenseXExtractor();

         this.dateExtractor = new DateAndMeasurementExtractor();

         //		try {
         //			Map<String, double[]> word_disSem = CoveredTextToValuesExtractor.parseTextDoublesMap(new File("src/main/resources/embeddings.size25.txt"), Charsets.UTF_8);
         //			this.disSemExtractor = new CoveredTextToValuesExtractor("DisSemFeat", word_disSem);
         //		} catch (IOException e) {
         //			e.printStackTrace();
         //		}
         //		this.durationExtractor = new DurationExpectationFeatureExtractor();
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
      LOGGER.info( "Finished." );
   }

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		LOGGER.info( "Finding DocTimeRel ..." );
		for (EventMention eventMention : JCasUtil.select(jCas, EventMention.class)) {
			List<Sentence> sents = JCasUtil.selectCovering(jCas, Sentence.class, eventMention);
			List<Feature> features = new ArrayList<>();
			if(sents!=null && sents.size()>0){
				features.addAll(this.contextExtractor.extractWithin(jCas, eventMention, sents.get(0)));
				features.addAll(this.tokenVectorContext.extractWithin(jCas, eventMention, sents.get(0)));
				features.addAll(this.tokenVectorContext2.extractWithin(jCas, eventMention, sents.get(0)));
			}else{
				features.addAll(this.contextExtractor.extract(jCas, eventMention));
				features.addAll(this.tokenVectorContext.extract(jCas, eventMention));
				features.addAll(this.tokenVectorContext2.extract(jCas, eventMention));
			}

			features.addAll(this.sectionIDExtractor.extract(jCas, eventMention)); //add section heading
			features.addAll(this.closestVerbExtractor.extract(jCas, eventMention)); //add closest verb
			features.addAll(this.timeXExtractor.extract(jCas, eventMention)); //add the closest time expression types
			features.addAll(this.genericExtractor.extract(jCas, eventMention)); //add the closest time expression types
//			features.addAll(this.umlsExtractor.extract(jCas, eventMention)); //add umls features
			features.addAll(this.verbTensePatternExtractor.extract(jCas, eventMention));//add nearby verb POS pattern feature

			//
			features.addAll(this.dateExtractor.extract(jCas, eventMention)); //add the closest NE type
//			features.addAll(this.durationExtractor.extract(jCas, eventMention)); //add duration feature
//			features.addAll(this.disSemExtractor.extract(jCas, eventMention)); //add distributional semantic features
			if (this.isTraining()) {
				if(eventMention.getEvent() != null){
					String outcome = eventMention.getEvent().getProperties().getDocTimeRel();
					this.dataWriter.write(new Instance<>(outcome, features));
				}
			} else {
				//        String outcome = this.classifier.classify(features);
				Map<String,Double> scores = this.classifier.score(features);
				Map.Entry<String, Double> maxEntry = null;
				for( Map.Entry<String, Double> entry: scores.entrySet() ){
					if(maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0){
						maxEntry = entry;
					}
				}

				if (probViewname != null){
					Map<String,Double> probs = SoftMaxUtil.getDistributionFromScores(scores);
					try {
						JCas probView = jCas.getView(probViewname);
						for(String label : probs.keySet()){
							EventMention mention = new EventMention(probView);
							mention.setId(eventMention.getId());
							mention.setConfidence(probs.get(label).floatValue());
							Event event = new Event(probView);
							EventProperties props = new EventProperties(probView);
							props.setDocTimeRel(label);
							event.setProperties(props);
							mention.setEvent(event);
							mention.addToIndexes();
						}
					} catch (CASException e) {
						e.printStackTrace();
						throw new AnalysisEngineProcessException(e);
					}

				}

				if (eventMention.getEvent() == null) {
					Event event = new Event(jCas);
					eventMention.setEvent(event);
					EventProperties props = new EventProperties(jCas);
					event.setProperties(props);
				}
				if( maxEntry != null){
					eventMention.getEvent().getProperties().setDocTimeRel(maxEntry.getKey());
					eventMention.getEvent().setConfidence(maxEntry.getValue().floatValue());
					//        	System.out.println("event DocTimeRel confidence:"+maxEntry.getValue().floatValue());
				}
			}
		}
	}

//	@Override
//	public void process(JCas jCas) throws AnalysisEngineProcessException {
//		LOGGER.info( "Finding DocTimeRel ..." );
//		final Map<Segment, Collection<Sentence>> sectionSentenceMap = JCasUtil.indexCovered( jCas,
//																														 Segment.class,
//																														 Sentence.class );
//		final Map<Sentence,Collection<IdentifiedAnnotation>> sentenceAnnotationsMap = JCasUtil.indexCovered( jCas,
//																														 Sentence.class,
//																														 IdentifiedAnnotation.class );
//		final Map<Sentence,Collection<BaseToken>> sentenceTokensMap = JCasUtil.indexCovered( jCas,
//																																 Sentence.class,
//																																BaseToken.class );
//		final Collection<EventMention> handled = new HashSet<>();
//		for ( Map.Entry<Segment,Collection<Sentence>> sectionSentences : sectionSentenceMap.entrySet() ) {
//			handleSection( jCas, sectionSentences.getKey(), sectionSentences.getValue(), sentenceAnnotationsMap,
//							sentenceTokensMap, handled );
//		}
//	}

	private void handleSection( final JCas jCas,
										 final Segment section,
												  final Collection<Sentence> sentences,
												  final Map<Sentence, Collection<IdentifiedAnnotation>> sentenceAnnotationsMap,
										 final Map<Sentence,Collection<BaseToken>> sentenceTokensMap,
												  final Collection<EventMention> handled ) throws AnalysisEngineProcessException {
		if ( sentences == null ) {
			return;
		}
		for ( Sentence sentence : sentences ) {
			handleSentence( jCas, section, sentence, sentenceAnnotationsMap.get( sentence ),
								 sentenceTokensMap.get( sentence ),
								 handled );
		}
	}

	private void handleSentence( final JCas jCas,
										  final Segment section,
													final Sentence sentence,
													final Collection<IdentifiedAnnotation> annotations,
													final Collection<BaseToken> tokens,
													final Collection<EventMention> handled ) throws AnalysisEngineProcessException {
		if ( annotations == null ) {
			return;
		}
		final Collection<EventMention> events = new ArrayList<>();
		final Collection<TimeMention> timexes = new ArrayList<>();
		final Collection<TimeAnnotation> times = new ArrayList<>();
		final Collection<DateAnnotation> dates = new ArrayList<>();
		final Collection<MeasurementAnnotation> measurements = new ArrayList<>();
		for ( IdentifiedAnnotation annotation : annotations ) {
			if ( annotation instanceof EventMention ) {
				events.add( (EventMention)annotation );
			} else if ( annotation instanceof TimeMention ) {
				timexes.add( (TimeMention) annotation );
			} else if ( annotation instanceof TimeAnnotation ) {
				times.add( (TimeAnnotation) annotation );
			} else if ( annotation instanceof DateAnnotation ) {
				dates.add( (DateAnnotation) annotation );
			} else if ( annotation instanceof MeasurementAnnotation ) {
				measurements.add( (MeasurementAnnotation) annotation );
			}
		}
		if ( events.isEmpty() ) {
			return;
		}
		final List<BaseToken> sortedTokens = tokens.stream()
																 .sorted( Comparator.comparingInt( BaseToken::getBegin )
																						  .thenComparingInt( BaseToken::getEnd ) )
																 .collect( Collectors.toList() );

		final List<WordToken> sortedWords = new ArrayList<>();
		final List<NumToken> sortedNumbers = new ArrayList<>();
		for ( BaseToken token : sortedTokens ) {
			if ( token instanceof WordToken ) {
				sortedWords.add( (WordToken) token );
			} else if ( token instanceof NumToken ) {
				sortedNumbers.add( (NumToken) token );
			}
		}
		final int sentenceEnd = sentence.getEnd();
		final List<Feature> features = new ArrayList<>();
		for ( EventMention event : events ) {
			if ( handled.contains( event ) ) {
				// Can't we exit here?  Why was the old method continuing with the addition of other features?
				continue;
			}
			if ( event.getEnd() > sentenceEnd ) {
				features.addAll( this.contextExtractor.extractWithin( jCas, event, sentence ) );
				features.addAll( this.tokenVectorContext.extractWithin( jCas, event, sentence ) );
				features.addAll( this.tokenVectorContext2.extractWithin( jCas, event, sentence ) );
				handled.add( event );
			} else {
				features.addAll( this.contextExtractor.extract( jCas, event ) );
				features.addAll( this.tokenVectorContext.extract( jCas, event ) );
				features.addAll( this.tokenVectorContext2.extract( jCas, event ) );
			}
			//add section heading
			features.addAll( this.sectionIDExtractor.extract( jCas, event, Collections.singletonList( section ) ) );
			//add closest verb
			features.addAll( this.closestVerbExtractor.extract( jCas, event, sortedWords ) );
			//add the closest time expression types
			features.addAll(
					this.timeXExtractor.extract( jCas, event, events, timexes, times, dates ) );
			//add the closest raw time expression types
			features.addAll( this.genericExtractor.extract( jCas, event, events, sortedTokens, sortedWords ) );
			//			features.addAll(this.umlsExtractor.extract(jCas, eventMention)); //add umls features
			//add nearby verb POS pattern feature
			features.addAll(
					this.verbTensePatternExtractor.extract( jCas, event, sortedWords ) );
			//add the closest NE
			features.addAll( this.dateExtractor.extract( jCas, event, dates, measurements, sortedNumbers ) );

			// type
			//			features.addAll(this.durationExtractor.extract(jCas, eventMention)); //add duration feature
			//			features.addAll(this.disSemExtractor.extract(jCas, eventMention)); //add distributional semantic features
			if ( this.isTraining() ) {
				if ( event.getEvent() != null ) {
					final String outcome = event.getEvent()
														  .getProperties()
														  .getDocTimeRel();
					this.dataWriter.write( new Instance<>( outcome, features ) );
				}
			} else {
				//        String outcome = this.classifier.classify(features);
				final Map<String, Double> scores = this.classifier.score( features );
				Map.Entry<String, Double> maxEntry = null;
				for ( Map.Entry<String, Double> entry : scores.entrySet() ) {
					if ( maxEntry == null || entry.getValue()
															.compareTo( maxEntry.getValue() ) > 0 ) {
						maxEntry = entry;
					}
				}

				if ( probViewname != null ) {
					Map<String, Double> probs = SoftMaxUtil.getDistributionFromScores( scores );
					try {
						JCas probView = jCas.getView( probViewname );
						for ( String label : probs.keySet() ) {
							final EventMention mention = new EventMention( probView );
							mention.setId( event.getId() );
							mention.setConfidence( probs.get( label )
																 .floatValue() );
							final Event viewEvent = new Event( probView );
							final EventProperties props = new EventProperties( probView );
							props.setDocTimeRel( label );
							viewEvent.setProperties( props );
							mention.setEvent( viewEvent );
							mention.addToIndexes();
						}
					} catch ( CASException e ) {
						e.printStackTrace();
						throw new AnalysisEngineProcessException( e );
					}

				}

				if ( event.getEvent() == null ) {
					Event casEvent = new Event( jCas );
					event.setEvent( casEvent );
					EventProperties props = new EventProperties( jCas );
					casEvent.setProperties( props );
				}
				if ( maxEntry != null ) {
					event.getEvent()
									.getProperties()
									.setDocTimeRel( maxEntry.getKey() );
					event.getEvent()
									.setConfidence( maxEntry.getValue()
																	.floatValue() );
					//        	System.out.println("event DocTimeRel confidence:"+maxEntry.getValue().floatValue());
				}
			}
		}

	}

}
