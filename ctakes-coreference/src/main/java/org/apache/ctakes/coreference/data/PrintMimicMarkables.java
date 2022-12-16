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
package org.apache.ctakes.coreference.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetectorAnnotatorBIO;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.cr.LuceneCollectionReader;
import org.apache.ctakes.coreference.ae.DeterministicMarkableAnnotator;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

/*
 * This class was created to allow easier data analysis of the coreference candidates ("markables").
 * The main method should be passed a lucene directory with documents, and a file path to write to,
 * and will extract all the markables from the dataset and write them, one markable per line,
 * to the file specified in the second argument.
 */
public class PrintMimicMarkables {

  public static void main(String[] args) throws UIMAException, IOException {
    if(args.length < 2){
      System.err.println("Two required arguments: <lucene index diretory> <output file>");
      System.exit(-1);
    }
    
    CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(LuceneCollectionReader.class,
        LuceneCollectionReader.PARAM_INDEX_DIR,
        args[0],
//        LuceneCollectionReader.PARAM_MAX_WORDS,
//        10000,
        LuceneCollectionReader.PARAM_FIELD_NAME,
        "content");
    
    AnalysisEngineDescription extractor = getMarkableExtractorDescription();
    
    AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(MarkablePrinter.class,
        MarkablePrinter.PARAM_OUTPUT_FILE,
        args[1]);

    AggregateBuilder agg = new AggregateBuilder();
    agg.add(extractor);
    agg.add(writer);
    
    CpeBuilder builder = new CpeBuilder();
    try {
      builder.setReader(reader);
      builder.setAnalysisEngine(agg.createAggregateDescription());
      builder.createCpe(null).process();
    } catch (SAXException | CpeDescriptorException e) {
      e.printStackTrace();
    }    
  }

  public static class MarkablePrinter extends JCasAnnotator_ImplBase {
    public static final String PARAM_OUTPUT_FILE="outputFile";
    @ConfigurationParameter(name=PARAM_OUTPUT_FILE,
        description="File to print markables from the cas, one at a time")
    private String outputFile = null;
    
    private PrintWriter out = null;
    
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException {
      super.initialize(context);
      
      try {
        out = new PrintWriter(this.outputFile);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        throw new ResourceInitializationException(e);
      }
    }
    
    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
      for(Markable m : JCasUtil.select(jcas, Markable.class)){
        StringBuffer buff = new StringBuffer();
        for(BaseToken token : JCasUtil.selectCovered(BaseToken.class, m)){
          buff.append(token.getCoveredText().replace('\n', ' '));
          buff.append(' ');
        }
        if(buff.length() > 0){
          out.println(buff.substring(0, buff.length()-1));
        }
      }
    } 
   
    @Override
    public void collectionProcessComplete()
        throws AnalysisEngineProcessException {
      super.collectionProcessComplete();
      System.err.println("Collection process complete called, closing file writer.");
      out.close();
    }
  }
  
  public static AnalysisEngineDescription getMarkableExtractorDescription() throws ResourceInitializationException{
    AggregateBuilder aggregateBuilder = new AggregateBuilder();

    aggregateBuilder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
    aggregateBuilder.add(SentenceDetectorAnnotatorBIO.getDescription());

    // identify tokens
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription( TokenizerAnnotatorPTB.class ) );
    // merge some tokens
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription( ContextDependentTokenizerAnnotator.class ) );

    // identify part-of-speech tags
    aggregateBuilder.add( POSTagger.createAnnotatorDescription());

    // add dependency parser
    aggregateBuilder.add( ClearNLPDependencyParserAE.createAnnotatorDescription() );

    // add ctakes constituency parses to system view
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription( ConstituencyParser.class,
        ConstituencyParser.PARAM_MODEL_FILENAME,
        "org/apache/ctakes/constituency/parser/models/thyme.bin" ) );
    
    aggregateBuilder.add( AnalysisEngineFactory.createEngineDescription(DeterministicMarkableAnnotator.class));
    return aggregateBuilder.createAggregateDescription();
  }
}
