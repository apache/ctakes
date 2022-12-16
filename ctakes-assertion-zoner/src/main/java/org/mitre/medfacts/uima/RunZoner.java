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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.apache.ctakes.core.ae.DocumentIdPrinterAnalysisEngine;
import org.apache.ctakes.core.cc.XmiWriterCasConsumerCtakes;
import org.apache.ctakes.core.cr.TextReader;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;

public class RunZoner
{
  private static Logger logger = Logger.getLogger(RunZoner.class.getName());
  
  File inputDirectory;
  List<File> inputFiles;

  File outputDirectory;
  
  public static void main(String args[]) throws UIMAException, IOException, URISyntaxException
  {
    if (args.length != 2)
    {
      System.err.format("Syntax: %s input_directory output_directory%n", RunZoner.class.getName());
    }
    
    File inputDirectory = new File(args[0]);
    File outputDirectory = new File(args[1]);
    
    List<File> inputFiles = listContentsAll(inputDirectory);
    
    RunZoner runner = new RunZoner();
    runner.setInputDirectory(inputDirectory);
    runner.setInputFiles(inputFiles);
    runner.setOutputDirectory(outputDirectory);
    
    runner.execute();
  }
  
  public static List<File> listContentsAll(File inputDirectory)
  {
    File fileArray[] = inputDirectory.listFiles();
    
    List<File> fileList = Arrays.asList(fileArray);
    return fileList;
  }

  public static List<File> listContentsXmiOnly(File inputDirectory)
  {
    File fileArray[] = inputDirectory.listFiles(new FilenameFilter()
    {
      
      @Override
      public boolean accept(File dir, String name)
      {
        return name.endsWith(".xmi");
      }
    });
    
    List<File> fileList = Arrays.asList(fileArray);
    return fileList;
  }

  public void execute() throws UIMAException, IOException, URISyntaxException
  {
    AggregateBuilder builder = new AggregateBuilder();
    
    TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath();
    
//    CollectionReader reader = 
//        CollectionReaderFactory.createReader(
//          XMIReader.class,
//          typeSystemDescription,
//          XMIReader.PARAM_FILES,
//          inputFiles);

    CollectionReader reader = 
    CollectionReaderFactory.createReader(
      TextReader.class,
      typeSystemDescription,
      TextReader.PARAM_FILES,
      inputFiles);

    
      AnalysisEngineDescription documentIdPrinter =
          AnalysisEngineFactory.createEngineDescription(DocumentIdPrinterAnalysisEngine.class);
      builder.add(documentIdPrinter);
    
      String generalSectionRegexFileUri =
        "org/mitre/medfacts/uima/section_regex.xml";
      //URI generalSectionRegexFileUri =
      //  this.getClass().getClassLoader().getResource("org/mitre/medfacts/zoner/section_regex.xml").toURI();
//      ExternalResourceDescription generalSectionRegexDescription = ExternalResourceFactory.createExternalResourceDescription(
//          SectionRegexConfigurationResource.class, new File(generalSectionRegexFileUri));
      AnalysisEngineDescription zonerAnnotator =
          AnalysisEngineFactory.createEngineDescription(ZoneAnnotator.class,
              ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
              generalSectionRegexFileUri
              );
      builder.add(zonerAnnotator);

      String mayoSectionRegexFileUri =
          "org/mitre/medfacts/uima/mayo_sections.xml";
//      URI mayoSectionRegexFileUri =
//          this.getClass().getClassLoader().getResource("org/mitre/medfacts/zoner/mayo_sections.xml").toURI();
//        ExternalResourceDescription mayoSectionRegexDescription = ExternalResourceFactory.createExternalResourceDescription(
//            SectionRegexConfigurationResource.class, new File(mayoSectionRegexFileUri));
      AnalysisEngineDescription mayoZonerAnnotator =
          AnalysisEngineFactory.createEngineDescription(ZoneAnnotator.class,
              ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
              mayoSectionRegexFileUri
              );
      builder.add(mayoZonerAnnotator);

      AnalysisEngineDescription xWriter = AnalysisEngineFactory.createEngineDescription(
          XmiWriterCasConsumerCtakes.class,
          typeSystemDescription,
          XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
          outputDirectory.toString()
          );
      
      builder.add(xWriter);

    logger.info("BEFORE RUNNING PIPELINE...");
    SimplePipeline.runPipeline(reader,  builder.createAggregateDescription());
    logger.info("AFTER RUNNING PIPELINE...COMPLETED");
  }

  public File getInputDirectory()
  {
    return inputDirectory;
  }

  public void setInputDirectory(File inputDirectory)
  {
    this.inputDirectory = inputDirectory;
  }

  public List<File> getInputFiles()
  {
    return inputFiles;
  }

  public void setInputFiles(List<File> inputFiles)
  {
    this.inputFiles = inputFiles;
  }

  public File getOutputDirectory()
  {
    return outputDirectory;
  }

  public void setOutputDirectory(File outputDirectory)
  {
    this.outputDirectory = outputDirectory;
  }
  
}
