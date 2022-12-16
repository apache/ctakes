/**
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
package org.apache.ctakes.assertion.medfacts.cleartk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.cleartk.ml.Feature;
import org.cleartk.ml.encoder.CleartkEncoderException;
import org.cleartk.ml.encoder.features.FeaturesEncoder;
import org.cleartk.ml.liblinear.LibLinearStringOutcomeDataWriter;
import org.cleartk.ml.liblinear.encoder.FeatureNodeArrayEncoder;

import com.google.common.collect.Lists;

import de.bwaldvogel.liblinear.FeatureNode;

public class AssertionFeatureAwareDataWriter extends LibLinearStringOutcomeDataWriter {

  public AssertionFeatureAwareDataWriter(File outputDirectory)
      throws FileNotFoundException {
    super(outputDirectory);
    WritingFeatureNodeArrayEncoder featEncoder = new WritingFeatureNodeArrayEncoder();
    this.setFeaturesEncoder(featEncoder);
  }

  public static class WritingFeatureNodeArrayEncoder implements FeaturesEncoder<FeatureNode[]> {
    private FeatureNodeArrayEncoder encoder = null;
    private Set<String> featureNames = null;
    public static final String LOOKUP_FILE_NAME = "features-lookup.txt";
    public static final String GROUP_FILE_NAME = "feature-groups.txt";
    public static final String[] FEATURE_GROUPS = {"Preceding", "Following", "Covered", "ClosestCue", "TreeFrag", "deppath", "Domain"};
    
    public WritingFeatureNodeArrayEncoder() {
      this(new FeatureNodeArrayEncoder());
    }

    public WritingFeatureNodeArrayEncoder(FeatureNodeArrayEncoder encoder){
      this.encoder = encoder;
      featureNames = new HashSet<>();
    }

    @Override
    public FeatureNode[] encodeAll(Iterable<Feature> features) throws CleartkEncoderException {
      FeatureNode[] encoded = encoder.encodeAll(features);
      for(Feature feature : features){
        String name;
        if(feature.getValue() instanceof Number) {
          name = feature.getName();
        } else {
          name = Feature.createName(new String[]{feature.getName(), feature.getValue().toString()});
        }
        featureNames.add(name);
      }
      return encoded;
    }

    @Override
    public void finalizeFeatureSet(File file) throws IOException {
      encoder.finalizeFeatureSet(file);

      HashMap<String,ArrayList<Integer>> groupIndexMap = new HashMap<>();
      for(String prefix : FEATURE_GROUPS){
        groupIndexMap.put(prefix, new ArrayList<>());
      }
      File outFile = new File(file.getPath(), LOOKUP_FILE_NAME);
      PrintWriter out = new PrintWriter(new FileWriter(outFile));
      for(String featName : featureNames){
        List<Feature> feat = Lists.newArrayList(new Feature(featName, 1.0));
        try {
          FeatureNode encodedNode = encoder.encodeAll(feat)[1]; // index 0 is the bias feature
          out.println(String.format("%s : %d", StringEscapeUtils.escapeJava(featName), encodedNode.getIndex()));
          for(String featGroup : FEATURE_GROUPS){
            if(featName.contains(featGroup)){
              groupIndexMap.get(featGroup).add(encodedNode.getIndex());
            }
          }
        }catch(CleartkEncoderException e){
          throw new IOException(e);
        }
      }
      out.close();
      
      File groupFile = new File(file.getPath(), GROUP_FILE_NAME);
      out = new PrintWriter(new FileWriter(groupFile));
      for(String featGroup : FEATURE_GROUPS){
        ArrayList<Integer> groupIndices = groupIndexMap.get(featGroup); 
        out.print(featGroup);
        out.print(" : ");
        Collections.sort(groupIndexMap.get(featGroup));
        out.println(StringUtils.join(groupIndices, ','));
      }
      out.close();
    }
  }
}
