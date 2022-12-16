package org.apache.ctakes.rest.service.response;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.ctakes.core.cc.XMISerializer;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.jcas.JCas;
import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/*
 * Rest web service that takes clinical text
 * as input and produces extracted text as output
 */
final public class XmiFormatter implements ResponseFormatter {


   /**
    * Returns the document information formatted in uima xmi.
    * {@inheritDoc}
    */
   @Override
   public String getResultText( final JCas jCas ) throws AnalysisEngineProcessException {
      try ( final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            OutputStream outputStream = new BufferedOutputStream( byteStream ) ) {
         XmiCasSerializer casSerializer = new XmiCasSerializer( jCas.getTypeSystem() );
         XMISerializer xmiSerializer = new XMISerializer( outputStream );
         casSerializer.serialize( jCas.getCas(), xmiSerializer.getContentHandler() );
         return byteStream.toString();
      } catch ( SAXException | IOException multiE ) {
         throw new AnalysisEngineProcessException( multiE );
      }
   }


}
