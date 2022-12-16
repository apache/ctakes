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
package org.apache.ctakes.regression.test;

import org.apache.commons.io.FilenameUtils;
import org.apache.ctakes.core.ae.UmlsEnvironmentConfiguration;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.util.XMLInputSource;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.IgnoreTextAndAttributeValuesDifferenceListener;
import org.junit.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Runs a full pipeline and compares the xml output to ensure all annotators
 * work together in harmony.
 *
 * This is designed to run all CPE's inside the
 * desc/collection_processing_engine Directory. So any new pipeline added there
 * will automatically be run and tested as long as they put the generated output
 * to expectedoutput/{nameofcpe}
 *
 * The Apache cTAKES Release Manager should ensure that at a min this test
 * passes before releasing.
 *
 * If there are new modules, be sure to add the desc here. If there are changes,
 * after it's been manually reviewed, re-record the expected output and put them
 * in output folder.
 *
 * This also tests the UMLS annotator(s) so be sure to download the UMLS
 * Resources per README and add the jvm params -Dctakes.umlsuser=
 * -Dctakes.umlspw= parameters before running
 *
 */
public class RegressionPipelineTest {

	static Logger logger = Logger.getLogger(RegressionPipelineTest.class.getName());

	// MAX timeout for the CPE
	private static final int MAX_TIMEOUT_MS = 60 * 60 * 1000; // 60 mins
	// TODO: consider refactor
	private int num_cpe = 0;

	private static final Boolean hasUMLSCredentials() {
		return EnvironmentVariable.getEnv(UmlsEnvironmentConfiguration.USER.toString()) != null;
	}

	@BeforeClass
	public static void beforeClass() {
		Assume.assumeTrue( hasUMLSCredentials() );
	}

	@Test
	public void testCPE() throws Exception {
		long started = System.currentTimeMillis();
		File directoryCPE = new File("desc/collection_processing_engine");
		File[] listOfFiles = directoryCPE.listFiles();
		for (File file : listOfFiles) {

			if (file.isFile()) {
				num_cpe++;
				File generated = new File(String.format("testdata/generatedoutput/%s", FilenameUtils.removeExtension(file.getName())));
				File expected = new File(String.format("testdata/expectedoutput/%s", FilenameUtils.removeExtension(file.getName())));

				logger.info("Creating directory: " + generated);

				Assert.assertTrue(
						"Unable to create " + generated.getAbsolutePath(),
						generated.exists() || generated.mkdirs());

				// Run all of the CPE's in the Dir
				logger.info("Parsing CPE Descriptor:" + file.getName());
				CpeDescription cpeDesc = UIMAFramework.getXMLParser()
						.parseCpeDescription(new XMLInputSource(file));
				CollectionProcessingEngine mCPE = UIMAFramework
						.produceCollectionProcessingEngine(cpeDesc);
				// Create and register a Status Callback Listener
				mCPE.addStatusCallbackListener(new RegressionPipelineTest.StatusCallbackListenerImpl(expected, generated));
				mCPE.process();
			}
		}
		// Wait until all of the CPE's have been completed
		// Before comparing.
		while (num_cpe > 0) {
			if (System.currentTimeMillis() - started >= MAX_TIMEOUT_MS) {
				Assert.assertEquals("Timed out:", String.format("Regression CPE test timed out after %d ms", MAX_TIMEOUT_MS));
			}
			Thread.sleep(1000);
		}
	}

	public void compareXMLOutput(File expected_, File generated_)
			throws IOException, ParserConfigurationException, SAXException {

		File[] listOfFiles = expected_.listFiles();
		for (File file : listOfFiles) {
			if (file.isFile()) {
				logger.info("Verifying Test Output: " + file.getAbsolutePath());
				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				dbf.setNamespaceAware(true);
				dbf.setCoalescing(true);
				dbf.setIgnoringElementContentWhitespace(true);
				dbf.setIgnoringComments(true);
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document expected = db.parse(file);
				File f = new File(generated_.getPath() + File.separator
						+ file.getName());
				Document generated = db.parse(f);
				generated.normalizeDocument();
				expected.normalizeDocument();

				Diff myDiff = new Diff(expected, generated);
				// Elment and attributes in the xcas could be in different
				// Ordering
				// Match on the id attribute are the same
				// myDiff.overrideElementQualifier(new
				// ElementNameAndAttributeQualifier("id"));
				myDiff.overrideDifferenceListener(new IgnoreTextAndAttributeValuesDifferenceListener());
				Assert.assertTrue(String.format("Verifying Test Output: %s%s", file.getName(), myDiff), myDiff.similar());
			}
		}
	}

	/**
	 * Callback Listener. Receives event notifications from CPE.
	 *
	 *
	 */
	class StatusCallbackListenerImpl implements StatusCallbackListener {
		int entityCount = 0;
		long size = 0;
		File expected = null;
		File generated = null;

		public StatusCallbackListenerImpl(File expected_, File generated_) {
			this.expected = expected_;
			this.generated = generated_;
		}

		/**
		 * Called when the initialization is completed.
		 *
		 * @see org.apache.uima.collection.processing.StatusCallbackListener#initializationComplete()
		 */
		public void initializationComplete() {
			logger.info("CPM Initialization Complete");
		}

		/**
		 * Called when the batchProcessing is completed.
		 *
		 * @see org.apache.uima.collection.processing.StatusCallbackListener#batchProcessComplete()
		 *
		 */
		public void batchProcessComplete() {
			logger.info("Completed " + entityCount + " documents");

		}

		/**
		 * Called when the collection processing is completed.
		 *
		 * @see org.apache.uima.collection.processing.StatusCallbackListener#collectionProcessComplete()
		 */
		public void collectionProcessComplete() {

			logger.info("Completed " + entityCount + " documents");
			num_cpe--;
			// Only compare after the batch has been completed.
			try {
				compareXMLOutput(expected, generated);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		/**
		 * Called when the CPM is paused.
		 *
		 * @see org.apache.uima.collection.processing.StatusCallbackListener#paused()
		 */
		public void paused() {
			logger.info("Paused");
		}

		/**
		 * Called when the CPM is resumed after a pause.
		 *
		 * @see org.apache.uima.collection.processing.StatusCallbackListener#resumed()
		 */
		public void resumed() {
			logger.info("Resumed");
		}

		/**
		 * Called when the CPM is stopped abruptly due to errors.
		 *
		 * @see org.apache.uima.collection.processing.StatusCallbackListener#aborted()
		 */
		public void aborted() {
			logger.info("Aborted");
		}

		/**
		 * Called when the processing of a Document is completed. <br>
		 * The process status can be looked at and corresponding actions taken.
		 *
		 * @param aCas
		 *            CAS corresponding to the completed processing
		 * @param aStatus
		 *            EntityProcessStatus that holds the status of all the
		 *            events for aEntity
		 */
		public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
			if (aStatus.isException()) {
				List<?> exceptions = aStatus.getExceptions();
				for (int i = 0; i < exceptions.size(); i++) {
					((Throwable) exceptions.get(i)).printStackTrace();
				}
				return;
			}
			entityCount++;
		}
	}
}
