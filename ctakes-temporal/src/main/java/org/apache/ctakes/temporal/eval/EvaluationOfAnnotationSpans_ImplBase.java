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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.util.ViewUriUtil;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;

public abstract class EvaluationOfAnnotationSpans_ImplBase extends
Evaluation_ImplBase<AnnotationStatistics<String>> {

	private final Logger logger = Logger.getLogger(this.getClass().getName());
	public void setLogging(Level level, File outputFile) throws IOException {
		if (!outputFile.getParentFile().exists()) {
			outputFile.getParentFile().mkdirs();
		}
		this.logger.setLevel(level);
		FileHandler handler = new FileHandler(outputFile.getPath());
		handler.setFormatter(new Formatter() {
			@Override
			public String format(LogRecord record) {
				return record.getMessage() + '\n';
			}
		});
		this.logger.addHandler(handler);
	}

	private Class<? extends Annotation> annotationClass;

	public EvaluationOfAnnotationSpans_ImplBase(
			File baseDirectory,
			File rawTextDirectory,
			File xmlDirectory,
			XMLFormat xmlFormat,
			Subcorpus subcorpus,
			File xmiDirectory,
			File treebankDirectory,
			Class<? extends Annotation> annotationClass) {
		super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, subcorpus, xmiDirectory, treebankDirectory);
		this.annotationClass = annotationClass;
	}

	public EvaluationOfAnnotationSpans_ImplBase(
			File baseDirectory,
			File rawTextDirectory, 
			File xmlDirectory,
			XMLFormat xmlFormat,
			Subcorpus subcorpus,
			File xmiDirectory,
			Class<? extends Annotation> annotationClass) {
		this(baseDirectory,rawTextDirectory, xmlDirectory, xmlFormat, subcorpus, xmiDirectory, null, annotationClass);
	}

	protected abstract AnalysisEngineDescription getDataWriterDescription(File directory)
			throws ResourceInitializationException;

	protected abstract void trainAndPackage(File directory) throws Exception;

	@Override
	protected void train(CollectionReader collectionReader, File directory) throws Exception {
		AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
		aggregateBuilder.add(CopyFromGold.getDescription(this.annotationClass));
		aggregateBuilder.add(this.getDataWriterDescription(directory), "TimexView", CAS.NAME_DEFAULT_SOFA);
		SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());
		this.trainAndPackage(directory);
	}

	protected abstract AnalysisEngineDescription getAnnotatorDescription(File directory)
			throws ResourceInitializationException;

	protected abstract Collection<? extends Annotation> getGoldAnnotations(JCas jCas, Segment segment);

  protected abstract Collection<? extends Annotation> getSystemAnnotations(JCas jCas, Segment segment);

	@Override
	protected AnnotationStatistics<String> test(CollectionReader collectionReader, File directory)
			throws Exception {
		AggregateBuilder aggregateBuilder = this.getPreprocessorAggregateBuilder();
		aggregateBuilder.add(this.getAnnotatorDescription(directory), "TimexView", CAS.NAME_DEFAULT_SOFA);
		if(this.i2b2Output != null){
			aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(WriteI2B2XML.class, WriteI2B2XML.PARAM_OUTPUT_DIR, this.i2b2Output), "TimexView", CAS.NAME_DEFAULT_SOFA);
		}
		AnnotationStatistics<String> stats = new AnnotationStatistics<>();
		Ordering<Annotation> bySpans = Ordering.<Integer> natural().lexicographical().onResultOf(
				new Function<Annotation, List<Integer>>() {
					@Override
					public List<Integer> apply(Annotation annotation) {
						return Arrays.asList(annotation.getBegin(), annotation.getEnd());
					}
				});
		for (Iterator<JCas> casIter = new JCasIterator(collectionReader, aggregateBuilder.createAggregate()); casIter.hasNext();) {
			JCas jCas = casIter.next();
			JCas goldView = jCas.getView(GOLD_VIEW_NAME);
			JCas systemView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      this.logger.fine("Errors in : " + ViewUriUtil.getURI(jCas).toString());
			for (Segment segment : JCasUtil.select(jCas, Segment.class)) {
				if (!THYMEData.SEGMENTS_TO_SKIP.contains(segment.getId())) {
					Collection<? extends Annotation> goldAnnotations = this.getGoldAnnotations(goldView, segment);
					Collection<? extends Annotation> systemAnnotations = this.getSystemAnnotations(systemView, segment);
					stats.add(goldAnnotations, systemAnnotations);

					Set<Annotation> goldSet = new TreeSet<>(bySpans);
					for (Annotation goldAnnotation : goldAnnotations) {
						// TODO: fix data so that this is not necessary
						if (goldAnnotation.getBegin() == Integer.MAX_VALUE || goldAnnotation.getEnd() == Integer.MIN_VALUE) {
							this.logger.warning("Invalid annotation");
							continue;
						}
						goldSet.add(goldAnnotation);
					}
					//goldSet.addAll(goldAnnotations);
					Set<Annotation> systemSet = new TreeSet<>(bySpans);
					systemSet.addAll(systemAnnotations);

					Set<Annotation> goldOnly = new TreeSet<>(bySpans);
					goldOnly.addAll(goldSet);
					goldOnly.removeAll(systemSet);

					Set<Annotation> systemOnly = new TreeSet<>(bySpans);
					systemOnly.addAll(systemSet);
					systemOnly.removeAll(goldSet);

					String text = jCas.getDocumentText().replaceAll("[\r\n]", " ");
					if (!goldOnly.isEmpty() || !systemOnly.isEmpty()) {
						this.logger.fine("Errors in : " + ViewUriUtil.getURI(jCas).toString());
						Set<Annotation> errors = new TreeSet<>(bySpans);
						errors.addAll(goldOnly);
						errors.addAll(systemOnly);
						for (Annotation annotation : errors) {
							int begin = annotation.getBegin();
							int end = annotation.getEnd();
							int windowBegin = Math.max(0, begin - 50);
							int windowEnd = Math.min(text.length(), end + 50);
							String label = goldOnly.contains(annotation) ? "DROPPED:" : "ADDED:  ";
							this.logger.fine(String.format(
									"%s  ...%s[!%s!:%d-%d]%s...",
									label,
									text.substring(windowBegin, begin),
									text.substring(begin, end),
									begin,
									end,
									text.substring(end, windowEnd)));
						}
						//add correct predictions:
						for (Annotation annotation: goldSet){
							if (!errors.contains(annotation)){
								int begin = annotation.getBegin();
								int end = annotation.getEnd();
								int windowBegin = Math.max(0, begin - 50);
								int windowEnd = Math.min(text.length(), end + 50);
								String label = "CORRECT:";
								this.logger.fine(String.format(
										"%s  ...%s[!%s!:%d-%d]%s...",
										label,
										text.substring(windowBegin, begin),
										text.substring(begin, end),
										begin,
										end,
										text.substring(end, windowEnd)));
							}
						}
					}
					Set<Annotation> partialGold = new HashSet<>();
					Set<Annotation> partialSystem = new HashSet<>();

					// get overlapping spans
					if(this.printOverlapping){
						// iterate over all remaining gold annotations
						for(Annotation gold : goldOnly){
							Annotation bestSystem = null;
							int bestOverlap = 0;
							for(Annotation system : systemOnly){
								if(system.getBegin() >= gold.getBegin() && system.getEnd() <= gold.getEnd()){
									// system completely contained by gold
									int overlap = system.getEnd() - system.getBegin();
									if(overlap > bestOverlap){
										bestOverlap = overlap;
										bestSystem = system;
									}
								}else if(gold.getBegin() >= system.getBegin() && gold.getEnd() <= system.getEnd()){
									// gold completely contained by gold
									int overlap = gold.getEnd() - gold.getBegin();
									if(overlap > bestOverlap){
										bestOverlap = overlap;
										bestSystem = system;
									}
								}
							}
							if(bestSystem != null){
								this.logger.info(String.format("Allowed overlapping annotation: Gold(%s) => System(%s)\n", gold.getCoveredText(), bestSystem.getCoveredText()));
								partialGold.add(gold);
								partialSystem.add(bestSystem);
							}
						}
						if(partialGold.size() > 0){
							goldOnly.removeAll(partialGold);
							systemOnly.removeAll(partialSystem);
							assert partialGold.size() == partialSystem.size();
							this.logger.info(String.format("Found %d overlapping spans and removed from gold/system errors\n", partialGold.size()));
						}
					}
				}
			}
		}
		return stats;
	}
}
