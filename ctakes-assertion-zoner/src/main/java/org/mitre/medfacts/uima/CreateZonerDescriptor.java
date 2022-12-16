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
package org.mitre.medfacts.uima;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;

public class CreateZonerDescriptor
{

  /**
   * @param args
   * @throws URISyntaxException 
   * @throws FileNotFoundException 
   * @throws ResourceInitializationException 
   */
  public static void main(String[] args) throws Exception
  {
    CreateZonerDescriptor creator = new CreateZonerDescriptor();
    
    creator.execute();

  }
  
  public void execute() throws Exception
  {
    AggregateBuilder builder = new AggregateBuilder();

//    AnalysisEngineDescription documentIdPrinter =
//        AnalysisEngineFactory.createEngineDescription(DocumentIdPrinterAnalysisEngine.class);
//    builder.add(documentIdPrinter);
  
    URI generalSectionRegexFileUri =
      this.getClass().getClassLoader().getResource("org/mitre/medfacts/uima/section_regex.xml").toURI();
//    ExternalResourceDescription generalSectionRegexDescription = ExternalResourceFactory.createExternalResourceDescription(
//        SectionRegexConfigurationResource.class, new File(generalSectionRegexFileUri));
    AnalysisEngineDescription zonerAnnotator =
        AnalysisEngineFactory.createEngineDescription(ZoneAnnotator.class,
            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
            generalSectionRegexFileUri
            );
    builder.add(zonerAnnotator);

    URI mayoSectionRegexFileUri =
        this.getClass().getClassLoader().getResource("org/mitre/medfacts/uima/mayo_sections.xml").toURI();
//      ExternalResourceDescription mayoSectionRegexDescription = ExternalResourceFactory.createExternalResourceDescription(
//          SectionRegexConfigurationResource.class, new File(mayoSectionRegexFileUri));
    AnalysisEngineDescription mayoZonerAnnotator =
        AnalysisEngineFactory.createEngineDescription(ZoneAnnotator.class,
            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
            mayoSectionRegexFileUri
            );
    builder.add(mayoZonerAnnotator);
    
    FileOutputStream outputStream = new FileOutputStream("desc/aggregateAssertionZoner.xml");
    
    AnalysisEngineDescription description = builder.createAggregateDescription();
    
    description.toXML(outputStream);
  }

}
