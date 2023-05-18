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
package org.apache.ctakes.temporal.ae.feature;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TimeWordTypeExtractor<T extends Annotation> implements FeatureExtractor1<T> {

  private static final String FEATURE_NAME = "TimeWordType";
  
//  private static final String LOOKUP_PATH = "/org/apache/ctakes/temporal/time_word_types.txt";
  private static final String LOOKUP_PATH = "org/apache/ctakes/temporal/time_word_types.txt";

  private Map<String, String> wordTypes;
  
  public TimeWordTypeExtractor() throws ResourceInitializationException {
    wordTypes = new HashMap<>();
    try ( final BufferedReader reader
                = new BufferedReader( new InputStreamReader( FileLocator.getAsStream( LOOKUP_PATH ) ) ) ) {
      reader.lines().forEachOrdered( this::loadWordTypes );
    } catch ( IOException | IllegalArgumentException multE ) {
      throw new ResourceInitializationException( multE );
    }

//    this.wordTypes = Maps.newHashMap();
//    URL url = TimeWordsExtractor.class.getResource(LOOKUP_PATH);
//    try {
//      for (String line : Resources.readLines(url, Charsets.US_ASCII)) {
//        String[] typeAndWord = line.split("\\s+");
//        if (typeAndWord.length != 2) {
//          throw new IllegalArgumentException("Expected '<type> <word>', found: " + line);
//        }
//        this.wordTypes.put(typeAndWord[1], typeAndWord[0]);
//      }
//    } catch (IOException e) {
//      throw new ResourceInitializationException(e);
//    }
  }

  static private final Pattern WHITE_SPACE_PATTERN = Pattern.compile( "\\s+" );

  private void loadWordTypes( final String line ) throws IllegalArgumentException {
    final String[] typeAndWord = WHITE_SPACE_PATTERN.split( line.trim() );
    if (typeAndWord.length != 2) {
      throw new IllegalArgumentException("Expected '<type> <word>', found: " + line);
    }
    wordTypes.put( typeAndWord[1], typeAndWord[0] );
  }

  @Override
  public List<Feature> extract(JCas view, Annotation focusAnnotation)
      throws CleartkExtractorException {
    String type = this.wordTypes.get(focusAnnotation.getCoveredText().toLowerCase());
    List<Feature> features;
    if (type == null) {
      features = Collections.emptyList();
    } else {
      features = Collections.singletonList(new Feature(FEATURE_NAME, type));
    }
    return features;
  }
}
