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
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.core.util.ListFactory;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.relation.AspectualTextRelation;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.CollectionTextRelation;
import org.apache.ctakes.typesystem.type.relation.CoreferenceRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.cr.UriCollectionReader;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class THYMEAnaforaXMLReader extends JCasAnnotator_ImplBase {
  private static Logger LOGGER = Logger.getLogger(THYMEAnaforaXMLReader.class);

  public static final String PARAM_ANAFORA_DIRECTORY = "anaforaDirectory";

  @ConfigurationParameter(
      name = PARAM_ANAFORA_DIRECTORY,
      description = "root directory of the Anafora-annotated files, with one subdirectory for "
          + "each annotated file")
  protected File anaforaDirectory;

  public static final String PARAM_ANAFORA_XML_SUFFIXES = "anaforaSuffixes";

  @ConfigurationParameter(
      name = PARAM_ANAFORA_XML_SUFFIXES,
      mandatory = false,
      description = "list of suffixes that might be added to a file name to identify the Anafora "
          + "XML annotations file; only the first suffix corresponding to a file will be used")
  protected String[] anaforaXMLSuffixes = new String[] {
      ".Temporal-Relations.gold.completed.xml",
      ".Temporal-Relation.gold.completed.xml",
          ".Temporal.dave.completed.xml",
      ".Temporal-Relation-Adjudication.gold.completed.xml",
      ".Temporal-Entity-Adjudication.gold.completed.xml",
      ".temporal.Temporal-Adjudication.gold.completed.xml",
      ".temporal.Temporal-Entities.gold.completed.xml",
      ".Temporal-Entity.gold.completed.xml",
      ".Gold_Temporal_Entities.xml",
      ".Gold_Temporal_Relations.xml"
      };

  public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(THYMEAnaforaXMLReader.class);
  }

  public static AnalysisEngineDescription getDescription(File anaforaDirectory)
      throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        THYMEAnaforaXMLReader.class,
        THYMEAnaforaXMLReader.PARAM_ANAFORA_DIRECTORY,
        anaforaDirectory);
  }

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    // determine source text file
    File textFile = new File(ViewUriUtil.getURI(jCas));
    LOGGER.info("processing " + textFile);

    // determine possible Anafora XML file names
    File corefFile = new File(textFile.getPath() + ".Coreference.gold.completed.xml");
    List<File> possibleXMLFiles = Lists.newArrayList();
    for (String anaforaXMLSuffix : this.anaforaXMLSuffixes) {
      if (this.anaforaDirectory == null) {
        possibleXMLFiles.add(new File(textFile + anaforaXMLSuffix));
      } else {
        possibleXMLFiles.add(new File(textFile.getPath() + anaforaXMLSuffix));
      }
    }

    // find an Anafora XML file that actually exists
    File xmlFile = null;
    for (File possibleXMLFile : possibleXMLFiles) {
      if (possibleXMLFile.exists()) {
        xmlFile = possibleXMLFile;
        break;
      }
    }
    if (this.anaforaXMLSuffixes.length > 0 && xmlFile == null) {
      throw new IllegalArgumentException("no Anafora XML file found from " + possibleXMLFiles);
    }

    if(xmlFile != null){
      processXmlFile(jCas, xmlFile);
    }
    if(corefFile.exists()){
    	processXmlFile(jCas, corefFile);
    }
  }
  
  private static void processXmlFile(JCas jCas, File xmlFile) throws AnalysisEngineProcessException{
    // load the XML
    Element dataElem;
    try {
      dataElem = new SAXBuilder().build(xmlFile.toURI().toURL()).getRootElement();
    } catch (MalformedURLException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (JDOMException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }

    int curEventId = 1;
    int curTimexId = 1;
    int curRelId = 1;
    int docLen = jCas.getDocumentText().length();
    
    for (Element annotationsElem : dataElem.getChildren("annotations")) {

      Map<String, Annotation> idToAnnotation = Maps.newHashMap();
      for (Element entityElem : annotationsElem.getChildren("entity")) {
        String id = removeSingleChildText(entityElem, "id", null);
        Element spanElem = removeSingleChild(entityElem, "span", id);
        String type = removeSingleChildText(entityElem, "type", id);
        Element propertiesElem = removeSingleChild(entityElem, "properties", id);

        // UIMA doesn't support disjoint spans, so take the span enclosing
        // everything
        int begin = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        for (String spanString : spanElem.getText().split(";")) {
          String[] beginEndStrings = spanString.split(",");
          if (beginEndStrings.length != 2) {
            error("span not of the format 'number,number'", id);
          }
          int spanBegin = Integer.parseInt(beginEndStrings[0]);
          int spanEnd = Integer.parseInt(beginEndStrings[1]);
          if (spanBegin < begin && spanBegin >= 0) {
            begin = spanBegin;
          }
          if (spanEnd > end && spanEnd <= docLen) {
            end = spanEnd;
          }
        }
        if(begin < 0 || end > docLen){
          error("Illegal begin or end boundary", id);
          continue;
        }

        Annotation annotation;
        if (type.equals("EVENT")) {
          String docTimeRel = removeSingleChildText(propertiesElem, "DocTimeRel", id);
          if (docTimeRel == null) {
            error("no docTimeRel, assuming OVERLAP", id);
            docTimeRel = "OVERLAP";
          }
          String eventType = removeSingleChildText(propertiesElem, "Type", id);
          String degree = removeSingleChildText(propertiesElem, "Degree", id);
          String polarity = removeSingleChildText(propertiesElem, "Polarity", id);
          String contextualModality = removeSingleChildText(propertiesElem, "ContextualModality", id);
          String contextualAspect = removeSingleChildText(propertiesElem, "ContextualAspect", id);
          String permanence = removeSingleChildText(propertiesElem, "Permanence", id);
          EventMention eventMention = new EventMention(jCas, begin, end);
          Event event = new Event(jCas);
          EventProperties eventProperties = new EventProperties(jCas);
          eventProperties.setDocTimeRel(docTimeRel);
          eventProperties.setCategory(eventType);
          eventProperties.setDegree(degree);
          if (polarity.equals("POS")) {
            eventProperties.setPolarity(CONST.NE_POLARITY_NEGATION_ABSENT);
          } else if (polarity.equals("NEG")) {
            eventProperties.setPolarity(CONST.NE_POLARITY_NEGATION_PRESENT);
          } else {
            error("polarity that was not POS or NEG", id);
          }
          eventProperties.setContextualModality(contextualModality);
          eventProperties.setContextualAspect(contextualAspect);
          eventProperties.setPermanence(permanence);
          eventProperties.addToIndexes();
          event.setConfidence(1.0f);
          event.setDiscoveryTechnique(CONST.NE_DISCOVERY_TECH_GOLD_ANNOTATION);
          event.setProperties(eventProperties);
          event.setMentions(new FSArray(jCas, 1));
          event.setMentions(0, eventMention);
          event.addToIndexes();
          eventMention.setId(curEventId++);
          eventMention.setConfidence(1.0f);
          eventMention.setDiscoveryTechnique(CONST.NE_DISCOVERY_TECH_GOLD_ANNOTATION);
          eventMention.setEvent(event);
          eventMention.addToIndexes();
          annotation = eventMention;

        } else if (type.equals("TIMEX3")) {
          String timeClass = removeSingleChildText(propertiesElem, "Class", id);
          TimeMention timeMention = new TimeMention(jCas, begin, end);
          timeMention.setId(curTimexId++);
          timeMention.setTimeClass(timeClass);
          timeMention.addToIndexes();
          annotation = timeMention;

        } else if (type.equals("DOCTIME")) {
          TimeMention timeMention = new TimeMention(jCas, begin, end);
          timeMention.setId(curTimexId++);
          timeMention.setTimeClass(type);
          timeMention.addToIndexes();
          annotation = timeMention;

        } else if (type.equals("SECTIONTIME")) {
          TimeMention timeMention = new TimeMention(jCas, begin, end);
          timeMention.setId(curTimexId++);
          timeMention.setTimeClass(type);
          timeMention.addToIndexes();
          annotation = timeMention;

        } else if (type.equals("Markable")) {
          while(end >= begin && (jCas.getDocumentText().charAt(end-1) == '\n' || jCas.getDocumentText().charAt(end-1) == '\r')){
            end--;
          }
          Markable markable = new Markable(jCas, begin, end);
          markable.addToIndexes();
          annotation = markable;

        } else if (type.equals("DUPLICATE")) {
          LOGGER.warn("Ignoring duplicate sections in annotations.");
          continue;
        } else {
          throw new UnsupportedOperationException("unsupported entity type: " + type);
        }

        // match the annotation to it's ID for later use
        idToAnnotation.put(id, annotation);

        // make sure all XML has been consumed
        removeSingleChild(entityElem, "parentsType", id);
        if (!propertiesElem.getChildren().isEmpty() || !entityElem.getChildren().isEmpty()) {
          List<String> children = Lists.newArrayList();
          for (Element child : propertiesElem.getChildren()) {
            children.add(child.getName());
          }
          for (Element child : entityElem.getChildren()) {
            children.add(child.getName());
          }
          error("unprocessed children " + children, id);
        }
      }

      for (Element relationElem : annotationsElem.getChildren("relation")) {
        String id = removeSingleChildText(relationElem, "id", null);
        String type = removeSingleChildText(relationElem, "type", id);
        Element propertiesElem = removeSingleChild(relationElem, "properties", id);

        if (type.equals("TLINK")) {
          String sourceID = removeSingleChildText(propertiesElem, "Source", id);
          String targetID = removeSingleChildText(propertiesElem, "Target", id);
          String tlinkType = removeSingleChildText(propertiesElem, "Type", id);
          TemporalTextRelation relation = new TemporalTextRelation(jCas);
          relation.setId(curRelId++);
          addRelation(jCas, relation, sourceID, targetID, tlinkType, idToAnnotation, id);

        } else if (type.equals("ALINK")) {
          String sourceID = removeSingleChildText(propertiesElem, "Source", id);
          String targetID = removeSingleChildText(propertiesElem, "Target", id);
          String alinkType = removeSingleChildText(propertiesElem, "Type", id);
          AspectualTextRelation relation = new AspectualTextRelation(jCas);
          addRelation(jCas, relation, sourceID, targetID, alinkType, idToAnnotation, id);

        } else if (type.equals("Identical")) {
          // Build list of Markables from FirstInstance and Coreferring_String annotations:
          String mention = removeSingleChildText(propertiesElem, "FirstInstance", id);
          List<Markable> markables = new ArrayList<>();
          Markable antecedent, anaphor;
          antecedent = (Markable) idToAnnotation.get(mention);
          if(antecedent != null){
            markables.add(antecedent);
          }else{
            error("Null markable as FirstInstance", id);
          }
          List<Element> corefs = propertiesElem.getChildren("Coreferring_String");
          for(Element coref : corefs){
            mention = coref.getText();
            anaphor = (Markable) idToAnnotation.get(mention);
            if(anaphor != null){
              markables.add(anaphor);
            }else{
              error("Null markable as Coreferring_String", id);
            }
          }
          // Iterate over markable list creating binary coref relations:
          for(int antInd = 0; antInd < markables.size()-1; antInd++){
            int anaInd = antInd + 1;
            // create set of binary relations from chain elements:
            CoreferenceRelation pair = new CoreferenceRelation(jCas);
            pair.setCategory("Identity");
            RelationArgument arg1 = new RelationArgument(jCas);
            arg1.setArgument(markables.get(antInd));
            arg1.setRole("antecedent");
            pair.setArg1(arg1);
            RelationArgument arg2 = new RelationArgument(jCas);
            arg2.setArgument(markables.get(anaInd));
            arg2.setRole("anaphor");
            pair.setArg2(arg2);
            pair.addToIndexes();
          }
          // Create FSList from markable list and add to collection text relation:
          if(markables.size() > 1){
            CollectionTextRelation chain = new CollectionTextRelation(jCas);
            FSList list = ListFactory.buildList(jCas, markables);
            list.addToIndexes();
            chain.setMembers(list);
            chain.addToIndexes();
          }else{
            error("Coreference chain of length <= 1", id);
          }
          propertiesElem.removeChildren("Coreferring_String");
        } else if (type.equals("Set/Subset")){
          error("This reader has not implemented reading of Set/Subset relations yet", id);
          
        } else if (type.equals("Whole/Part")){
          error("This reader has not implemented reading of Whole/Part relations yet", id);
          
        } else if (type.equals("Appositive")){
          error("This reader has not implemented reading of Appositive relations yet", id);
          
        } else {
          throw new UnsupportedOperationException("unsupported relation type: " + type);
        }

        // make sure all XML has been consumed
        removeSingleChild(relationElem, "parentsType", id);
        if (!propertiesElem.getChildren().isEmpty() || !relationElem.getChildren().isEmpty()) {
          List<String> children = Lists.newArrayList();
          for (Element child : propertiesElem.getChildren()) {
            children.add(child.getName());
          }
          for (Element child : relationElem.getChildren()) {
            children.add(child.getName());
          }
          error("unprocessed children " + children, id);
        }
      }
    }
  }

  private static Element getSingleChild(Element elem, String elemName, String causeID) {
    List<Element> children = elem.getChildren(elemName);
    if (children.size() != 1) {
      error(String.format("not exactly one '%s' child", elemName), causeID);
    }
    return children.size() > 0 ? children.get(0) : null;
  }

  private static Element removeSingleChild(Element elem, String elemName, String causeID) {
    Element child = getSingleChild(elem, elemName, causeID);
    elem.removeChildren(elemName);
    return child;
  }

  private static String removeSingleChildText(Element elem, String elemName, String causeID) {
    Element child = getSingleChild(elem, elemName, causeID);
    String text = child.getText();
    if (text.isEmpty()) {
      error(String.format("an empty '%s' child", elemName), causeID);
      text = null;
    }
    elem.removeChildren(elemName);
    return text;
  }

  private static void addRelation(
      JCas jCas,
      BinaryTextRelation relation,
      String sourceID,
      String targetID,
      String category,
      Map<String, Annotation> idToAnnotation,
      String causeID) {
    if (sourceID != null && targetID != null) {
      Annotation source = getArgument(sourceID, idToAnnotation, causeID);
      Annotation target = getArgument(targetID, idToAnnotation, causeID);
      if (source != null && target != null) {
        RelationArgument sourceArg = new RelationArgument(jCas);
        sourceArg.setArgument(source);
        sourceArg.addToIndexes();
        RelationArgument targetArg = new RelationArgument(jCas);
        targetArg.setArgument(target);
        targetArg.addToIndexes();
        relation.setCategory(category);
        relation.setArg1(sourceArg);
        relation.setArg2(targetArg);
        relation.addToIndexes();
      }
    }
  }

  private static Annotation getArgument(
      String id,
      Map<String, Annotation> idToAnnotation,
      String causeID) {
    Annotation annotation = idToAnnotation.get(id);
    if (annotation == null) {
      error("no annotation with id " + id, causeID);
    }
    return annotation;
  }

  private static void error(String found, String id) {
    LOGGER.error(String.format("found %s in annotation with ID %s", found, id));
  }

  public static void main(String[] args) throws Exception {
    List<File> files = Lists.newArrayList();
    for (String path : args) {
      files.add(new File(path));
    }
    CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(files);
    AnalysisEngine engine = AnalysisEngineFactory.createEngine(THYMEAnaforaXMLReader.class);
    SimplePipeline.runPipeline(reader, engine);
  }
}
