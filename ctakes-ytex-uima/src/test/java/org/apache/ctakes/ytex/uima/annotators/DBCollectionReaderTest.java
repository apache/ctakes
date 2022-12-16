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

import java.io.IOException;

import org.apache.ctakes.ytex.uima.TestUtils;
import org.apache.ctakes.ytex.uima.types.DocKey;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.jcas.JCas;
import org.junit.Assert;
import org.junit.Test;
import org.apache.uima.fit.factory.JCasFactory;
import org.xml.sax.SAXException;

public class DBCollectionReaderTest {

	@Test
	public void test() throws IOException,
			SAXException, CpeDescriptorException, UIMAException, CASRuntimeException, CASAdminException {
		CollectionReader colReader = TestUtils.getFractureDemoCollectionReader();
		int count = 0;
		JCas jcas = JCasFactory.createJCasFromPath("src/main/resources/org/apache/ctakes/ytex/types/TypeSystem.xml");
		while(colReader.hasNext()) {
			count++;
			colReader.getNext(jcas.getCas());
			Assert.assertTrue("document should have a dockey", jcas.getAnnotationIndex(DocKey.type).iterator().hasNext());
			jcas.reset();
		}
		Assert.assertTrue("should have read some documents", count > 0);
	}



}
