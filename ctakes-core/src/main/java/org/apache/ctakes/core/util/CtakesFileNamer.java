package org.apache.ctakes.core.util;

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

import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.initializable.Initializable;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * This is a very simple implementation of XWriterFileNamer that generates file names based on a
 * prefix string and a incrementing counter.
 *
 * @author Philip Ogren
 */

public class CtakesFileNamer implements Initializable {

   /**
    * The parameter name for the configuration parameter that specifies a fixed prefix for all
    * returned file names.
    */
   public static final String PARAM_PREFIX = "prefix";
   @ConfigurationParameter(name = PARAM_PREFIX, description = "specify a prefix that is prepended to all returned file names", defaultValue = "")
   private String prefix;

   /**
    * The parameter name for the configuration parameter that specifies a fixed suffix for all
    * returned file names.
    */
   public static final String PARAM_SUFFIX = "suffix";
   @ConfigurationParameter(name = PARAM_SUFFIX, description = "specify a suffix that is appended to all returned file names", defaultValue = "")
   private String suffix;

   int i = 1;

   public String nameFile( JCas jcas ) {
      String sourceFileName = DocIdUtil.getDocumentID( jcas );
      StringBuilder b = new StringBuilder();
      if ( prefix != null && !prefix.isEmpty() ) {
         b.append( prefix );
      }

      if ( sourceFileName != null
           && !sourceFileName.isEmpty()
           && !sourceFileName.equals( DocIdUtil.NO_DOCUMENT_ID ) ) {
         b.append( sourceFileName );
      } else {
         b.append( i++ );
      }

      if ( suffix != null && !suffix.isEmpty() ) {
         b.append( suffix );
      }

      String calculatedFilename = b.toString();

      return calculatedFilename;
   }

   public void initialize( UimaContext context ) throws ResourceInitializationException {
      ConfigurationParameterInitializer.initialize( this, context );
   }
}