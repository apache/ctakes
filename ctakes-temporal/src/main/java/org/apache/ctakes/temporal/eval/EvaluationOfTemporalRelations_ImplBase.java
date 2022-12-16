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
import java.util.Collection;
import java.util.Set;

import org.apache.ctakes.temporal.eval.EvaluationOfEventTimeRelations.ParameterSettings;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.tksvmlight.model.CompositeKernel.ComboOperator;
import org.cleartk.eval.AnnotationStatistics;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.lexicalscope.jewel.cli.Option;

public abstract class EvaluationOfTemporalRelations_ImplBase extends
Evaluation_ImplBase<AnnotationStatistics<String>> {

	public static interface TempRelOptions extends Evaluation_ImplBase.Options{
		@Option
		public boolean getTest();

		@Option
		public boolean getPrintFormattedRelations();

		@Option
		public boolean getBaseline();

		@Option
		public boolean getClosure();

		@Option
		public boolean getClassificationOnly();

	}

	protected static boolean DEFAULT_BOTH_DIRECTIONS = false;
	protected static float DEFAULT_DOWNSAMPLE = 1.0f;
	protected static double DEFAULT_SVM_C = 1.0;
	protected static double DEFAULT_SVM_G = 1.0;
	protected static double DEFAULT_TK = 0.5;
	protected static double DEFAULT_LAMBDA = 0.5;

	protected static ParameterSettings defaultParams = new ParameterSettings(DEFAULT_BOTH_DIRECTIONS, DEFAULT_DOWNSAMPLE, "linear",
			DEFAULT_SVM_C, DEFAULT_SVM_G, "polynomial", ComboOperator.SUM, DEFAULT_TK, DEFAULT_LAMBDA);


	protected ParameterSettings params = null;
	protected boolean printRelations = false;

	public EvaluationOfTemporalRelations_ImplBase(File baseDirectory,
			File rawTextDirectory, File xmlDirectory, XMLFormat xmlFormat, Subcorpus subcorpus,
			File xmiDirectory, File treebankDirectory, boolean printErrors, boolean printRelations, ParameterSettings params) {
		super(baseDirectory, rawTextDirectory, xmlDirectory, xmlFormat, subcorpus, xmiDirectory,
				treebankDirectory);
		this.params = params;
		this.printRelations = printRelations;
		this.printErrors =  printErrors;
	}

	public static class PreserveEventEventRelations extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
		public static final String PARAM_GOLD_VIEW = "GoldView";

		@ConfigurationParameter(name = PARAM_GOLD_VIEW,mandatory=false)
		private String goldViewName = CAS.NAME_DEFAULT_SOFA;

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for(BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(jCas, BinaryTextRelation.class))){
				RelationArgument arg1 = relation.getArg1();
				RelationArgument arg2 = relation.getArg2();
				if(arg1.getArgument() instanceof EventMention && arg2.getArgument() instanceof EventMention && relation instanceof TemporalTextRelation){
					// these are the kind we keep.
					continue;
				}
				arg1.removeFromIndexes();
				arg2.removeFromIndexes();
				relation.removeFromIndexes();
			}
		}   
	}

	public static class RemoveNonContainsRelations extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
		public static final String PARAM_RELATION_VIEW = "RelationView";

		@ConfigurationParameter(name = PARAM_RELATION_VIEW,mandatory=false)
		private String relationViewName = CAS.NAME_DEFAULT_SOFA;
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			JCas relationView = null;

			try {
				relationView = jCas.getView(relationViewName);
			} catch (CASException e) {
				e.printStackTrace();
			}
			for (BinaryTextRelation relation : Lists.newArrayList(JCasUtil.select(
					relationView,
					BinaryTextRelation.class))) {
				RelationArgument arg1 = relation.getArg1();
				RelationArgument arg2 = relation.getArg2();
				if (!relation.getCategory().startsWith("CONTAINS")) {
					relation.getArg1().removeFromIndexes();
					relation.getArg2().removeFromIndexes();
					relation.removeFromIndexes();
				}
			}
		}
	}
	public static class RemoveGoldAttributes extends JCasAnnotator_ImplBase {
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for(EventMention event : JCasUtil.select(jCas, EventMention.class)){
				if(event.getEvent() != null && event.getEvent().getProperties() != null){
					event.getEvent().getProperties().setContextualAspect("UNK");
					event.getEvent().getProperties().setContextualModality("UNK");
				}
			}
			for(TimeMention timex : JCasUtil.select(jCas, TimeMention.class)){
				timex.setTimeClass("UNK");
			}
		}
	}


	protected static Collection<BinaryTextRelation> correctArgOrder(
			Collection<BinaryTextRelation> systemRelations,
			Collection<BinaryTextRelation> goldRelations) {
		Set<BinaryTextRelation> goodSys = Sets.newHashSet();

		for(BinaryTextRelation sysrel : systemRelations){
			Annotation sysArg1 = sysrel.getArg1().getArgument();
			Annotation sysArg2 = sysrel.getArg2().getArgument();
			for(BinaryTextRelation goldrel : goldRelations){
				Annotation goldArg1 = goldrel.getArg1().getArgument();
				Annotation goldArg2 = goldrel.getArg2().getArgument();
				if (matchSpan(sysArg2, goldArg1) && matchSpan(sysArg1, goldArg2)){//the order of system pair was flipped 
					if(sysrel.getCategory().equals("OVERLAP")){ //if the relation is overlap, and the arg order was flipped, then change back the order
						RelationArgument tempArg = (RelationArgument) sysrel.getArg1().clone();
						sysrel.setArg1((RelationArgument) sysrel.getArg2().clone());
						sysrel.setArg2(tempArg);
					}//for other types of relation, still maintain the type.
					continue;
				}
			}
			goodSys.add(sysrel);
		}

		return goodSys;
	}

	private static boolean matchSpan(Annotation arg1, Annotation arg2) {
		boolean result = false;
		result = arg1.getBegin() == arg2.getBegin() && arg1.getEnd() == arg2.getEnd();
		return result;
	}

	protected static void printRelationAnnotations(String fileName, Collection<BinaryTextRelation> relations) {

		for(BinaryTextRelation binaryTextRelation : relations) {

			Annotation arg1 = binaryTextRelation.getArg1().getArgument();
			Annotation arg2 = binaryTextRelation.getArg2().getArgument();

			String arg1Type = arg1.getClass().getSimpleName();
			String arg2Type = arg2.getClass().getSimpleName();

			int arg1Begin = arg1.getBegin();
			int arg1End = arg1.getEnd();
			int arg2Begin = arg2.getBegin();
			int arg2End = arg2.getEnd();

			String category = binaryTextRelation.getCategory();

			System.out.format("%s\t%s\t%s\t%d\t%d\t%s\t%d\t%d\n", 
					fileName, category, arg1Type, arg1Begin, arg1End, arg2Type, arg2Begin, arg2End);
		}
	}

	protected static String formatRelation(BinaryTextRelation relation) {
		IdentifiedAnnotation arg1 = (IdentifiedAnnotation)relation.getArg1().getArgument();
		IdentifiedAnnotation arg2 = (IdentifiedAnnotation)relation.getArg2().getArgument();
		String arg1Type ="E";
		String arg2Type ="T";
		if(arg1 instanceof TimeMention) arg1Type = "T";
		if(arg2 instanceof EventMention) arg2Type = "E";
		String text = arg1.getCAS().getDocumentText();
		int begin = Math.min(arg1.getBegin(), arg2.getBegin());
		int end = Math.max(arg1.getEnd(), arg2.getEnd());
		int begin1 = 0;
		int end1 = 0;
		int begin2 = 0;
		int end2 = 0;
		if(arg1.getBegin()< arg2.getBegin()){
			begin1 = arg1.getBegin();
			end1 = arg1.getEnd();
			begin2 = arg2.getBegin();
			end2 = arg2.getEnd();
		}else{ //arg1 is after arg2
			begin1 = arg2.getBegin();
			end1 = arg2.getEnd();
			begin2 = arg1.getBegin();
			end2 = arg1.getEnd();
		}
		begin = Math.max(0, begin - 200);
		end = Math.min(text.length(), end + 200);
		StringBuilder between = new StringBuilder();
		if(begin<begin1) between.append(text.substring(begin, begin1).replaceAll("[\r\n]", " "));
		between.append(" [" + text.substring(begin1, end1) + "] ");
		if(begin2<=end1){
			between.append(text.substring(begin,begin1).replaceAll("[\r\n]", " ")+" ["+ text.substring(begin1,begin2)+" [");
			if(end1<end2){
				between.append(text.substring(begin2,end1) + "]" + text.substring(end1,end2) + "]" +  text.substring(end2,end));
			}else{
				between.append(text.substring(begin2,end2) + "]" + text.substring(end2,end1) + "]" +  text.substring(end1,end));
			}

		}else if(begin2-end1 <=400){
			between.append(text.substring(end1, begin2).replaceAll("[\r\n]", " "));
		}else{
			between.append(text.substring(end1, end1+200).replaceAll("[\r\n]", " ")+ "...");
			int extra = (begin2-end1-400)/10;
			for (int i = 1; i<extra; i++){
				between.append(".");
			}
			between.append(text.substring(begin2-200, begin2).replaceAll("[\r\n]", " "));
		}
		between.append(" [" + text.substring(begin2, end2)+ "] ");
		if(end2 < end)  between.append(text.substring(end2, end).replaceAll("[\r\n]", " ") );
		return String.format(
				"%s(%s(type=%d!%d-%d!%s), %s(type=%d!%d-%d!%s)) in ...%s...",
				relation.getCategory(),
				arg1.getCoveredText(),
				arg1.getTypeID(),
				//add extra
				arg1.getBegin(),
				arg1.getEnd(),
				arg1Type,

				arg2.getCoveredText(),
				arg2.getTypeID(),
				//add extra
				arg2.getBegin(),
				arg2.getEnd(),
				arg2Type,
				between.toString());
	}

}
