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
package org.apache.ctakes.assertion.medfacts.cleartk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URISyntaxException;

import org.apache.ctakes.assertion.eval.AssertionEvaluation;
import org.apache.ctakes.assertion.eval.AssertionEvaluation.ReferenceAnnotationsSystemAssertionClearer;
import org.apache.ctakes.assertion.eval.AssertionEvaluation.ReferenceIdentifiedAnnotationsSystemToGoldCopier;
import org.apache.ctakes.assertion.eval.AssertionEvaluation.ReferenceSupportingAnnotationsSystemToGoldCopier;
import org.apache.ctakes.core.cc.FileTreeXmiWriter;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;

public class CreateAssertionDescriptor
{
  
//  public static final Class<? extends DataWriterFactory<String>> dataWriterFactoryClass = MaxentStringOutcomeDataWriter.class;

  /**
   * @param args -
   * @throws URISyntaxException -
   * @throws FileNotFoundException -
   * @throws ResourceInitializationException -
   */
  public static void main(String[] args) throws Exception
  {
    CreateAssertionDescriptor creator = new CreateAssertionDescriptor();
    
    creator.execute();

  }
  
  public void execute() throws Exception
  {
    createTrainDescriptor();
    createTestDescriptor();
  }
  
  public void createTrainDescriptor() throws Exception
  {
    File trainDirectory = new File("/tmp/assertion_data/train");
    File directory = trainDirectory;
    AggregateBuilder builder = new AggregateBuilder();

////
    AnalysisEngineDescription goldCopierIdentifiedAnnotsAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceIdentifiedAnnotationsSystemToGoldCopier.class);
    builder.add(goldCopierIdentifiedAnnotsAnnotator);
    
    AnalysisEngineDescription goldCopierSupportingAnnotsAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceSupportingAnnotationsSystemToGoldCopier.class);
    builder.add(goldCopierSupportingAnnotsAnnotator);
    
    AnalysisEngineDescription assertionAttributeClearerAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceAnnotationsSystemAssertionClearer.class);
    builder.add(assertionAttributeClearerAnnotator);
    
//    String generalSectionRegexFileUri =
//        "org/mitre/medfacts/zoner/section_regex.xml";
//    AnalysisEngineDescription zonerAnnotator =
//        AnalysisEngineFactory.createPrimitiveDescription(ZoneAnnotator.class,
//            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
//            generalSectionRegexFileUri
//            );
//    builder.add(zonerAnnotator);
//
//    String mayoSectionRegexFileUri =
//        "org/mitre/medfacts/uima/mayo_sections.xml";
//    AnalysisEngineDescription mayoZonerAnnotator =
//        AnalysisEngineFactory.createPrimitiveDescription(ZoneAnnotator.class,
//            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
//            mayoSectionRegexFileUri
//            );
//    builder.add(mayoZonerAnnotator);
    
    
    AnalysisEngineDescription polarityAnnotator = AnalysisEngineFactory.createEngineDescription(PolarityCleartkAnalysisEngine.class); //,  this.additionalParamemters);
    ConfigurationParameterFactory.addConfigurationParameters(
        polarityAnnotator,
        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
        AssertionEvaluation.GOLD_VIEW_NAME,
//        CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
//        this.dataWriterFactoryClass.getName(),
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        new File(directory, "polarity").getPath()
        );
    builder.add(polarityAnnotator);

    AnalysisEngineDescription conditionalAnnotator = AnalysisEngineFactory.createEngineDescription(ConditionalCleartkAnalysisEngine.class); //,  this.additionalParamemters);
    ConfigurationParameterFactory.addConfigurationParameters(
        conditionalAnnotator,
        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
        AssertionEvaluation.GOLD_VIEW_NAME,
//        CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
//        this.dataWriterFactoryClass.getName(),
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        new File(directory, "conditional").getPath()
        );
    builder.add(conditionalAnnotator);

    AnalysisEngineDescription uncertaintyAnnotator = AnalysisEngineFactory.createEngineDescription(UncertaintyCleartkAnalysisEngine.class); //,  this.additionalParamemters);
    ConfigurationParameterFactory.addConfigurationParameters(
        uncertaintyAnnotator,
        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
        AssertionEvaluation.GOLD_VIEW_NAME,
//        CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
//        this.dataWriterFactoryClass.getName(),
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        new File(directory, "uncertainty").getPath()
        );
    builder.add(uncertaintyAnnotator);

    AnalysisEngineDescription subjectAnnotator = AnalysisEngineFactory.createEngineDescription(SubjectCleartkAnalysisEngine.class); //,  this.additionalParamemters);
    ConfigurationParameterFactory.addConfigurationParameters(
        subjectAnnotator,
        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
        AssertionEvaluation.GOLD_VIEW_NAME,
        CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
//        this.dataWriterFactoryClass.getName(),
//        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        new File(directory, "subject").getPath()
        );
    builder.add(subjectAnnotator);

    AnalysisEngineDescription genericAnnotator = AnalysisEngineFactory.createEngineDescription(GenericCleartkAnalysisEngine.class); //,  this.additionalParamemters);
    ConfigurationParameterFactory.addConfigurationParameters(
        genericAnnotator,
        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
        AssertionEvaluation.GOLD_VIEW_NAME,
//        CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
//        this.dataWriterFactoryClass.getName(),
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
        new File(directory, "generic").getPath()
        );
    builder.add(genericAnnotator);
    
////
    
    FileOutputStream outputStream = new FileOutputStream("desc/analysis_engine/assertion_train.xml");
    
    AnalysisEngineDescription description = builder.createAggregateDescription();
    
    description.toXML(outputStream);
  }

  public void createTestDescriptor() throws Exception
  {
    File testDirectory = new File("/tmp/assertion_data/test");
    File directory = testDirectory;
    File testOutputDirectory = new File("/tmp/assertion_data/test_output");
    AggregateBuilder builder = new AggregateBuilder();

////
    AnalysisEngineDescription goldCopierAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceIdentifiedAnnotationsSystemToGoldCopier.class);
    builder.add(goldCopierAnnotator);
    
    AnalysisEngineDescription assertionAttributeClearerAnnotator = AnalysisEngineFactory.createEngineDescription(ReferenceAnnotationsSystemAssertionClearer.class);
    builder.add(assertionAttributeClearerAnnotator);
    
//    String generalSectionRegexFileUri =
//      "org/mitre/medfacts/zoner/section_regex.xml";
//    AnalysisEngineDescription zonerAnnotator =
//        AnalysisEngineFactory.createPrimitiveDescription(ZoneAnnotator.class,
//            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
//            generalSectionRegexFileUri
//            );
//    builder.add(zonerAnnotator);
//
//    String mayoSectionRegexFileUri =
//      "org/mitre/medfacts/uima/mayo_sections.xml";
//    AnalysisEngineDescription mayoZonerAnnotator =
//        AnalysisEngineFactory.createPrimitiveDescription(ZoneAnnotator.class,
//            ZoneAnnotator.PARAM_SECTION_REGEX_FILE_URI,
//            mayoSectionRegexFileUri
//            );
//    builder.add(mayoZonerAnnotator);
    
    AnalysisEngineDescription polarityAnnotator = AnalysisEngineFactory.createEngineDescription(PolarityCleartkAnalysisEngine.class); //,  this.additionalParamemters);
    ConfigurationParameterFactory.addConfigurationParameters(
        polarityAnnotator,
        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
        AssertionEvaluation.GOLD_VIEW_NAME,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        new File(new File(directory, "polarity"), "model.jar").getPath()
        );
    builder.add(polarityAnnotator);

    AnalysisEngineDescription conditionalAnnotator = AnalysisEngineFactory.createEngineDescription(ConditionalCleartkAnalysisEngine.class); //,  this.additionalParamemters);
    ConfigurationParameterFactory.addConfigurationParameters(
        conditionalAnnotator,
        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
        AssertionEvaluation.GOLD_VIEW_NAME,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        new File(new File(directory, "conditional"), "model.jar").getPath()
        );
    builder.add(conditionalAnnotator);
  
    AnalysisEngineDescription uncertaintyAnnotator = AnalysisEngineFactory.createEngineDescription(UncertaintyCleartkAnalysisEngine.class); //,  this.additionalParamemters);
    ConfigurationParameterFactory.addConfigurationParameters(
        uncertaintyAnnotator,
        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
        AssertionEvaluation.GOLD_VIEW_NAME,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        new File(new File(directory, "uncertainty"), "model.jar").getPath()
        );
    builder.add(uncertaintyAnnotator);

    AnalysisEngineDescription subjectAnnotator = AnalysisEngineFactory.createEngineDescription(SubjectCleartkAnalysisEngine.class); //,  this.additionalParamemters);
    ConfigurationParameterFactory.addConfigurationParameters(
        subjectAnnotator,
        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
        AssertionEvaluation.GOLD_VIEW_NAME,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        new File(new File(directory, "subject"), "model.jar").getPath()
        );
    builder.add(subjectAnnotator);

    AnalysisEngineDescription genericAnnotator = AnalysisEngineFactory.createEngineDescription(GenericCleartkAnalysisEngine.class); //,  this.additionalParamemters);
    ConfigurationParameterFactory.addConfigurationParameters(
        genericAnnotator,
        AssertionCleartkAnalysisEngine.PARAM_GOLD_VIEW_NAME,
        AssertionEvaluation.GOLD_VIEW_NAME,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        new File(new File(directory, "generic"), "model.jar").getPath()
        );
    builder.add(genericAnnotator);

    AnalysisEngineDescription xwriter =
    AnalysisEngineFactory.createEngineDescription(
//          XmiWriterCasConsumerCtakes.class,
//          AssertionComponents.CTAKES_CTS_TYPE_SYSTEM_DESCRIPTION,
//          XmiWriterCasConsumerCtakes.PARAM_OUTPUTDIR,
          FileTreeXmiWriter.class,
          ConfigParameterConstants.PARAM_OUTPUTDIR,
          testOutputDirectory);
    builder.add(xwriter);
////
    
    FileOutputStream outputStream = new FileOutputStream("desc/analysis_engine/assertion_test.xml");
    
    AnalysisEngineDescription description = builder.createAggregateDescription();
    
    description.toXML(outputStream);
  }
  
  
}
