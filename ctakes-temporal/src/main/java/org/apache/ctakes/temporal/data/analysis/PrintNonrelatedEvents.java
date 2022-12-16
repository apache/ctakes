package org.apache.ctakes.temporal.data.analysis;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.ctakes.temporal.eval.CommandLine;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.XMIReader;
import org.apache.ctakes.temporal.eval.THYMEData;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

public class PrintNonrelatedEvents {
  static interface Options {
    @Option(longName = "xmi")
    public File getXMIDirectory();

    @Option(longName = "patients")
    public CommandLine.IntegerRanges getPatients();

    @Option(longName = "text")
    public File getRawTextDirectory();
  }

  public static void main(String[] args) throws ResourceInitializationException, CASException, AnalysisEngineProcessException {
    Options options = CliFactory.parseArguments(Options.class, args);
    List<Integer> patientSets = options.getPatients().getList();
    List<Integer> trainItems = THYMEData.getPatientSets(patientSets, THYMEData.TRAIN_REMAINDERS);
    List<File> files = THYMEData.getFilesFor(trainItems, options.getRawTextDirectory());

    CollectionReader reader = UriCollectionReader.getCollectionReaderFromFiles(files);
    AggregateBuilder aggregateBuilder = new AggregateBuilder();
    aggregateBuilder.add(UriToDocumentTextAnnotator.getDescription());
    aggregateBuilder.add(AnalysisEngineFactory.createEngineDescription(
        XMIReader.class,
        XMIReader.PARAM_XMI_DIRECTORY,
        options.getXMIDirectory()));

    int totalNumEvents = 0;
    int totalUnrelatedEvents = 0;
    
    for (Iterator<JCas> casIter = new JCasIterator(reader, aggregateBuilder.createAggregate()); casIter.hasNext();) {
      int docNumEvents = 0;
      int docUnrelatedEvents = 0;
      
      JCas jCas = casIter.next();
      JCas goldView = jCas.getView("GoldView");
      
      String docUri = ViewUriUtil.getURI(jCas).toString();
      if(docUri.contains("path")){
        continue;
      }
      
      System.out.println("Processing note " + docUri);
      Set<Annotation> argSet = new HashSet<>();
      for(RelationArgument arg : JCasUtil.select(goldView, RelationArgument.class)){
        argSet.add(arg.getArgument());
      }
      
      for(EventMention goldEvent : JCasUtil.select(goldView, EventMention.class)){
        docNumEvents++;
        if(!argSet.contains(goldEvent)){
          docUnrelatedEvents++;
          System.out.println(String.format("Event at span (%d, %d) with text %s and doctimerel=%s is not involved in any relation.", 
              goldEvent.getBegin(), goldEvent.getEnd(), goldEvent.getCoveredText(), goldEvent.getEvent().getProperties().getDocTimeRel()));
          
        }
      }
      totalNumEvents += docNumEvents;
      totalUnrelatedEvents += docUnrelatedEvents;
      System.out.println(String.format("This document had %d total events, %d of which were not related to anything", docNumEvents, docUnrelatedEvents));
    }
    System.out.println(String.format("This corpus had %d total events, %d of which were not related to anything", totalNumEvents, totalUnrelatedEvents));
  }

}
