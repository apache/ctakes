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
package org.apache.ctakes.ytex.uima;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.ctakes.ytex.dao.DBUtil;
import org.apache.ctakes.ytex.uima.annotators.DBConsumer;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;

import com.google.common.base.Strings;

public class TestUtils {
	static final String queryGetDocumentKeys = "select note_id instance_id from %s%s";
	static final String queryGetDocument = "select note_text from %s%s where note_id = :instance_id";

	public static void addDescriptor(AggregateBuilder builder, String path)
			throws IOException, InvalidXMLException {
		File fileCtakes = new File(path);
		XMLParser parser = UIMAFramework.getXMLParser();
		XMLInputSource source = new XMLInputSource(fileCtakes);
		builder.add(parser.parseAnalysisEngineDescription(source));
	}

	/**
	 * Create a simple aggregate ae that does sentence splitting, tokenization,
	 * and stores results in database. runs the following AEs: <li>
	 * SegmentRegexAnnotator <li>SentenceDetectorAnnotator <li>
	 * TokenizerAnnotator <li>DBConsumer
	 * 
	 * @param analysisBatch
	 *            name of analysis batch for dbconsumer. If null will be set to
	 *            test-[current time millis].
	 * @return
	 * @throws IOException
	 * @throws InvalidXMLException
	 * @throws ResourceInitializationException
	 */
	public static AnalysisEngine createTokenizerAE(String analysisBatch)
			throws IOException, InvalidXMLException,
			ResourceInitializationException {
		String dbAnalysisBatch = analysisBatch;
		if (Strings.isNullOrEmpty(dbAnalysisBatch))
			dbAnalysisBatch = "test-" + System.currentTimeMillis();
		AggregateBuilder builder = new AggregateBuilder();
		addDescriptor(builder, "desc/analysis_engine/SegmentRegexAnnotator.xml");
		addDescriptor(builder,
				"desc/analysis_engine/SentenceDetectorAnnotator.xml");
		addDescriptor(builder,
				"../ctakes-core/desc/analysis_engine/TokenizerAnnotator.xml");
		builder.add(AnalysisEngineFactory.createEngineDescription(
				DBConsumer.class, "analysisBatch", dbAnalysisBatch,
				"storeDocText", false, "storeCAS", true));
		AnalysisEngine engine = builder.createAggregate();
		return engine;
	}

	public static CollectionReader getFractureDemoCollectionReader()
			throws ResourceInitializationException {
		CollectionReader colReader = CollectionReaderFactory
				.createReader(
						DBCollectionReader.class,
						"queryGetDocumentKeys",
						String.format(queryGetDocumentKeys,
								DBUtil.getYTEXTablePrefix(),
								DBUtil.formatTableName("fracture_demo")),
						"queryGetDocument",
						String.format(queryGetDocument,
								DBUtil.getYTEXTablePrefix(),
								DBUtil.formatTableName("fracture_demo")));
		return colReader;
	}
}
