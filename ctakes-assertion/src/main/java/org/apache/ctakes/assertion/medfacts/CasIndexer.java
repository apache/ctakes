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
package org.apache.ctakes.assertion.medfacts;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class CasIndexer<T extends Annotation>
{
  private Logger LOGGER = LogManager.getLogger(CasIndexer.class.getName());
  private JCas jcas;
  protected Map<Integer, T> mapByAddress;
  protected Type targetType;
  
  public CasIndexer(JCas jcas, Type targetType)
  {
    this.jcas = jcas;
    this.targetType = targetType;
    initialize();
  }
  
  public CasIndexer()
  {
  }

  @SuppressWarnings( "unchecked" )
  public void initialize()
  {
    AnnotationIndex<Annotation> annotationIndex = null;
    if (targetType == null)
    {
      annotationIndex = jcas.getAnnotationIndex();
    } else
    {
      annotationIndex = jcas.getAnnotationIndex(targetType);
    }
    
    mapByAddress = new HashMap<Integer, T>();
    
    //LOGGER.info("    before iterating over all annotations in index...");
    for (Annotation annotation : annotationIndex)
    {
      //LOGGER.info("    begin single annotation");
      Integer address = annotation.getAddress();
      //LOGGER.info(String.format("      address: %d; type: %s", address, annotation.getClass().getName()));
      T current = (T)annotation;
      
      mapByAddress.put(address,  current);
      //LOGGER.info("    end single annotation");
    }
    //LOGGER.info("    after iterating over all annotations in index...");
    
  }
  
  public Annotation lookupByAddress(int address)
  {
    return mapByAddress.get(address);
  }
  
  public String convertToDebugOutput(String label, T targetAnnotation)
  {
    StringWriter sw = new StringWriter();
    PrintWriter p = new PrintWriter(sw);
    
    if (label == null) label = "";
    
    p.format("=== \"%s\" BEGIN [%d] ===%n", label, targetAnnotation.getAddress());
    
    Set<Integer> addressSet = new TreeSet<Integer>();
    if (mapByAddress != null)
    {
      addressSet.addAll(mapByAddress.keySet());
    }
    
    if (!addressSet.isEmpty())
    {
      for (Integer currentAddress : addressSet)
      {
        String highlightIfMatching = currentAddress.equals(targetAnnotation.getAddress()) ? "###" : "";
        p.format("ANNOTATION: address: %d %s%n", currentAddress, highlightIfMatching);
        T currentAnnotation = mapByAddress.get(currentAddress);
        if (currentAnnotation == null)
        {
          p.println("  annotation IS NULL");
        } else
        {
          p.format("  class: %s%n", currentAnnotation.getClass().getName());
          p.format("  value: %s%n", currentAnnotation.toString());
        }
      }
    }
    
    p.format("=== \"%s\" END [%d] ===%n", label, targetAnnotation.getAddress());
    
    String output = sw.toString();
    p.close();
    
    return output;
  }

  public JCas getJcas()
  {
    return jcas;
  }

  public void setJcas(JCas jcas)
  {
    this.jcas = jcas;
  }
}
