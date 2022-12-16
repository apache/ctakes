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
package org.apache.ctakes.relationextractor.pipelines;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.ctakes.relationextractor.ae.ModifierExtractorAnnotator;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.relationextractor.eval.ModifierExtractorEvaluation;
import org.apache.ctakes.relationextractor.eval.ParameterSettings;
import org.apache.ctakes.relationextractor.eval.RelationExtractorEvaluation;
import org.apache.ctakes.relationextractor.eval.SHARPXMI;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.DegreeOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.xml.sax.SAXException;

import com.google.common.collect.ObjectArrays;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

/**
 * This class produces production models for the RelationExtractor module.
 * Specifically it produces model and descriptor files for the
 * ModifierExtractor, DegreeOfRelationExtractor, and
 * EntityMentionPairRelationExtractor. Additionally it produces an aggregrate
 * descriptor for the entire pipeline from pre-processing to relation
 * extraction.
 * 
 * @author dmitriy dligach
 * 
 */
public class RelationExtractorTrain {

  static interface Options extends RelationExtractorEvaluation.Options {

    @Option(
        longName = "resources-dir",
        defaultValue = "../ctakes-relation-extractor-models/src/main/resources",
        description = "the directory where models should be written")
    public File getResourcesDirectory();

    @Option(
        longName = "descriptors-dir",
        defaultValue = "desc/analysis_engine",
        description = "the directory where descriptors should be written")
    public File getDescriptorsDirectory();
  }

  public static void main(String[] args) throws Exception {
    final Options options = CliFactory.parseArguments(Options.class, args);
    if (!options.getResourcesDirectory().exists()) {
      throw new IllegalArgumentException("directory not found: "
          + options.getResourcesDirectory().getCanonicalPath());
    }
    if (!options.getDescriptorsDirectory().exists()) {
      throw new IllegalArgumentException("directory not found: "
          + options.getDescriptorsDirectory().getCanonicalPath());
    }
    File resourcesDirectory = options.getResourcesDirectory();
    File descriptorsDirectory = options.getDescriptorsDirectory();

    File preprocessDescFile = new File(descriptorsDirectory, "RelationExtractorPreprocessor.xml");
    if (!preprocessDescFile.exists()) {
      throw new IllegalArgumentException("Can't create aggregate without "
          + preprocessDescFile.getCanonicalPath());
    }

    List<File> trainFiles = SHARPXMI.getAllTextFiles(options.getSharpBatchesDirectory());
    trainFiles = SHARPXMI.toXMIFiles(options.getXMIDirectory(), trainFiles);

    // Initialize model directories
    String modelPathPrefix = "org/apache/ctakes/relation/extractor/models/";
    String modifierModelPath = modelPathPrefix + "modifier_extractor";
    String degreeOfModelPath = modelPathPrefix + "degree_of";
    String locationOfModelPath = modelPathPrefix + "location_of";

    // create the modifier extractor
    System.err.println("Training modifier extractor");
    File modifierTrainDirectory = new File(resourcesDirectory, modifierModelPath);
    ModifierExtractorEvaluation evaluation =
        new ModifierExtractorEvaluation(
            modifierTrainDirectory,
            ModifierExtractorEvaluation.BEST_PARAMETERS);
    evaluation.train(evaluation.getCollectionReader(trainFiles), modifierTrainDirectory);
    AnalysisEngineDescription modifierExtractorDesc =
        AnalysisEngineFactory.createEngineDescription(
            ModifierExtractorAnnotator.class,
            GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
            "/" + modifierModelPath + "/model.jar");
    writeDesc(descriptorsDirectory, ModifierExtractorAnnotator.class, modifierExtractorDesc);

    // create the degree_of extractor
    System.err.println("Training degree_of extractor");
    AnalysisEngineDescription degreeOfRelationExtractorDesc =
        trainRelationExtractor(
            resourcesDirectory,
            degreeOfModelPath,
            trainFiles,
            DegreeOfTextRelation.class,
            descriptorsDirectory);

    // create the location_of extractor
    System.err.println("Training location_of extractor");
    AnalysisEngineDescription locationOfRelationExtractorDesc =
        trainRelationExtractor(
            resourcesDirectory,
            locationOfModelPath,
            trainFiles,
            LocationOfTextRelation.class,
            descriptorsDirectory);

    // create the aggregate
    System.err.println("Assembling relation extraction aggregate");
    AggregateBuilder builder = new AggregateBuilder();
    XMLParser parser = UIMAFramework.getXMLParser();
    XMLInputSource source = new XMLInputSource(preprocessDescFile);
    builder.add(parser.parseAnalysisEngineDescription(source));
    builder.add(modifierExtractorDesc);
    builder.add(degreeOfRelationExtractorDesc);
    builder.add(locationOfRelationExtractorDesc);
    AnalysisEngineDescription aggregateDescription = builder.createAggregateDescription();
    writeDesc(descriptorsDirectory, "RelationExtractorAggregate", aggregateDescription);

    // cleanup unnecessary model files
    for (File modelDir : new File(resourcesDirectory, modelPathPrefix).listFiles()) {
      File modelFile = JarClassifierBuilder.getModelJarFile(modelDir);
      for (File file : modelDir.listFiles()) {
        if (!file.equals(modelFile)) {
          file.delete();
        }
      }
    }
  }

  private static AnalysisEngineDescription trainRelationExtractor(
      File resourcesDirectory,
      String modelPath,
      List<File> trainFiles,
      Class<? extends BinaryTextRelation> relationClass,
      File descriptorsDirectory) throws Exception {

    // get the annotator class and best parameters for this relation
    Class<? extends RelationExtractorAnnotator> annotatorClass =
        RelationExtractorEvaluation.ANNOTATOR_CLASSES.get(relationClass);
    ParameterSettings params = RelationExtractorEvaluation.BEST_PARAMETERS.get(relationClass);

    // train the relation extractor
    File trainDirectory = new File(resourcesDirectory, modelPath);
    RelationExtractorEvaluation evaluation =
        new RelationExtractorEvaluation(trainDirectory, relationClass, annotatorClass, params);
    evaluation.train(evaluation.getCollectionReader(trainFiles), trainDirectory);

    // create the description
    Object[] pathParameters =
        new Object[] { GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
            "/" + modelPath + "/model.jar" };
    AnalysisEngineDescription relationExtractorDescription =
        AnalysisEngineFactory.createEngineDescription(
            annotatorClass,
            ObjectArrays.concat(params.configurationParameters, pathParameters, Object.class));

    // write the description
    writeDesc(descriptorsDirectory, annotatorClass, relationExtractorDescription);

    // return the description (for use in an aggregate)
    return relationExtractorDescription;
  }

  private static void writeDesc(
      File descDir,
      Class<?> annotatorClass,
      AnalysisEngineDescription desc) throws SAXException, IOException {
    // set the type system (uimaFIT expands all imports, so this simplifies the
    // descriptor)
    desc.getAnalysisEngineMetaData().setTypeSystem(
        TypeSystemDescriptionFactory.createTypeSystemDescription("org.apache.ctakes.typesystem.types.TypeSystem"));
    writeDesc(descDir, annotatorClass.getSimpleName(), desc);
  }

  private static void writeDesc(File descDir, String name, AnalysisEngineDescription desc)
      throws SAXException,
      IOException {
    // set the name (not done by uimaFIT)
    desc.getMetaData().setName(name);
    File descFile = new File(descDir, name + ".xml");
    System.err.println("Writing description to " + descFile);
    FileOutputStream output = new FileOutputStream(descFile);
    desc.toXML(output);
    output.close();
  }
}
