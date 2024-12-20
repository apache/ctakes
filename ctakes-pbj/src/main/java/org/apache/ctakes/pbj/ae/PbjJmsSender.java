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

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.log.DotLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Enumeration;


/**
 * @author DJ , chip-nlp
 * @since {1/10/22}
 */
@PipeBitInfo(
      name = "PbjJmsSender",
      description = "Sends jcas to Artemis Queue using JMS",
      role = PipeBitInfo.Role.SPECIAL
)
public class PbjJmsSender extends PbjSender {

   static private final Logger LOGGER = LoggerFactory.getLogger( "PbjJmsSender" );

   private MessageProducer _producer;
   private Connection _connection;
   private Session _session;
   private QueueBrowser _queueBrowser;



   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //    Sender stuff
   //
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * {@inheritDoc}
    */
   @Override
   protected void waitOnQueue() {
      try {
         if ( shouldWait( _queueBrowser.getEnumeration() ) ) {
            LOGGER.info( "Waiting on queue ..." );
            keepWaitingOnQueue();
         }
      } catch ( JMSException | InterruptedException multE ) {
         //
      }
   }

   private void keepWaitingOnQueue() throws JMSException, InterruptedException {
      try ( DotLogger dotter = new DotLogger() ) {
         while ( true ) {
            Thread.sleep( 500 );
            if ( !shouldWait( _queueBrowser.getEnumeration() ) ) {
               return;
            }
         }
      } catch ( IOException e ) {
         //
      }
   }

   private boolean shouldWait( final Enumeration enumeration ) {
      int count = 0;
      while ( enumeration.hasMoreElements() ) {
         count++;
         enumeration.nextElement();
         if ( count > _queueSize ) {
            return true;
         }
      }
      LOGGER.info( "Queued Message Count = {}", count );
      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void sendText( final String text ) throws AnalysisEngineProcessException {
      try {
         final TextMessage message = _session.createTextMessage( text );
         _producer.send( message );
      } catch ( JMSException jmsE ) {
         throw new AnalysisEngineProcessException( jmsE );
      }
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //    Connection Close, Disconnect
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * {@inheritDoc}
    */
   @Override
   protected void connect() throws ResourceInitializationException {
      try ( DotLogger dotter = new DotLogger( LOGGER, "Connecting PBJ Sender on {} {} ", _host, _queue ) ) {
         final InitialContext initialContext = new InitialContext();
         final ActiveMQConnectionFactory cf
               = new ActiveMQConnectionFactory( "tcp://" + _host + ":" + _port );
         // Time To Live TTL of -1 asks server to never close this connection.
         cf.setConnectionTTL( -1 );
         cf.setReconnectAttempts( -1 );
         // On the java side we don't need to parse STOMP.  JMS will automatically translate.
         _connection = cf.createConnection();
         _session = _connection.createSession( false, Session.AUTO_ACKNOWLEDGE );
         final ActiveMQQueue queue = new ActiveMQQueue( _queue );
         _producer = _session.createProducer( queue );
         _producer.setTimeToLive( 300000 );
         _connection.start();
         _queueBrowser = _session.createBrowser( queue );
      } catch ( NamingException | JMSException | IOException multE ) {
         throw new ResourceInitializationException( multE );
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   protected void disconnect() throws AnalysisEngineProcessException {
      if ( _connection == null ) {
         return;
      }
      try ( DotLogger dotter = new DotLogger( LOGGER, "Disconnecting PBJ Sender on {} {} ", _host, _queue ) ) {
         _connection.close();
         _connection = null;
      } catch ( JMSException | IOException multE ) {
         LOGGER.error( multE.getMessage() );
         _connection = null;
         throw new AnalysisEngineProcessException( multE );
      }
   }


}
