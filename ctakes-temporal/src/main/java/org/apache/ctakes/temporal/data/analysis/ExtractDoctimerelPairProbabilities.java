package org.apache.ctakes.temporal.data.analysis;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator;
import org.apache.ctakes.temporal.ae.DocTimeRelAnnotator;
import org.apache.ctakes.temporal.ae.EventAnnotator;
import org.apache.ctakes.temporal.pipelines.TemporalExtractionPipeline_ImplBase;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.utils.struct.CounterMap;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ExtractDoctimerelPairProbabilities extends TemporalExtractionPipeline_ImplBase {
  static interface Options {
    @Option(
        shortName = "i",
        description = "specify the path to the directory containing the clinical notes to be processed")
    public String getInputDirectory();
  }
  
  public static void main(String[] args) throws Exception {
    System.out.println("STarting...");
    Options options = CliFactory.parseArguments(Options.class, args);
    CollectionReader collectionReader = CollectionReaderFactory.createReaderFromPath(
        "../ctakes-core/desc/collection_reader/FilesInDirectoryCollectionReader.xml",
          ConfigParameterConstants.PARAM_INPUTDIR,
        options.getInputDirectory()
        );
    
    AggregateBuilder aggregateBuilder = TemporalExtractionPipeline_ImplBase.getPreprocessorAggregateBuilder();
    aggregateBuilder.add(EventAnnotator.createAnnotatorDescription());
    aggregateBuilder.add(BackwardsTimeAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/models/timeannotator/model.jar"));
    aggregateBuilder.add(DocTimeRelAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/models/doctimerel/model.jar"));
    
    CounterMap<String> corpusBefores = new CounterMap<>();
    HashMap<String,CounterMap<String>> corpusPairs = new HashMap<>();
    
    for (Iterator<JCas> casIter = new JCasIterator(collectionReader, aggregateBuilder.createAggregate()); casIter.hasNext();) {
      JCas jCas = casIter.next();
//      String docUri = ViewUriUtil.getURI(jCas).toString();
      Set<String> docBefores = new HashSet<>();
      Set<String> docAfters = new HashSet<>();
      
      for(EventMention event : JCasUtil.select(jCas, EventMention.class)){
        String dtr = event.getEvent().getProperties().getDocTimeRel();
        if(dtr.equals("BEFORE")){
          docBefores.add(event.getCoveredText().toLowerCase());
        }else if(dtr.equals("AFTER")){
          docAfters.add(event.getCoveredText().toLowerCase());
        }
      }
      
      for(String beforeEvent : docBefores){
        corpusBefores.add(beforeEvent, docAfters.size());
        if(!corpusPairs.containsKey(beforeEvent)){
          corpusPairs.put(beforeEvent, new CounterMap<>());
        }
        for(String afterEvent : docAfters){
          corpusPairs.get(beforeEvent).add(afterEvent);
        }
      }
    }
    
    for(String beforeEvent : corpusBefores.keySet()){
      int total = corpusBefores.get(beforeEvent);
      for(String afterEvent : corpusPairs.get(beforeEvent).keySet()){
        int count = corpusPairs.get(beforeEvent).get(afterEvent);
        System.out.println(String.format("%s : %s => %f", beforeEvent, afterEvent, (float) count / total));
      }
      
    }
  }  
}
