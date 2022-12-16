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
package org.apache.ctakes.constituency.parser.ae;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.AbstractBottomUpParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.parser.chunking.Parser;

import org.apache.ctakes.core.cr.LinesFromFileCollectionReader;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

@PipeBitInfo(
		name = "Parser Evaluator",
		description = ".."
)
public class ParserEvaluationAnnotator extends JCasAnnotator_ImplBase{

	public static final String PARAM_PARSERMODEL = "ParserModel";
	
	@ConfigurationParameter(
			name = PARAM_PARSERMODEL,
			description = "Parser model file to use for parsing",
			mandatory = false,
			defaultValue = "org/apache/ctakes/constituency/parser/models/sharpacqwsj.bin"
	)
	private File parserModel;
			
	Parser parser = null;
	
	@Override
	public void initialize(org.apache.uima.UimaContext aContext) throws org.apache.uima.resource.ResourceInitializationException {
		super.initialize(aContext);
//		String modelFileOrDirname = (String) aContext.getConfigParameterValue("modelDir");
		try {
//			FileInputStream fis = new FileInputStream(new File(modelFileOrDirname));
//			File parserFile = FileLocator.locateFile(parserModel);
			FileInputStream fis = new FileInputStream(parserModel);
			ParserModel model = new ParserModel(fis);
//			parser = ParserFactory.create(model, AbstractBottomUpParser.defaultBeamSize, AbstractBottomUpParser.defaultAdvancePercentage); //TreebankParser.getParser(modelFileOrDirname, useTagDictionary, useCaseSensitiveTagDictionary, AbstractBottomUpParser.defaultBeamSize, AbstractBottomUpParser.defaultAdvancePercentage);
			parser = new Parser(model, AbstractBottomUpParser.defaultBeamSize, AbstractBottomUpParser.defaultAdvancePercentage);
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		String sent = jcas.getDocumentText();
		StringBuffer buff = new StringBuffer();
		Parse parse = null;
		try{
			parse = ParserTool.parseLine(sent, parser, 1)[0];
			parse.show(buff);
		}catch(NullPointerException e){
			System.err.println("Parser error... no parse found... check POS tags of missed sentence (immediately above)");
		}
		if(parse == null){
			buff.append("(S null)");
		}

		TopTreebankNode ttn = new TopTreebankNode(jcas);
		ttn.setTreebankParse(buff.toString());
		ttn.addToIndexes();
	}
	
	public static void main(String[] args) throws UIMAException, IOException{
		if(args.length < 2){
			System.err.println("Requires 2 arguments: <input file> <output file>");
			System.exit(-1);
		}
		
		CollectionReader reader = CollectionReaderFactory.createReader(LinesFromFileCollectionReader.class,
				LinesFromFileCollectionReader.PARAM_INPUT_FILE_NAME,
				args[0]);
		PrintWriter out = new PrintWriter(args[1]);
		AnalysisEngine ae = AnalysisEngineFactory.createEngine(ParserEvaluationAnnotator.class, new Object[]{});
		
		JCas jcas = null;
		JCasIterator casIter = new JCasIterator(reader, ae);
		while(casIter.hasNext()){
			jcas = casIter.next();
			Collection<TopTreebankNode> nodes = JCasUtil.select(jcas, TopTreebankNode.class);
			for(TopTreebankNode tree : nodes){
				out.println(tree.getTreebankParse());
			}
		}
		out.close();
		
	}
}
