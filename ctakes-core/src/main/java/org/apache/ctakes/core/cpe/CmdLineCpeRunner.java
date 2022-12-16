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
package org.apache.ctakes.core.cpe;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

import java.io.IOException;
import java.util.List;

/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/17/13
 */
public class CmdLineCpeRunner {


   public static void main( String[] args ) {
      if ( args.length == 0 || args[0].isEmpty() ) {
         System.err.println( "Please provide the path to a cpe.xml in the first argument.\n"
                                   + "If you do not have a cpe.xml you can create one with the cpe gui." );
         System.exit( 1 );
      }
      XMLInputSource xmlInputSource = null;
      try {
         xmlInputSource = new XMLInputSource( args[0] );
      } catch ( IOException ioE ) {
         System.err.println( "Couldn't open cpe xml " + args[0] );
         System.err.println( "  " + ioE.getLocalizedMessage() );
         System.exit( 1 );
      }
      CpeDescription cpeDescription = null;
      try {
         cpeDescription = UIMAFramework.getXMLParser().parseCpeDescription( xmlInputSource );
      } catch ( InvalidXMLException ixmlE ) {
         System.err.println( "Couldn't parse cpe xml " + args[0] );
         System.err.println( "  " + ixmlE.getLocalizedMessage() );
         System.exit( 1 );
      }
      CollectionProcessingEngine collectionProcessingEngine = null;
      try {
         collectionProcessingEngine = UIMAFramework.produceCollectionProcessingEngine( cpeDescription );
      } catch ( ResourceInitializationException riE ) {
         System.err.println( "Couldn't initialize processing engine." );
         System.err.println( "  " + riE.getLocalizedMessage() );
         System.exit( 1 );
      }
      collectionProcessingEngine.addStatusCallbackListener( new MyStatusCallbackListener() );

      try {
         collectionProcessingEngine.process();
      } catch ( ResourceInitializationException riE ) {
         System.err.println( "Couldn't Run processing engine." );
         System.err.println( "  " + riE.getLocalizedMessage() );
         System.exit( 1 );
      }
   }


   /**
    * Callback Listener. Receives event notifications from CPE.
    */
   static private class MyStatusCallbackListener implements StatusCallbackListener {
      /**
       * Start time of CPE initialization
       */
      private long mStartTime = System.currentTimeMillis();

      /**
       * Start time of the processing
       */
      private long mInitCompleteTime;

      int entityCount = 0;

      long size = 0;

      public void initializationComplete() {
         System.out.println("CPM Initialization Complete");
         mInitCompleteTime = System.currentTimeMillis();
      }

      public void batchProcessComplete() {
         System.out.print("Completed " + entityCount + " documents");
         if (size > 0) {
            System.out.print("; " + size + " characters");
         }
         System.out.println();
         long elapsedTime = System.currentTimeMillis() - mStartTime;
         System.out.println("Time Elapsed : " + elapsedTime + " ms ");
      }

      public void collectionProcessComplete() {
         long time = System.currentTimeMillis();
         System.out.print("Completed " + entityCount + " documents");
         if (size > 0) {
            System.out.print("; " + size + " characters");
         }
         System.out.println();
         long initTime = mInitCompleteTime - mStartTime;
         long processingTime = time - mInitCompleteTime;
         long elapsedTime = initTime + processingTime;
         System.out.println("Total Time Elapsed: " + elapsedTime + " ms ");
         System.out.println("Initialization Time: " + initTime + " ms");
         System.out.println("Processing Time: " + processingTime + " ms");

         System.out.println("\n\n ------------------ PERFORMANCE REPORT ------------------\n");
//         System.out.println(mCPE.getPerformanceReport().toString());
         // stop the JVM. Otherwise main thread will still be blocked waiting for
         // user to press Enter.
//         System.exit(1);
      }

      public void paused() {
         System.out.println("Paused");
      }

      public void resumed() {
         System.out.println("Resumed");
      }

      public void aborted() {
         System.out.println("Aborted");
         // stop the JVM. Otherwise main thread will still be blocked waiting for
         // user to press Enter.
         System.exit(1);
      }

      public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
         if (aStatus.isException()) {
            List<Exception> exceptions = aStatus.getExceptions();
            for (Exception exception : exceptions ) {
               exception.printStackTrace();
            }
            return;
         }
         entityCount++;
         String docText = aCas.getDocumentText();
         if (docText != null) {
            size += docText.length();
         }
      }
   }

}
