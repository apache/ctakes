package org.apache.ctakes.core.pipeline;

import org.apache.ctakes.core.ae.SentenceDetectorAnnotatorBIO;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import java.io.FileWriter;
import java.io.IOException;

public class GenerateSentenceBIODescriptors {

  public static final String sentModelPath = "/org/apache/ctakes/core/models/sentdetect/model.jar";
  public static final String segModelPath = "/org/apache/ctakes/core/models/segdetect/model.jar";
  
  public static void main(String[] args) throws ResourceInitializationException, SAXException, IOException {
    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    aggregateBuilder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
    aggregateBuilder.add(SentenceDetectorAnnotatorBIO.getDescription(sentModelPath));
    
    aggregateBuilder.createAggregateDescription().toXML(new FileWriter("desc/analysis_engine/SentenceAnnotatorBIOAggregate.xml"));
    SentenceDetectorAnnotatorBIO.getDescription(sentModelPath).toXML(new FileWriter("desc/analysis_engine/SentenceDetectorAnnotatorBIO.xml"));    
  }

}
