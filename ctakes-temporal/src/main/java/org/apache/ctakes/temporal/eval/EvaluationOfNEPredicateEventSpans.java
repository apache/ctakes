/**
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
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.apache.ctakes.temporal.ae.NEPredicateEventAnnotator;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.eval.AnnotationStatistics;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;

import com.lexicalscope.jewel.cli.CliFactory;

public class EvaluationOfNEPredicateEventSpans extends
		EvaluationOfAnnotationSpans_ImplBase {

	public EvaluationOfNEPredicateEventSpans(File baseDirectory,
			File rawTextDirectory, File xmlDirectory, XMLFormat xmlFormat,
			Subcorpus subcorpus, File xmiDirectory, File treebankDirectory,
			Class<? extends Annotation> annotationClass) {
		super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat,
				subcorpus, xmiDirectory, treebankDirectory, annotationClass);
		// TODO Auto-generated constructor stub
	}

	public EvaluationOfNEPredicateEventSpans(File baseDirectory,
			File rawTextDirectory, File xmlDirectory, XMLFormat xmlFormat,
			Subcorpus subcorpus, File xmiDirectory) {
		super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat,
				subcorpus, xmiDirectory, EventMention.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected AnalysisEngineDescription getDataWriterDescription(File directory)
			throws ResourceInitializationException {
		// not training a model - just using the NEs and predicates
	    return AnalysisEngineFactory.createEngineDescription(NoOpAnnotator.class);
	}

	@Override
	protected void trainAndPackage(File directory) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected AnalysisEngineDescription getAnnotatorDescription(File directory)
			throws ResourceInitializationException {
		// not training a model - just using the NEs and predicates
//		return AnalysisEngineFactory.createEngineDescription(NoOpAnnotator.class);
	    return NEPredicateEventAnnotator.createAnnotatorDescription(directory.getAbsolutePath());
	}

	@Override
	protected Collection<? extends Annotation> getGoldAnnotations(JCas jCas, Segment segment) {
		return selectExact(jCas, EventMention.class, segment);
	}

	@Override
	protected Collection<? extends Annotation> getSystemAnnotations(JCas jCas, Segment segment) {
		return selectExact(jCas, EventMention.class, segment);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		Options options = CliFactory.parseArguments(Options.class, args);
	    List<Integer> patientSets = options.getPatients().getList();
	    List<Integer> trainItems = getTrainItems(options);
	    List<Integer> testItems = getTestItems(options);
	    EvaluationOfNEPredicateEventSpans evaluation = new EvaluationOfNEPredicateEventSpans(
	        new File("target/eval/event-spans"),
	        options.getRawTextDirectory(),
	        options.getXMLDirectory(),
	        options.getXMLFormat(),
	        options.getSubcorpus(),
	        options.getXMIDirectory());
	    evaluation.prepareXMIsFor(patientSets);
	    evaluation.setLogging(Level.FINE, new File("target/eval/ctakes-event-errors.log"));
	    AnnotationStatistics<String> stats = evaluation.trainAndTest(trainItems, testItems);
	    System.err.println(stats);

	}

}
