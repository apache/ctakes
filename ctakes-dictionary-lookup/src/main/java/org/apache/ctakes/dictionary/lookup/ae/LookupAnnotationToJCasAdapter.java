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
package org.apache.ctakes.dictionary.lookup.ae;

import java.util.HashMap;
import java.util.Map;

import org.apache.ctakes.dictionary.lookup.vo.LookupToken;
import org.apache.uima.jcas.tcas.Annotation;


/**
 * @author Mayo Clinic
 */
public class LookupAnnotationToJCasAdapter implements LookupToken {

   final private Map<String, String> _attributeMap;
   final private Annotation _jcasAnnotation;

   public LookupAnnotationToJCasAdapter( final Annotation jcasAnnotation ) {
      _jcasAnnotation = jcasAnnotation;
      _attributeMap = new HashMap<>();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addStringAttribute( final String attrKey, final String attrVal ) {
      _attributeMap.put( attrKey, attrVal );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getEndOffset() {
      return _jcasAnnotation.getEnd();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getLength() {
      return getStartOffset() - getEndOffset();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getStartOffset() {
      return _jcasAnnotation.getBegin();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getStringAttribute( final String attrKey ) {
      return _attributeMap.get( attrKey );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getText() {
      return _jcasAnnotation.getCoveredText();
   }
}
