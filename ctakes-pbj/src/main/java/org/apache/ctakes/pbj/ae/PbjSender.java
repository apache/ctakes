/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.ctakes.pbj.ae;

import org.apache.ctakes.core.cc.XMISerializer;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.apache.ctakes.pbj.util.PbjConstants.*;


/**
 * @author DJ , chip-nlp
 * @since {1/10/22}
 */
abstract public class PbjSender extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PbjSender" );

   static public final String PARAM_SENDER_NAME = "SenderName";
   static public final String PARAM_SENDER_PASS = "SenderPass";
   static public final String PARAM_HOST = "SendHost";
   static public final String PARAM_PORT = "SendPort";
   static public final String PARAM_QUEUE = "SendQueue";
   static public final String PARAM_SEND_STOP = "SendStop";
   static public final String PARAM_QUEUE_SIZE = "QueueSize";

   static public final String DESC_SENDER_NAME = "Your Artemis Username.";
   static public final String DESC_SENDER_PASS = "Your Artemis Password.";
   static public final String DESC_HOST = "The Artemis Host to send information.";
   static public final String DESC_PORT = "The Artemis Port to send information.";
   static public final String DESC_QUEUE = "The Artemis Queue to send information.";
   static public final String DESC_SEND_STOP = "Yes to send a stop signal to Pbj Receivers.";
   static public final String DESC_QUEUE_SIZE = "The size of the Queue.  Default is 5 messages.";

   @ConfigurationParameter(
         name = PARAM_SENDER_NAME,
         description = DESC_SENDER_NAME,
         mandatory = false,
         defaultValue = DEFAULT_USER
   )
   protected String _userName;

   @ConfigurationParameter(
           name = PARAM_SENDER_PASS,
           description = DESC_SENDER_PASS,
           mandatory = false,
           defaultValue = DEFAULT_PASS
   )
   protected String _password;

   @ConfigurationParameter(
         name = PARAM_HOST,
         description = DESC_HOST,
         mandatory = false,
         defaultValue = DEFAULT_HOST
   )
   protected String _host;

   @ConfigurationParameter(
         name = PARAM_PORT,
         description = DESC_PORT,
         mandatory = false
   )
   protected int _port = DEFAULT_PORT;

   @ConfigurationParameter(
         name = PARAM_QUEUE,
         description = DESC_QUEUE
   )
   protected String _queue;

   @ConfigurationParameter(
         name = PARAM_SEND_STOP,
         description = DESC_SEND_STOP,
         mandatory = false,
           defaultValue = DEFAULT_SEND_STOP
   )
   protected String _sendStop;

   @ConfigurationParameter(
         name = PARAM_QUEUE_SIZE,
         description = DESC_QUEUE_SIZE,
         mandatory = false
   )
   protected int _queueSize = DEFAULT_QUEUE_SIZE;


   /**
    *
    * @param host name of the Artemis broker host machine.
    */
   public void setHost( final String host ) {
      _host = host;
   }

   /**
    *
    * @param queue name of the Artemis queue to which the jcas should be sent.
    */
   public void setQueue( final String queue ) {
      _queue = queue;
   }

   /**
    *
    * @param userName username for the Artemis broker.
    */
   public void setUserName( final String userName ) {
      _userName = userName;
   }

   /**
    *
    * @param password user's password for the Artemis broker.
    */
   public void setPassword( final String password ) {
      _password = password;
   }

   /**
    *
    * @param port of the Artemis broker host machine.
    */
   public void setPort( final String port ) {
      try {
         setPort( Integer.parseInt( port ) );
      } catch (NumberFormatException nfE ) {
         LOGGER.warn( "Couldn't set Port on Sender " + nfE.getMessage() );
      }
   }

   /**
    *
    * @param port of the Artemis broker host machine.
    */
   public void setPort( final int port ) {
      _port = port;
   }

   /**
    * Send a stop signal when collection processing has completed.
    */
   public void setSendStop() {
      _sendStop = "yes";
   }

   /**
    *
    * @param queueSize of the Artemis broker host machine.
    */
   public void setQueueSize( final String queueSize ) {
      try {
         setQueueSize( Integer.parseInt( queueSize ) );
      } catch (NumberFormatException nfE ) {
         LOGGER.warn( "Couldn't set Queue size " + nfE.getMessage() );
      }
   }

   /**
    *
    * @param queueSize of the Artemis broker host machine.
    */
   public void setQueueSize( final int queueSize ) {
      _queueSize = queueSize;
   }


   protected String _id = "";
   private XmiCasSerializer _casSerializer;
   private XMISerializer _xmiSerializer;


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      LOGGER.info( "Starting PBJ Sender on " + _host + " " + _queue + " ..." );
      _id = createID();
      connect();
      registerShutdownHook();
   }

   /**
    *
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Sending processed information to " + _host + " " + _queue + " ..." );
//      waitOnQueue();
      sendJCas( jcas );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      sendStop();
      disconnect();
   }

   /**
    *
    * @return a unique ID for the sender.  Basically allows multiple sockets to the same queue from colliding.
    */
   static private String createID() {
      UUID uuid = UUID.randomUUID();
      return uuid.toString();
   }

   private void sendJCas( final JCas jCas ) throws AnalysisEngineProcessException {
      try ( ByteArrayOutputStream outputStream = new ByteArrayOutputStream() ) {
         final CAS cas = jCas.getCas();
         if ( _xmiSerializer == null ) {
            _casSerializer = new XmiCasSerializer( cas.getTypeSystem() );
         }
         if ( _xmiSerializer == null ) {
            _xmiSerializer = new XMISerializer();
         }
         _xmiSerializer.setOutputStream( outputStream );
         _casSerializer.serialize( cas, _xmiSerializer.getContentHandler() );
         final String xmi = outputStream.toString();
         sendText( xmi );
      } catch ( IOException | SAXException multE ) {
         throw new AnalysisEngineProcessException( multE );
      }
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //    Sender stuff
   //
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Wait until the queue can accept more items
    */
   abstract protected void waitOnQueue();

   /**
    * Send text to the queue.
    * @param text -
    * @throws AnalysisEngineProcessException -
    */
   abstract protected void sendText( final String text ) throws AnalysisEngineProcessException;

   /**
    * Send a stop code to the Artemis queue.
    * @throws AnalysisEngineProcessException -
    */
   protected void sendStop() throws AnalysisEngineProcessException {
      if ( !_sendStop.equalsIgnoreCase( "yes" ) ) {
         return;
      }
      LOGGER.info( "Sending Stop code to " + _host + " " + _queue + " ..." );
      try {
         // TODO send stop as MULTICAST to stop all receivers on the queue.  v6 ?
         sendText( STOP_MESSAGE );
      } catch ( AnalysisEngineProcessException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //    Connection, Socket Connect and Disconnect
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Create a connection to the queue.
    * @throws ResourceInitializationException -
    */
   abstract protected  void connect() throws ResourceInitializationException;

   /**
    * Disconnect from the queue.
    * @throws AnalysisEngineProcessException -
    */
   abstract protected void disconnect() throws AnalysisEngineProcessException;


    /**
    * Registers a shutdown hook for the process so that it disconnects when the VM exits.
    * This includes kill signals and user actions like "Ctrl-C".
    */
   private void registerShutdownHook() {
      Runtime.getRuntime()
             .addShutdownHook( new Thread( () -> {
                try {
                   sendStop();
                   disconnect();
                } catch ( AnalysisEngineProcessException aE ) {
                   if ( aE.getMessage() == null || aE.getMessage().equals( "null" ) ) {
                      return;
                   }
                   LOGGER.info( "Artemis Disconnect: " + aE.getMessage() );
                }
             } ) );
   }


}
