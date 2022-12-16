/**
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
package org.apache.ctakes.core.util;

import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.uima.jcas.JCas;

/**
 * Utility class for fetching document id
 * @deprecated use DocIdUtil in (sub) package doc
 */
@Deprecated
final public class DocumentIDAnnotationUtil {

   // Added for CTAKES-365
   static public final String NO_DOCUMENT_ID = DocIdUtil.NO_DOCUMENT_ID;
   static public final String NO_DOCUMENT_ID_PREFIX = DocIdUtil.NO_DOCUMENT_ID_PREFIX;

   private DocumentIDAnnotationUtil() {
   }

   /**
    * Check the jcas for a document id.  Unlike {@link #getDeepDocumentId(JCas)},
    * this method does not progress into deeper jcas layers/views.
    *
    * @param jcas ye olde ...
    * @return the document id contained in the type "DocumentID" or {@link #NO_DOCUMENT_ID}
    */
   @Deprecated
   public static String getDocumentID( final JCas jcas ) {
      return DocIdUtil.getDocumentID( jcas );
   }


   /**
    * Gets the document Id by progressing through 3 layers until an Id is found: starting JCas, Initial View, Plaintext View
    *
    * @param startingJcas initial JCas to start the checking
    * @return Document Id from the starting JCas, the Initial View, the Plaintext View, or {@link #NO_DOCUMENT_ID}
    */
   @Deprecated
   static public String getDeepDocumentId( final JCas startingJcas ) {
      return DocIdUtil.getDeepDocumentId( startingJcas );
   }

   /**
    * Create a unique id for the document that can be used for an output filename or url.
    * Will be the source document file name if possible,
    * otherwise the first 10 characters of the text plus text hashcode,
    * or "Unknown_" and the current millis if there is no text.
    * Non-alphanumeric characters are replaced with '_'.
    *
    * @param jcas -
    * @return an ok document id
    */
   @Deprecated
   static public String getDocumentIdForFile( final JCas jcas ) {
      return DocIdUtil.getDocumentIdForFile( jcas );
   }

   /**
    * Check the jcas for a document id prefix.  Unlike {@link #getDeepDocumentId(JCas)},
    * this method does not progress into deeper jcas layers/views.
    *
    * @param jcas ye olde ...
    * @return the document id prefix contained in the type "DocumentIdPrefix" or {@link #NO_DOCUMENT_ID}
    */
   @Deprecated
   public static String getDocumentIdPrefix( final JCas jcas ) {
      return DocIdUtil.getDocumentIdPrefix( jcas );
   }

}
