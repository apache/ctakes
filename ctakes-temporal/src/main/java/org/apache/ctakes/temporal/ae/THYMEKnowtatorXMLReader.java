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
package org.apache.ctakes.temporal.ae;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ctakes.core.ae.SHARPKnowtatorXMLReader;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.cr.UriCollectionReader;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

@PipeBitInfo(
      name = "Knowtator XML Reader (THYME)",
      description = "Reads annotations from THYME schema Knowtator XML files in a directory.",
      role = PipeBitInfo.Role.SPECIAL,
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.EVENT,
            PipeBitInfo.TypeProduct.TIMEX, PipeBitInfo.TypeProduct.LOCATION_RELATION,
            PipeBitInfo.TypeProduct.DEGREE_RELATION, PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class THYMEKnowtatorXMLReader extends SHARPKnowtatorXMLReader {

  public static final String PARAM_KNOWTATOR_XML_DIRECTORY = "knowtatorXMLDirectory";

  @ConfigurationParameter(name = PARAM_KNOWTATOR_XML_DIRECTORY, mandatory = true)
  protected File knowtatorXMLDirectory;

  public static AnalysisEngineDescription getDescription(File knowtatorXMLDirectory)
      throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        THYMEKnowtatorXMLReader.class,
        THYMEKnowtatorXMLReader.PARAM_KNOWTATOR_XML_DIRECTORY,
        knowtatorXMLDirectory);
  }
  
  @Override
  protected URI getTextURI(JCas jCas) {
    try {
        return ViewUriUtil.getURI(jCas);
    } catch (AnalysisEngineProcessException e){
    	e.printStackTrace();
    }
    return null;
  }

  @Override
  protected URI getKnowtatorURI(JCas jCas) {
    URI uri = this.getTextURI(jCas);
    File file = new File(uri.getPath());
    String subDir = file.getName().substring(3, 5);
    Matcher matcher = Pattern.compile("^\\d+$").matcher(subDir);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Unrecognized subdirectory naming: " + subDir);
    }
    subDir = String.format("gold_revised_xml_docset%s", subDir);
    String fileName = file.getName() + ".knowtator.xml";
    return new File(new File(this.knowtatorXMLDirectory, subDir), fileName).toURI();
  }

  @Override
  protected String[] getAnnotatorNames() {
    return new String[] { "consensus set annotator team", "consensus set_rel annotator team" };
  }

  private static interface Options {

    @Option(longName = "text")
    public File getRawTextDirectory();

    @Option(longName = "xml")
    public File getKnowtatorXMLDirectory();

    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();
  }

  /**
   * Just runs reader over files. Useful primarily for debugging annotations.
   */
  public static void main(String[] args) throws Exception {
    Options options = CliFactory.parseArguments(Options.class, args);
    List<File> files = new ArrayList<>();
    for (Integer set : options.getPatients().getList()) {
      File setTextDirectory = new File(options.getRawTextDirectory(), "doc" + set);
      for (File file : setTextDirectory.listFiles()) {
        files.add(file);
      }
    }
    CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(files);
    AnalysisEngine engine = AnalysisEngineFactory.createEngine(
        THYMEKnowtatorXMLReader.class,
        THYMEKnowtatorXMLReader.PARAM_KNOWTATOR_XML_DIRECTORY,
        options.getKnowtatorXMLDirectory());
    SimplePipeline.runPipeline(reader, engine);
  }
}
