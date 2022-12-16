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
#!/usr/bin/env groovy
/**
** 	This assumes that you have installed Groovy and 
** 	that you have the command groovy available in your path. 
** 	On Debian/Ubuntu systems, installing Groovy should be as easy as apt-get install groovy.
** 	You can download groovy from http://groovy.codehaus.org/
** 	The first run may be slow since it needs to download all of the dependencies.
**  Usage: $./parser.groovy [inputDir]
**  where inputDir contains the files to be parsed.
** 	Or enable more verbose status $groovy -Dgroovy.grape.report.downloads=true parser.groovy [inputDir]
**/

      
@Grab(group='org.apache.ctakes',
      module='ctakes-core',
            version='4.0.0')
@Grab(group='org.apache.ctakes',
      module='ctakes-core-res',
            version='4.0.0')			
@Grab(group='org.apache.ctakes',
      module='ctakes-constituency-parser',
            version='4.0.0')
@Grab(group='org.apache.ctakes',
      module='ctakes-constituency-parser-res',
            version='4.0.0')
@Grab(group='org.cleartk',
      module='cleartk-util',
      version='0.9.2')
      
@Grab(group='org.apache.uima',
      module='uimafit-core',
      version='2.2.0')

/*
@Grab(group='org.apache.ctakes',
      module='ctakes-clinical-pipeline',
            version='4.0.0')
*/
          
import java.io.File;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.cleartk.util.cr.FilesCollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.SimplePipeline;	
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.factory.TypePrioritiesFactory;
import static org.apache.uima.fit.util.JCasUtil.*;

import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.ctakes.typesystem.type.syntax.TopTreebankNode;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.uima.fit.util.JCasUtil;

		if(args.length < 1) {
		System.out.println("Please specify input directory");
		System.exit(1);
		}
		System.out.println("Reading from directory: " + args[0]);

		CollectionReader collectionReader = FilesCollectionReader.getCollectionReaderWithSuffixes(args[0], CAS.NAME_DEFAULT_SOFA, "txt");
		//Download Models
		//TODO: Separate downloads from URL here is a hack.  
		//Models should really be automatically downloaded from 
		//maven central as part of ctakes-*-res projects/artifacts via @grab.
		//Illustrative purposes until we have all of the *-res artifacts in maven central.
		downloadFile("http://svn.apache.org/repos/asf/ctakes/trunk/ctakes-core-res/src/main/resources/org/apache/ctakes/core/sentdetect/sd-med-model.zip","sd-med-model.zip");
		downloadFile("http://svn.apache.org/repos/asf/ctakes/trunk/ctakes-constituency-parser-res/src/main/resources/org/apache/ctakes/constituency/parser/models/sharpacq-3.1.bin","sharpacq-3.1.bin");

		//Build the pipeline to run
		AggregateBuilder aggregateBuilder = new AggregateBuilder();
		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(SimpleSegmentAnnotator.class));
		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(
            SentenceDetector.class,
            SentenceDetector.SD_MODEL_FILE_PARAM,
            "sd-med-model.zip"));
		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(TokenizerAnnotatorPTB.class));			
		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(
			ConstituencyParser.class,
			ConstituencyParser.PARAM_MODEL_FILENAME,
            "sharpacq-3.1.bin"));
		aggregateBuilder.add(AnalysisEngineFactory.createPrimitiveDescription(Writer.class));
		SimplePipeline.runPipeline(collectionReader, aggregateBuilder.createAggregate());

// Custom writer class used at the end of the pipeline to write results to screen
class Writer extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
  void process(JCas jcas) {
	//Get each Treebanknode and print out the text and it's parse string
    //select(jcas, TopTreebankNode).each { println "${it.treebankParse} "  }
    for(TopTreebankNode node : JCasUtil.select(jcas, TopTreebankNode.class)){
        println(node.getTreebankParse());
    }
  }
}

def downloadFile(String url, String filename) {
	System.out.println("Downloading: " + url);
	def file = new File(filename);
	if(file.exists()) {
	  System.out.println("File already exists:" + filename);
	  return;
	}
    def f = new FileOutputStream(file)
    def out = new BufferedOutputStream(f)
    out << new URL(url).openStream()
    out.close()
}

