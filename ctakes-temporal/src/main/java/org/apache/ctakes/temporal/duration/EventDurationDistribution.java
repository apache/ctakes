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
package org.apache.ctakes.temporal.duration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ctakes.relationextractor.eval.XMIReader;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;

/**
 * Extract durations of event mentions (e.g. sign/symptom or disease/disorder).
 * 
 * @author dmitriy dligach
 */
public class EventDurationDistribution {

  private static Class<? extends EventMention> targetClass = MedicationMention.class;
  
  public static class Options  {
    @Option(
        name = "--input-dir",
        usage = "specify the path to the directory containing the xmi files",
        required = true)
    public File inputDirectory;
    
    @Option(
        name = "--output-file",
        usage = "specify the path to the output file",
        required = true)
    public String outputFile;
  }
  
	public static void main(String[] args) throws Exception {
		
	  Options options = new Options();
	  CmdLineParser parser = new CmdLineParser(options);
	  parser.parseArgument(args);
	  
	  
		List<File> trainFiles = Arrays.asList(options.inputDirectory.listFiles());
    CollectionReader collectionReader = getCollectionReader(trainFiles);
		
    AnalysisEngine temporalDurationExtractor = AnalysisEngineFactory.createEngine(
    		TemporalDurationExtractor.class,
    		"OutputFile",
    		options.outputFile);
    		
		SimplePipeline.runPipeline(collectionReader, temporalDurationExtractor);
	}
  
  public static class TemporalDurationExtractor extends JCasAnnotator_ImplBase {
    
    @ConfigurationParameter(
        name = "OutputFile",
        mandatory = true,
        description = "path to the output file that will store the distributions")
    private String outputFilePath;
    private File outputFile;
    
    // regular expression to match temporal durations in time mention annotations
    private final static String regex = "(sec|min|hour|hrs|day|week|wk|month|year|yr|decade)";
    
    // mapping between time units and their normalized forms
    private final static Map<String, String> abbreviationToTimeUnit = ImmutableMap.<String, String>builder()
        .put("sec", "second")
        .put("min", "minute")
        .put("hour", "hour")
        .put("hrs", "hour")
        .put("day", "day")
        .put("week", "week")
        .put("wk", "week")
        .put("month", "month")
        .put("year", "year")
        .put("yr", "year")
        .put("decade", "decade")
        .build(); 
    
    // max distance between an event and the time mention that defines the event's duration
    private final static int MAXDISTANCE = 2;

    // regex to match different time units (e.g. 'day', 'month')
    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException  {
      super.initialize(context);
      outputFile = new File(outputFilePath);
      if(outputFile.exists()) {
        System.out.println(outputFile + " exists... deleting...");
        outputFile.delete();
      }
    }
    
    
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

      Collection<DocumentID> ids = JCasUtil.select(jCas, DocumentID.class);
      String fileName = ids.iterator().next().getDocumentID();
      String mentionText = fileName.split("\\.")[0]; // e.g. "smoker.txt"

      // counts of different time units for this sign/symptom
      Multiset<String> durationDistribution = HashMultiset.create();

      for(EventMention mention : JCasUtil.select(jCas, targetClass)) {
        if(mention.getCoveredText().equals(mentionText)) {
          if(isNegated(jCas, mention) || isMedicationPattern(jCas, mention)) {
            continue;
          }

          TimeMention nearestTimeMention = getNearestTimeMention(jCas, mention);
          if(nearestTimeMention == null) {
            continue;
          }
          
          // try to parse this timex with Bethard normalizer
          HashSet<String> timeUnits = Utils.getTimeUnits(nearestTimeMention.getCoveredText());
          if(timeUnits.size() > 0) {
            for(String timeUnit : timeUnits) {
              durationDistribution.add(timeUnit);
            }
          } else {
            // could be an abbreviation e.g. "wks"
            Matcher matcher = pattern.matcher(nearestTimeMention.getCoveredText());
            // need a loop to handle things like 'several days/weeks'
            while(matcher.find()) {
              String matchedTimeUnit = matcher.group(); // e.g. "wks"
              String normalizedTimeUnit = abbreviationToTimeUnit.get(matchedTimeUnit);
              durationDistribution.add(normalizedTimeUnit);
            }            
          }
        }
      }

      if(durationDistribution.size() > 0) { 
        try {
          Files.append(Utils.formatDistribution(mentionText, durationDistribution, ", ", false) + "\n", outputFile, Charsets.UTF_8);
        } catch (IOException e) {
          System.out.println("Could not open output file: " + outputFile);
        } 
      } else {
        System.out.println("No duration data for: " + mentionText);
      }
    }
    
    /**
     * Return true if sign/symptom is negated.
     * TODO: using rules for now; switch to using a negation module
     */
    private static boolean isNegated(JCas jCas, EventMention mention) {
      
      for(BaseToken token : JCasUtil.selectPreceding(jCas, BaseToken.class, mention, 3)) {
        if(token.getCoveredText().equals("no") || 
           token.getCoveredText().equals("not") || 
           token.getCoveredText().equals("off")) {
          return true;
        }
      }
      
      return false;
    }

    /**
     * Return true of this is a medication pattern. 
     * E.g. five (5) ml po qid  (4 times a day) as needed for heartburn for 2 weeks.
     */
    private static boolean isMedicationPattern(JCas jCas, EventMention mention) {
      
      for(BaseToken token : JCasUtil.selectPreceding(jCas, BaseToken.class, mention, 1)) {
        if(token.getCoveredText().equals("for")) {
          return true;
        }
      }
           
      return false;
    }
    
    /**
     * Find nearest time mention on the right that is within allowable distance. 
     * Return null if none found.
     */
    private static TimeMention getNearestTimeMention(JCas jCas, EventMention mention) {

      List<TimeMention> timeMentions = JCasUtil.selectFollowing(jCas, TimeMention.class, mention, 1);
      if(timeMentions.size() < 1) {
        return null;
      }
      
      assert timeMentions.size() == 1;
      
      TimeMention nearestTimeMention = timeMentions.get(0);
      int distance = JCasUtil.selectBetween(jCas, BaseToken.class, mention, nearestTimeMention).size();
      if(distance > MAXDISTANCE) {
        return null;
      }
      
      return nearestTimeMention;
    }
    
    @SuppressWarnings("unused")
    private static String getAnnotationContext(Annotation annotation, int maxContextWindowSize) {
      
      String text = annotation.getCAS().getDocumentText();
      int begin = Math.max(0, annotation.getBegin() - maxContextWindowSize);
      int end = Math.min(text.length(), annotation.getEnd() + maxContextWindowSize);
      
      return text.substring(begin, end).replaceAll("[\r\n]", " ");
    }
    
    @SuppressWarnings("unused")
    private static String formatDistribution(Multiset<String> durationDistribution) {
      
      List<String> durationBins = Arrays.asList("second", "minute", "hour", "day", "week", "month", "year", "decade");
      List<Integer> durationValues = new LinkedList<Integer>();
      
      for(String durationBin : durationBins) {
        durationValues.add(durationDistribution.count(durationBin));
      }

      Joiner joiner = Joiner.on(',');
      return joiner.join(durationValues);
    }
  }
  
  private static CollectionReader getCollectionReader(List<File> items) throws Exception {

    String[] paths = new String[items.size()];
    Collections.sort(items, new FileSizeComparator());
    for (int i = 0; i < paths.length; ++i) {
      paths[i] = items.get(i).getPath();
    }
    
    return CollectionReaderFactory.createReader(
        XMIReader.class,
        XMIReader.PARAM_FILES,
        paths);
  }
  
  public static class FileSizeComparator implements Comparator<File> {

    @Override
    public int compare(File o1, File o2) {
      if(o1.length() > o2.length()){
        return 1;
      }else if(o1.length() < o2.length()){
        return -1;
      }else{
        return 0;
      }
    } 
  }
}
