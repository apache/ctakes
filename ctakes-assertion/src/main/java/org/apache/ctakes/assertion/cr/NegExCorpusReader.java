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
package org.apache.ctakes.assertion.cr;

import org.apache.ctakes.assertion.medfacts.cleartk.AssertionComponents;
import org.apache.ctakes.assertion.util.AssertionConst;
import org.apache.ctakes.core.cc.FileTreeXmiWriter;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.pipeline.PipelineBuilder;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads lines from file named by AssertionConst.NEGEX_CORPUS
 * If use the main() method, uses this collection reader to
 * read the corpus and then uses the XMI writer to write
 * the XMI to directory given by AssertionConst.NEGEX_CORPUS_PREPROCESSED
 */
@PipeBitInfo(
		name = "NegEx Corpus Reader",
		description = "Reads lines from file named by AssertionConst.NEGEX_CORPUS",
		role = PipeBitInfo.Role.READER,
		products = { PipeBitInfo.TypeProduct.DOCUMENT_ID, PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class NegExCorpusReader extends CollectionReader_ImplBase {
  static Logger LOGGER = LoggerFactory.getLogger(NegExCorpusReader.class);
  
  private boolean skipReadingValuesJustReadText = false;
  
  private static final TypeSystemDescription typeSystemDescription = AssertionComponents.CTAKES_CTS_TYPE_SYSTEM_DESCRIPTION; // TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath();//.createTypeSystemDescription();


  public NegExCorpusReader() {
	  this(false);
  }
  
  public NegExCorpusReader(boolean skipReadingValuesJustReadText) {
	  this.skipReadingValuesJustReadText = skipReadingValuesJustReadText;
	  readAndParseAllLines(null);
  }
  
  private static List<NegExAnnotation> list;
  private static List<NegExAnnotation> readAndParseAllLines(String filename) {
	  if (filename == null || filename.length()==0) {
		  Exception e = new RuntimeException("Going to continue with default values");
		  LOGGER.warn(e.getLocalizedMessage());
		  filename = AssertionConst.NEGEX_CORPUS;
	  }

	  // For each line of data in the file that contains the negex corpus, parse the line and add parsed data to list.
	  String [] lines = readNonWhiteSpaceLines(filename);
	  int n = lines.length;
	  if (n==0) LOGGER.error(n + " lines found in " + filename);
	  LOGGER.info("Processing " + n + " lines from the negex file, treating each line as a document.");

	  list = new ArrayList<>();
	  for (String data : lines) {
		  LOGGER.info("Processing line '" + data + "'.");
		  try {
			  list.add(new NegExAnnotation(data));
		  } catch (RuntimeException e) {
			  LOGGER.warn("Skipping this one because of RuntimeException");
		  } 
	  }
	  
	  return list;
  }

  /**
   * This main method is only for testing purposes. It runs the reader on Knowtator directories.
   * 	args[0] = "/usr/data/MiPACQ/copies-of-just-clinical-knowtator-xml-and-text/";
   * should have a child directory called "text"
   * should have a child directory called "exported-xml"
   * files in knowtator xml directory should have files that end with .xml
   */
  public static void main(String[] args) throws Exception {

	  String filename;
	  if (args.length != 0) {
		  filename = args[0];
	  } else {
		  try {
			  LOGGER.warn(String.format(
					  "usage: java %s path/to/negex/file ",
					  NegExCorpusReader.class.getName()));
		  } catch (IllegalArgumentException e) {
			  e.printStackTrace();
		  }
		  Exception e = new RuntimeException("Going to continue with default values");
		  LOGGER.warn(e.getLocalizedMessage());
		  filename = AssertionConst.NEGEX_CORPUS;
		  LOGGER.warn("filename: " +  filename);
	  }

	  
	  //CollectionReader negexReader = new NegExCorpusReader(false);
	  //List<NegExAnnotation> list = readAndParseAllLines(filename);
//	  CollectionReaderDescription collectionReader = CollectionReaderFactory.createReaderDescription(
//			  NegExCorpusReader.class,
//				typeSystemDescription
//		);
//
//	  //TypeSystemDescription typeSystemDescription = AssertionComponents.TYPE_SYSTEM_DESCRIPTION; // TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath();//.createTypeSystemDescription();
//	  AnalysisEngineDescription xWriter = AnalysisEngineFactory.createEngineDescription(
//			  XmiWriterCasConsumerCtakes.class,
//			  XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
//			  AssertionConst.NEGEX_CORPUS_PREPROCESSED
//			  );
//
//	  AggregateBuilder aggregate = new AggregateBuilder();
//	  aggregate.add(xWriter);
//
//	  SimplePipeline.runPipeline(collectionReader, aggregate.createAggregateDescription());

	  // This is much simpler than the dozen or so lines above.
	  new PipelineBuilder().reader( NegExCorpusReader.class )
								  .set( ConfigParameterConstants.PARAM_OUTPUTDIR,
										  AssertionConst.NEGEX_CORPUS_PREPROCESSED )
								  .add( FileTreeXmiWriter.class )
								  .run();
  }



private static String[] readNonWhiteSpaceLines(String filename) {
	  List<String> lines = new ArrayList<>();
	  BufferedReader br  = null;
	  try {
		  br = new BufferedReader(new FileReader(filename));
		  String line;
		  while ((line=br.readLine())!=null) {
			  if (line.trim().length()>0) {
				  lines.add(line);
			  }
		  }
	  } catch (Exception e) {
		  //
	  } finally {
		  if (br!=null)
			try {
				br.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	  }
	  return lines.toArray(new String[0]);
	  
  }




private static int i = 0;

/**
 * Does more than a typical reader - also creates an IdentifiedAnnotation
 * and a DocumentID annotation
 */
@Override
public void getNext(CAS aCAS) throws IOException, CollectionException {
	JCas jCas;
	try {
		jCas = aCAS.getJCas();
	} catch(CASException e){
		throw new CollectionException(e);
	}
	
	NegExAnnotation a = list.get(i);
	i++;

	jCas.setDocumentText(a.sentenceText);
	DocumentID documentID = new DocumentID(jCas);
	documentID.setDocumentID("doc" + a.lineNumber);
	documentID.addToIndexes();
	EventMention ia = new EventMention(jCas);
	ia.setBegin(Integer.parseInt(a.begin));
	ia.setEnd(Integer.parseInt(a.end));
	if (!skipReadingValuesJustReadText) ia.setPolarity(Integer.parseInt(a.polarity));
	ia.addToIndexes();

}

@Override
public boolean hasNext() throws IOException, CollectionException {

	try {
		return i < list.size();
	} catch (Exception e) { // list == null for example
		throw new CollectionException(e);
	}
			
}



@Override
public void close() throws IOException {
	// TODO Auto-generated method stub
	
}

@Override
public Progress[] getProgress() {
	Progress p = new ProgressImpl(i, list.size(), Progress.ENTITIES);
	return new Progress[]{ p};
}

}
