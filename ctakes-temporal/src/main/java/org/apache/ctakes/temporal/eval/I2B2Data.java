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
package org.apache.ctakes.temporal.eval;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class I2B2Data {

  private static final int trainDevCutoff = 600;   // Nothing special here -- no dev set specified by i2b2 challenege, so I just split it up so there's about 150 train and 40 dev.
  
  static Pattern filePatt = Pattern.compile("(\\d+).xml");
  
  public static List<Integer> getTrainPatientSets(File xmlDirectory) {
    List<Integer> trains = new ArrayList<>();
    
    File[] files = getAllFiles(xmlDirectory, "training");
    Matcher m = null;
    for(File file : files){
      m = filePatt.matcher(file.getName());
      if(m.matches()){
        int ptNum = Integer.parseInt(m.group(1));
        if(ptNum < trainDevCutoff){
          trains.add(ptNum);
        }
      }
    }
    return trains;
  }

  public static List<Integer> getDevPatientSets(File xmlDirectory) {
    List<Integer> devs = new ArrayList<>();
    File[] files = getAllFiles(xmlDirectory, "training");
    Matcher m = null;
    
    for(File file : files){
      m = filePatt.matcher(file.getName());
      if(m.matches()){
        int ptNum = Integer.parseInt(m.group(1));
        if(ptNum >= trainDevCutoff){
          devs.add(ptNum);
        }
      }
    }
    return devs;
  }

  public static List<Integer> getTestPatientSets(File xmlDirectory) {
    List<Integer> tests = new ArrayList<>();
    File[] files = getAllFiles(xmlDirectory, "test");
    Matcher m = null;
    
    for(File file : files){
      m = filePatt.matcher(file.getName());
      if(m.matches()){
        int ptNum = Integer.parseInt(m.group(1));
        tests.add(ptNum);
      }
    }
    return tests;
  }
  
  private static File[] getAllFiles(File xmlDirectory, String sub){
    return new File(xmlDirectory, sub).listFiles(new FilenameFilter() {      
      public boolean accept(File dir, String name) {
        return name.endsWith(".xml");
      }
    });
  }

}
