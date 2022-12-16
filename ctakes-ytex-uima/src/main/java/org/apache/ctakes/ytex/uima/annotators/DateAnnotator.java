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
package org.apache.ctakes.ytex.uima.annotators;

import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.ytex.uima.types.Date;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;



import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.utils.Span;

/**
 * The cTAKES date doesn't actually parse the date. Parse the date with Chronic,
 * store a new annotation with the real date. Takes as initialization parameter
 * a type name; defaults to "org.apache.ctakes.typesystem.type.textsem.DateAnnotation"
 * Iterate through all annotations of this type, and use chronic to parse the
 * covered text.
 */
@PipeBitInfo(
		name = "Date Annotator",
		description = "Annotates Dates based upon whether or not text can be normalized to a date.",
		dependencies = { PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class DateAnnotator extends JCasAnnotator_ImplBase {
	private static final Log log = LogFactory.getLog(DateAnnotator.class);
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	String dateType;

	private ThreadLocal<SimpleDateFormat> tlDateFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat(DATE_FORMAT);
		}
	};

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		dateType = (String) aContext.getConfigParameterValue("dateType");
		if (dateType == null) {
			dateType = "org.apache.ctakes.typesystem.type.textsem.DateAnnotation";
		}
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		Type t = jCas.getTypeSystem().getType(dateType);
		if (t != null) {
			AnnotationIndex<Annotation> annoIndex = jCas.getAnnotationIndex();
			FSIterator<Annotation> iter = annoIndex.iterator();
			while (iter.hasNext()) {
				Annotation anno = iter.next();
				try {
					Span span = Chronic.parse(anno.getCoveredText());
					if (span != null && span.getBeginCalendar() != null) {
						Date date = new Date(jCas);
						date.setBegin(anno.getBegin());
						date.setEnd(anno.getEnd());
						date.setDate(tlDateFormat.get().format(
								span.getBeginCalendar().getTime()));
						date.addToIndexes();
					}
				} catch (Exception e) {
					if (log.isDebugEnabled())
						log.debug(
								"chronic failed on: " + anno.getCoveredText(),
								e);
				}
			}
		}
	}

}
