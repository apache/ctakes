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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.ctakes.ytex.dao.DBUtil;
import org.apache.ctakes.ytex.kernel.SparseDataExporter;
import org.apache.ctakes.ytex.uima.ApplicationContextHolder;
import org.apache.ctakes.ytex.uima.TestUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.uima.fit.pipeline.SimplePipeline;

public class SparseDataExporterTest {
	static final String instanceClassQuery = "select note_id,  fracture, case when note_set = 'train' then 1 else 0 end train from %sfracture_demo";
	static final String numericWordQuery = "select f.note_id, coveredText, COUNT(*) "
			+ "from %1$sanno_token w "
			+ "inner join %1$sanno_base da on w.anno_base_id = da.anno_base_id "
			+ "inner join %1$sdocument d on d.document_id = da.document_id "
			+ "inner join %1$sfracture_demo f on f.note_id = d.instance_id "
			+ "where coveredText is not null "
			+ "and d.analysis_batch = '%2$s' "
			+ "group by f.note_id, coveredText";
	static String analysisBatch;
	static SparseDataExporter exporter;

	/**
	 * set the analysis batch, run the pipeline on the fracture demo
	 * 
	 * @throws ResourceInitializationException
	 * @throws InvalidXMLException
	 * @throws UIMAException
	 * @throws IOException
	 */
	@BeforeClass
	public static void setup() throws ResourceInitializationException,
			InvalidXMLException, UIMAException, IOException {
		analysisBatch = "SparseDataExporterTest-" + System.currentTimeMillis();
		SimplePipeline.runPipeline(TestUtils.getFractureDemoCollectionReader(),
				TestUtils.createTokenizerAE(analysisBatch));
		exporter = ApplicationContextHolder.getApplicationContext().getBean(
				SparseDataExporter.class);
	}

	@Test
	public void testWeka() throws ResourceInitializationException,
			InvalidXMLException, UIMAException, IOException {
		File propFile = setupExportProps("weka");
		exporter.exportData(propFile.getAbsolutePath(), "weka");
		File trainArff = new File(propFile.getParent() + "/train.arff");
		Assert.assertTrue("train.arff should exist", trainArff.exists());
		Assert.assertTrue("train.arff should have a non-trivial size",
				trainArff.length() > 2000);
	}

	@Test
	public void testLibsvm() throws ResourceInitializationException,
			InvalidXMLException, UIMAException, IOException {
		File propFile = setupExportProps("libsvm");
		exporter.exportData(propFile.getAbsolutePath(), "libsvm");
		File train_data = new File(propFile.getParent() + "/train_data.txt");
		assertOutputFileGood(train_data);
	}

	private void assertOutputFileGood(File train_data) {
		Assert.assertTrue(train_data.getName() + " should exist",
				train_data.exists());
		Assert.assertTrue(train_data.getName()
				+ " should have a non-trivial size", train_data.length() > 2000);
	}

	@Test
	public void testSparsematrix() throws ResourceInitializationException,
			InvalidXMLException, UIMAException, IOException {
		File propFile = setupExportProps("sparsematrix");
		exporter.exportData(propFile.getAbsolutePath(), "sparsematrix");
		assertOutputFileGood(new File(propFile.getParent() + "/data.txt"));
	}

	/**
	 * set up export.xml in the specified subdir relative to tempdir. if the
	 * ./target directory exists, create the output there, else use
	 * java.io.tmpdir as output dir.
	 * 
	 * @param subdir
	 * @return file for export.xml
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private File setupExportProps(String subdir) throws FileNotFoundException,
			IOException {
		File baseOutputDir = new File("./target");
		if (!baseOutputDir.exists() || !baseOutputDir.isDirectory()) {
			baseOutputDir = new File(System.getProperty("java.io.tmpdir"));
		}
		File tempDir = new File(baseOutputDir.getAbsolutePath() + "/"
				+ analysisBatch + "/" + subdir);
		tempDir.mkdirs();
		System.out.println("temp dir: " + tempDir);
		Properties props = new Properties();
		props.setProperty("arffRelation", "fracture-word");
		props.setProperty("instanceClassQuery",
				String.format(instanceClassQuery, DBUtil.getYTEXTablePrefix()));
		props.setProperty("numericWordQuery", String.format(numericWordQuery,
				DBUtil.getYTEXTablePrefix(), analysisBatch));
		props.setProperty("outdir", tempDir.getAbsolutePath());
		File propFile = new File(tempDir, "export.xml");
		OutputStream fos = null;
		try {
			fos = new FileOutputStream(propFile);
			props.storeToXML(fos, null);
		} finally {
			fos.close();
		}
		return propFile;
	}
}
