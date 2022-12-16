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
package org.apache.ctakes.constituency.parser.uima.co;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;

@PipeBitInfo(
		name = "Parser Tree Writer",
		description = "Writes information about TreeBank Nodes to file.",
		role = PipeBitInfo.Role.WRITER
)
public class ParserTreeWriterConsumer extends CasConsumer_ImplBase {

	PrintWriter out = null;
	
	@Override
	public void initialize() throws ResourceInitializationException {
		String fn = (String) getConfigParameterValue("outputFile");
		try {
			out = new PrintWriter(new FileOutputStream(fn));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ResourceInitializationException();
		}
	}
	
	@Override
	public void processCas(CAS aCAS) throws ResourceProcessException {
		// TODO Auto-generated method stub
		JCas jcas;
		try {
			jcas = aCAS.getCurrentView().getJCas();
		} catch (CASException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		FSIterator<Annotation> iterator = jcas.getAnnotationIndex(TopTreebankNode.type).iterator();
		
		while(iterator.hasNext()){
			TopTreebankNode node = (TopTreebankNode) iterator.next();
			out.println(node.getTreebankParse());
		}
		out.flush();
	}
	
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {
		super.collectionProcessComplete(arg0);
		out.flush();
		out.close();
	}

}
