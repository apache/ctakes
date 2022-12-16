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
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class THYMEQAAnaforaXMLReader extends THYMEAnaforaXMLReader {
	private static Logger LOGGER = Logger.getLogger(THYMEQAAnaforaXMLReader.class);
	
  public static AnalysisEngineDescription getDescription(File anaforaDirectory)
      throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        THYMEQAAnaforaXMLReader.class,
        THYMEAnaforaXMLReader.PARAM_ANAFORA_XML_SUFFIXES,
        new String[]{ ".THYME_QA.timi4508.completed.xml",
            ".THYME_QA.gusa3085.completed.xml",
            ".THYME_QA.bethard.completed.xml",
            ".THYME_QA.timi4508.completed.xml",
            ".THYME_QA.dligach.completed.xml"},
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
    Map<String, List<String>> questionRelations = Maps.newHashMap();
    
    for (Element annotationsElem : dataElem.getChildren("annotations")) {

    	// TODO -- need mapping from id to relation
      Map<String, Annotation> idToAnnotation = Maps.newHashMap();
      Map<String, BinaryTextRelation> idToRelation = Maps.newHashMap();
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
          if (spanBegin < begin) {
            begin = spanBegin;
          }
          if (spanEnd > end) {
            end = spanEnd;
          }
        }
        if(begin < 0 || end >= docLen){
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
          String polarity = removeSingleChildText(propertiesElem, "Polarity", id);
          EventMention eventMention = new EventMention(jCas, begin, end);
          Event event = new Event(jCas);
          EventProperties eventProperties = new EventProperties(jCas);
          eventProperties.setDocTimeRel(docTimeRel);
          if (polarity.equals("POS")) {
            eventProperties.setPolarity(CONST.NE_POLARITY_NEGATION_ABSENT);
          } else if (polarity.equals("NEG")) {
            eventProperties.setPolarity(CONST.NE_POLARITY_NEGATION_PRESENT);
          } else {
            error("polarity that was not POS or NEG", id);
          }
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
          idToRelation.put(id, relation);

        } else if (type.equals("ALINK")) {
          String sourceID = removeSingleChildText(propertiesElem, "Source", id);
          String targetID = removeSingleChildText(propertiesElem, "Target", id);
          String alinkType = removeSingleChildText(propertiesElem, "Type", id);
          AspectualTextRelation relation = new AspectualTextRelation(jCas);
          addRelation(jCas, relation, sourceID, targetID, alinkType, idToAnnotation, id);
          idToRelation.put(id, relation);

        } else if (type.equals("Question")){
        	String questionText = removeSingleChildText(propertiesElem, "Question", id);
        	String confidence = removeSingleChildText(propertiesElem, "Confidence", id);
        	String difficulty = removeSingleChildText(propertiesElem, "Difficulty", id);
        	String questionDescription = questionText + " - Confidence: " + confidence + " - Difficulty: " + difficulty;
        	
        	List<Element> answers = propertiesElem.getChildren("Answer");
        	List<String> ids = new ArrayList<>();
        	for(Element answer : answers){
        		ids.add(answer.getText());
        	}
        	propertiesElem.removeChildren("Answer");
        	questionRelations.put(questionDescription, ids);
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
      
      // After reading in all the relations we can create the Question annotations
      for(String question : questionRelations.keySet()){
      	CollectionTextRelation qaRel = new CollectionTextRelation(jCas);
      	qaRel.setCategory(question);
        List<TOP> answerList = new ArrayList<>();
      	for(String id : questionRelations.get(question)){
      		TOP answer = idToAnnotation.get(id);
      		if(answer == null){
      			answer = idToRelation.get(id);
      			if(answer == null){
      				LOGGER.error("cannot find answer for id: " + id);
      			}
      		}
      		answerList.add(answer);
      	}
      	qaRel.setMembers(ListFactory.buildList(jCas, answerList));
      	qaRel.addToIndexes();
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

}
