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
package org.apache.ctakes.temporal.data.analysis;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.*;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import difflib.Chunk;
import difflib.Delta;
import difflib.Patch;
import difflib.myers.Equalizer;
import difflib.myers.MyersDiff;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

//import javax.annotation.Nullable;

public class CompareFeatureStructures {
  static interface Options {
    @Option(longName = "dir1")
    public File getDirectory1();

    @Option(longName = "dir2")
    public File getDirectory2();

    @Option(longName = "roots", defaultValue = {
        "org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation",
        "org.apache.ctakes.typesystem.type.relation.Relation" })
    public List<String> getAnnotationClassNames();
  }

  public static void main(String[] args) throws Exception {
    Options options = CliFactory.parseArguments(Options.class, args);
    List<Class<?>> annotationClasses = Lists.newArrayList();
    for (String annotationClassName : options.getAnnotationClassNames()) {
      annotationClasses.add(Class.forName(annotationClassName));
    }

    MyersDiff<String> stringDiff = new MyersDiff<String>();
    MyersDiff<FeatureStructure> fsDiff =
        new MyersDiff<FeatureStructure>(new FeatureStructureEqualizer());

    File originalDir = options.getDirectory1();
    File revisedDir = options.getDirectory2();
    Patch<String> dirPatch = stringDiff.diff(originalDir.list(), revisedDir.list());
    if (!dirPatch.getDeltas().isEmpty()) {
      log("--- %s files\n", originalDir);
      log("+++ %s files\n", revisedDir);
      log(dirPatch);
    } else {
      for (String fileName : originalDir.list()) {
        File originalFile = new File(originalDir, fileName);
        File revisedFile = new File(revisedDir, fileName);
        JCas originalJCas = readXMI(originalFile);
        JCas revisedJCas = readXMI(revisedFile);
        List<String> originalViews = getViewNames(originalJCas);
        List<String> revisedViews = getViewNames(revisedJCas);
        Patch<String> viewsPatch = stringDiff.diff(originalViews, revisedViews);
        if (!viewsPatch.getDeltas().isEmpty()) {
          log("--- %s views\n", originalFile);
          log("+++ %s views\n", revisedFile);
          log(viewsPatch);
        } else {
          for (String viewName : originalViews) {
            JCas originalView = originalJCas.getView(viewName);
            JCas revisedView = revisedJCas.getView(viewName);
            List<FeatureStructure> originalFSes =
                toFeatureStructures(originalView, annotationClasses);
            List<FeatureStructure> revisedFSes =
                toFeatureStructures(revisedView, annotationClasses);
            Patch<FeatureStructure> fsPatch = fsDiff.diff(originalFSes, revisedFSes);
            if (!fsPatch.getDeltas().isEmpty()) {
              log("--- %s view %s\n", originalFile, viewName);
              log("+++ %s view %s\n", revisedFile, viewName);
              for (Delta<FeatureStructure> fsDelta : fsPatch.getDeltas()) {
                logHeader(fsDelta);
                switch (fsDelta.getType()) {
                case DELETE:
                case INSERT:
                  log(fsDelta);
                  break;
                case CHANGE:
                  List<String> originalLines = toLines(fsDelta.getOriginal().getLines());
                  List<String> revisedLines = toLines(fsDelta.getRevised().getLines());
                  Patch<String> linesPatch = stringDiff.diff(originalLines, revisedLines);
                  ListMultimap<Integer, String> deletes = ArrayListMultimap.create();
                  ListMultimap<Integer, String> inserts = ArrayListMultimap.create();
                  Set<Integer> skips = Sets.newHashSet();
                  for (Delta<String> linesDelta : linesPatch.getDeltas()) {
                    Chunk<String> originalChunk = linesDelta.getOriginal();
                    Chunk<String> revisedChunk = linesDelta.getRevised();
                    int start = originalChunk.getPosition();
                    deletes.putAll(start, originalChunk.getLines());
                    inserts.putAll(start, revisedChunk.getLines());
                    for (int i = start; i < start + originalChunk.size(); ++i) {
                      skips.add(i);
                    }
                  }
                  for (int i = 0; i < originalLines.size(); ++i) {
                    if (!skips.contains(i)) {
                      log(" %s\n", originalLines.get(i));
                    }
                    for (String line : deletes.get(i)) {
                      log("-%s\n", line);
                    }
                    for (String line : inserts.get(i)) {
                      log("+%s\n", line);
                    }
                  }
                  break;
                }
              }
            }
          }
        }
      }
    }

  }

  private static <T> void log(String message, Object... args) {
    System.err.printf(message, args);
  }

  private static <T> void log(Patch<T> patch) {
    for (Delta<T> delta : patch.getDeltas()) {
      logHeader(delta);
      log(delta);
    }
  }

  private static <T> void logHeader(Delta<T> delta) {
    Chunk<T> original = delta.getOriginal();
    Chunk<T> revised = delta.getRevised();
    log(
        "@@ -%d,%d +%d,%d @@\n",
        original.getPosition(),
        original.size(),
        revised.getPosition(),
        revised.size());
  }

  private static <T> void log(Delta<T> delta) {
    Chunk<T> original = delta.getOriginal();
    Chunk<T> revised = delta.getRevised();
    for (T line : original.getLines()) {
      log("-%s\n", line.toString().replaceAll("\n", "\n-"));
    }
    for (T line : revised.getLines()) {
      log("+%s\n", line.toString().replaceAll("\n", "\n+"));
    }
  }

  private static JCas readXMI(File xmiFile) throws Exception {
    JCas jCas = JCasFactory.createJCas();
    FileInputStream inputStream = new FileInputStream(xmiFile);
    try {
      XmiCasDeserializer.deserialize(inputStream, jCas.getCas());
    } finally {
      inputStream.close();
    }
    return jCas;
  }

  private static List<String> getViewNames(JCas jCas) throws CASException {
    List<String> viewNames = Lists.newArrayList();
    Iterator<JCas> viewIter = jCas.getViewIterator();
    while (viewIter.hasNext()) {
      viewNames.add(viewIter.next().getViewName());
    }
    return viewNames;
  }

  private static List<FeatureStructure> toFeatureStructures(
      JCas jCas,
      List<Class<?>> annotationClasses) {
    List<FeatureStructure> fsList = Lists.newArrayList();
    for (Class<?> annotationClass : annotationClasses) {
      Type type = JCasUtil.getType(jCas, annotationClass);
      Iterators.addAll(fsList, jCas.getFSIndexRepository().getAllIndexedFS(type));
    }
    return BY_TYPE_AND_OFFSETS.sortedCopy(fsList);
  }

  private static final Ordering<FeatureStructure> BY_TYPE_AND_OFFSETS =
      Ordering.natural().<Comparable<?>> lexicographical().onResultOf(
          new Function<FeatureStructure, Iterable<Comparable<?>>>() {
            @Override
            //  All of guava just for two @Nullable?  temporal has about 100 you be outdated errors and warnings.
            // findbugs is the least of worries.
//            public Iterable<Comparable<?>> apply(@Nullable FeatureStructure input) {
            public Iterable<Comparable<?>> apply( FeatureStructure input ) {
              List<Integer> offsets = Lists.newArrayList();
              this.findOffsets(input, offsets);
              List<Comparable<?>> result =
                  Lists.<Comparable<?>> newArrayList(input.getType().getName());
              result.addAll(Ordering.natural().sortedCopy(offsets));
              return result;
            }

            private void findOffsets(FeatureStructure input, List<Integer> offsets) {
              if (input != null) {
                if (input instanceof Annotation) {
                  Annotation annotation = (Annotation) input;
                  offsets.add(annotation.getBegin());
                  offsets.add(annotation.getEnd());
                } else if (input instanceof FSArray) {
                  FSArray fsArray = (FSArray) input;
                  for (int i = 0; i < fsArray.size(); ++i) {
                    this.findOffsets(fsArray.get(i), offsets);
                  }
                } else if (input instanceof NonEmptyFSList) {
                  NonEmptyFSList fsList = (NonEmptyFSList) input;
                  this.findOffsets(fsList.getHead(), offsets);
                  this.findOffsets(fsList.getTail(), offsets);
                } else {
                  for (Feature feature : input.getType().getFeatures()) {
                    if (!feature.getRange().isPrimitive()) {
                      this.findOffsets(input.getFeatureValue(feature), offsets);
                    }
                  }
                }
              }
            }
          });

  public static List<String> toLines(List<FeatureStructure> fsList) {
    List<String> lines = Lists.newArrayList();
    for (FeatureStructure fs : fsList) {
      for (String line : fs.toString().split("\n")) {
        lines.add(line);
      }
    }
    return lines;
  }

  static class FeatureStructureEqualizer implements Equalizer<FeatureStructure> {

    @Override
    public boolean equals(FeatureStructure original, FeatureStructure revised) {
      return this.equals(original, revised, Lists.<FeatureStructure> newArrayList());
    }

    private boolean equals(
        FeatureStructure original,
        FeatureStructure revised,
        List<FeatureStructure> seen) {
      if (!seen.contains(original) && !seen.contains(revised)) {
        seen.add(original);
        seen.add(revised);
        for (Feature feature : original.getType().getFeatures()) {
          if (feature.getName().equals("uima.cas.AnnotationBase:sofa")) {
            continue;
          }
          if (feature.getRange().isPrimitive()) {
            String originalValue = original.getFeatureValueAsString(feature);
            String revisedValue = revised.getFeatureValueAsString(feature);
            if (!Objects.equal(originalValue, revisedValue)) {
              return false;
            }
          } else {
            FeatureStructure originalValue = original.getFeatureValue(feature);
            FeatureStructure revisedValue = revised.getFeatureValue(feature);
            if (originalValue == null
                || revisedValue == null
                || !originalValue.getType().getName().equals(revisedValue.getType().getName())) {
              if (!Objects.equal(originalValue, revisedValue)) {
                return false;
              }
            } else {
              if (!this.equals(originalValue, revisedValue, seen)) {
                return false;
              }
            }
          }
        }
      }
      return true;
    }
  }
}
