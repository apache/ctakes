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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.google.common.collect.Sets;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;
import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.TEMPORAL_RELATION;

@PipeBitInfo(
      name = "I2B2 XML Reader (THYME)",
      description = "Reads annotations from THYME schema I2B2 XML files in a directory.",
      role = PipeBitInfo.Role.SPECIAL,
      products = {  PipeBitInfo.TypeProduct.EVENT, PipeBitInfo.TypeProduct.TEMPORAL_RELATION }
)
public class I2B2TemporalXMLReader extends JCasAnnotator_ImplBase {
  public static final String PARAM_INPUT_DIR = "PARAM_INPUT_DIR";
  @ConfigurationParameter(
      name=PARAM_INPUT_DIR,
      mandatory=true, 
      description="Directory containing i2b2 files to read")
  protected File inputDir;
  
  public static final String PARAM_MAP_THYME = "PARAM_MAP_THYME";
  @ConfigurationParameter(
      name=PARAM_MAP_THYME,
      mandatory=false,
      description="Whether to map i2b2 relations/properties/types to THYME types")
  protected boolean mapThyme=false;
  
  private static final Set<String> beforeSet = Sets.newHashSet("BEFORE", "ENDED_BY", "BEFORE_OVERLAP");
  private static final Set<String> afterSet = Sets.newHashSet("BEGUN_BY", "AFTER");
  
  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {
    File textFile = new File(ViewUriUtil.getURI(jcas));
    File xmlFile = new File(textFile.getAbsolutePath().substring(0, textFile.getAbsolutePath().length()-4));
    Map<String,Annotation> id2entity = new HashMap<>();
    
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

    for(Element timexEl : dataElem.getChild("TAGS").getChildren("TIMEX3")){
      int begin = Integer.parseInt(timexEl.getAttributeValue("start"))-1;
      int end = Integer.parseInt(timexEl.getAttributeValue("end"))-1;
      String timeClass = timexEl.getAttributeValue("type");
      TimeMention timex = new TimeMention(jcas, begin, end);
      id2entity.put(timexEl.getAttributeValue("id"), timex);
      timex.setTimeClass(timeClass);
      timex.addToIndexes();
    }
    
    for(Element eventEl : dataElem.getChild("TAGS").getChildren("EVENT")){
      int begin = Integer.parseInt(eventEl.getAttributeValue("start"))-1;
      int end = Integer.parseInt(eventEl.getAttributeValue("end"))-1;
      Event e = new Event(jcas);
      EventProperties props = new EventProperties(jcas);
      EventMention event = new EventMention(jcas, begin, end);
      id2entity.put(eventEl.getAttributeValue("id"), event);
      String polarity = eventEl.getAttributeValue("polarity");
      if(polarity.equals("POS")) event.setPolarity(CONST.NE_POLARITY_NEGATION_ABSENT);
      else if(polarity.equals("NEG")) event.setPolarity(CONST.NE_POLARITY_NEGATION_PRESENT);
      String modality = eventEl.getAttributeValue("modality");
      if(mapThyme){
        if(modality.equals("FACTUAL")){
          props.setContextualModality("ACTUAL");
        }else if(modality.equals("POSSIBLE")){
          props.setContextualModality("HEDGED");
        }else if(modality.equals("HYPOTHETICAL") || modality.equals("CONDITIONAL")){
          props.setContextualModality("HYPOTHETICAL");
        }else if(modality.equals("PROPOSED")){
          props.setContextualModality("GENERIC");
        }
      }else{
        props.setContextualModality(modality);
      }
      e.setProperties(props);
      FSArray mentions = new FSArray(jcas,1);
      mentions.set(0, event);
      e.setMentions(mentions);
      event.setEvent(e);
      e.addToIndexes();
      event.addToIndexes();
      props.addToIndexes();
    }
    
    for(Element linkEl : dataElem.getChild("TAGS").getChildren("TLINK")){
      Annotation fromEnt = id2entity.get(linkEl.getAttributeValue("fromID"));
      Annotation toEnt = id2entity.get(linkEl.getAttributeValue("toID"));
      String cat = linkEl.getAttributeValue("type");
      TemporalTextRelation link = new TemporalTextRelation(jcas);
      RelationArgument arg1 = new RelationArgument(jcas);
      arg1.setArgument(fromEnt);
      link.setArg1(arg1);
      RelationArgument arg2 = new RelationArgument(jcas);
      arg2.setArgument(toEnt);
      link.setArg2(arg2);
      if(mapThyme){
        throw new UnsupportedOperationException("Mapping to THYME relations is not implemented yet!");
      }else{
        if(beforeSet.contains(cat)){
          link.setCategory("BEFORE");
        }else if(afterSet.contains(cat)){
          link.setCategory("AFTER");
        }else{
          link.setCategory("OVERLAP");
        }
//        link.setCategory(cat);
      }
      link.addToIndexes();
      arg1.addToIndexes();
      arg2.addToIndexes();
    }
  }

  public static AnalysisEngineDescription getDescription(File xmlDirectory) throws ResourceInitializationException {
    return AnalysisEngineFactory.createEngineDescription(
        I2B2TemporalXMLReader.class,
        I2B2TemporalXMLReader.PARAM_INPUT_DIR,
        xmlDirectory);
  }
}
