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
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.log.DotLogger;
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

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.ctakes.pbj.util.PbjUtil.*;


/**
 * @author DJ , chip-nlp
 * @since {1/10/22}
 */
@PipeBitInfo(
      name = "PbjSender",
      description = "Sends jcas to Artemis Queue using Stomp",
      role = PipeBitInfo.Role.SPECIAL
)
public class PbjSender extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "PbjSender" );
   // to add a configuration parameter, type "param" and hit tab.
   static public final String PARAM_SENDER_NAME = "SenderName";
   static public final String PARAM_SENDER_PASS = "SenderPass";
   static public final String PARAM_HOST = "SendHost";
   static public final String PARAM_PORT = "SendPort";
   static public final String PARAM_QUEUE = "SendQueue";
   static public final String PARAM_SEND_STOP = "SendStop";

   static public final String DESC_SENDER_NAME = "Your Artemis Username.";
   static public final String DESC_SENDER_PASS = "Your Artemis Password.";
   static public final String DESC_HOST = "The Artemis Host to send information.";
   static public final String DESC_PORT = "The Artemis Port to send information.";
   static public final String DESC_QUEUE = "The Artemis Queue to send information.";
   static public final String DESC_SEND_STOP = "Yes to send a stop signal to Pbj Receivers.";

   @ConfigurationParameter(
         name = PARAM_SENDER_NAME,
         description = DESC_SENDER_NAME,
         mandatory = false,
         defaultValue = DEFAULT_USER
   )
   private String _userName;

   @ConfigurationParameter(
           name = PARAM_SENDER_PASS,
           description = DESC_SENDER_PASS,
           mandatory = false,
           defaultValue = DEFAULT_PASS
   )
   private String _password;

   @ConfigurationParameter(
         name = PARAM_HOST,
         description = DESC_HOST,
         mandatory = false,
         defaultValue = DEFAULT_HOST
   )
   private String _host;

   @ConfigurationParameter(
         name = PARAM_PORT,
         description = DESC_PORT,
         mandatory = false
   )
   private int _port = DEFAULT_PORT;

   @ConfigurationParameter(
         name = PARAM_QUEUE,
         description = DESC_QUEUE
   )
   private String _queue;

   @ConfigurationParameter(
         name = PARAM_SEND_STOP,
         description = DESC_SEND_STOP,
         mandatory = false,
           defaultValue = DEFAULT_SEND_STOP
   )
   private String _sendStop;


   ////////////////////////////////////////////////////////////////////////////////////////
   //    Constants for the test.    In ctakes we want host, port and queue to be configurable.
   ////////////////////////////////////////////////////////////////////////////////////////

   private static final String END_OF_FRAME = "\u0000";

   static private final Object SOCKET_LOCK = new Object();

   ////////////////////////////////////////////////////////////////////////////////////////
   //    Static mutable variables.  These need to go in a Singleton to maintain thread safety.
   //      As we could -possibly- have multiple different types of senders in one pipeline,
   //      we may want these to be in a map<String,SocketHandler>
   //      where String is a sender name and SocketHandler is a class containing _executer and _socket.
   //    For now we should push forward with the intention of having only one sender per pipeline.
   ////////////////////////////////////////////////////////////////////////////////////////

   static private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool( 1 );
   static private Socket _socket = null;
   static private boolean _stop = false;
   static private String _id = "";


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

   public void initialize( final String queue ) {
      setQueue( queue );
   }

   public void initialize( final String queue, final String host, final String port ) {
      setQueue( queue );
      setHost( host );
      setPort( port );
   }

   /**
    *
    * @param jCas ye olde ...
    * @throws IOException -
    * @throws SAXException -
    */
   public void sendJCas( final JCas jCas ) throws IOException, SAXException {
      sendXmi( jCas.getCas());
   }

   /**
    * Send a stop code to the Artemis queue.
    * @throws IOException -
    */
   public void sendStop() throws IOException {
      LOGGER.info( "Sending Stop code to " + _queue + " ..." );
      sendText( _queue, STOP_MESSAGE );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      // If custom initialization is not necessary then this method override can be removed.
      // If custom initialization is necessary then Always call super.initialize(..) first.
      super.initialize( context );
      LOGGER.info( "Initializing ..." );
      try ( DotLogger dotter = new DotLogger() ) {
         // run long initialization process.  Caught Exception may be of some other type.
         _id = createID();
         startTimeOutLoop( _host, _port, _id, 30 );
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
      // If the initialization is quick then do not use the DotLogger.
   }

   ////////////////////////////////////////////////////////////////////////////////////////
   //    TimeoutLoop     Will disconnect and reconnect the sender socket every 30 seconds.
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Starts up a background thread to constantly disconnect and reconnect the sender socket.
    *
    * @param host -
    * @param port -
    * @param id   -
    * @param wait timeout / reconnection interval in seconds
    * @throws IOException -
    */
   private void startTimeOutLoop( final String host,
                                  final int port,
                                  final String id,
                                  final long wait ) throws IOException {
      final Reconnecter reconnecter = new Reconnecter( host, port, id );
      _executor.scheduleAtFixedRate( reconnecter, 0, wait, TimeUnit.SECONDS );
   }

   private void stopTimeOutLoop() throws IOException {
      _executor.shutdown();
   }

   /**
    * The Artemis implementation using Stomp times out if no information has been sent for some period of time.
    * This will reconnect to the broker.
    */
   private class Reconnecter implements Runnable {
      private final String _host;
      private final int _port;
      private final String _id;
      private Reconnecter( final String host, final int port, final String id ) {
         _host = host;
         _port = port;
         _id = id;
      }
      public void run() {
         try {
            reconnect( _host, _port, _id );
         } catch ( IOException ioE ) {
            ioE.printStackTrace();
         }
      }
   }

   /**
    *
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Sending processed information to " + _queue + " ..." );
      try ( DotLogger dotter = new DotLogger() ) {
         sendJCas( jcas );
      } catch ( IOException | SAXException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void collectionProcessComplete() throws AnalysisEngineProcessException {
      if ( _sendStop.equalsIgnoreCase( "yes" ) ) {
         try {
            sendStop();
            _stop = true;
            stopTimeOutLoop();
            disconnect( _id );
//            System.exit( 0 );
         } catch ( IOException ioE ) {
            throw new AnalysisEngineProcessException( ioE );
         }
      }
   }

   /**
    * Serialize a CAS to a file in XMI format
    *
    * @param cas CAS to serialize
    * @throws IOException  -
    * @throws SAXException -
    */
   private void sendXmi( final CAS cas ) throws IOException, SAXException {
      try ( ByteArrayOutputStream outputStream = new ByteArrayOutputStream() ) {
         XmiCasSerializer casSerializer = new XmiCasSerializer( cas.getTypeSystem() );
         XMISerializer xmiSerializer = new XMISerializer( outputStream );
         casSerializer.serialize( cas, xmiSerializer.getContentHandler() );
//         String XMI = new String( outputStream.toByteArray() );
         String XMI = outputStream.toString();
         sendText( _queue, XMI );
      }
   }



   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //    Sender stuff
   //
   ////////////////////////////////////////////////////////////////////////////////////////

   // TODO  https://activemq.apache.org/components/artemis/documentation/1.1.0/examples.html
   //  According to https://activemq.apache.org/components/artemis/documentation/1.0.0/interoperability.html
   //  (about half way down), it looks like we CAN go from a jms sender to a STOMP receiver:
   //     "The same logic applies when mapping a JMS message or a Core message to Stomp.
   //     A Stomp client can check the presence of the content-length header
   //     to determine the type of the message body (String or bytes)."
   //  That might be a lot more convenient than using all of this socket garbage.
   //  At some point we should try and test.


   /**
    *
    * @return a unique ID for the sender.  Basically allows multiple sockets to the same queue from colliding.
    */
   static private String createID() {
      UUID uuid = UUID.randomUUID();
      return uuid.toString();
   }




   ////////////////////////////////////////////////////////////////////////////////////////
   //    Socket Create, Connect
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * create a sender socket and send the "Connect" request.
    * @param host -
    * @param port -
    * @param id -
    * @throws IOException -
    */
   private void connect( final String host, final int port, final String id ) throws IOException {
      if ( _stop ) {
         return;
      }
      final String connectFrame = "CONNECT\n" +
                                  "accept-version:1.2\n" +
                                  "host:" + host + "\n" +
                                  "request-id:" + id + "\n" +
                                  "\n" +
                                  END_OF_FRAME;
      synchronized ( SOCKET_LOCK ) {
         _socket = createSocket( host, port );
         sendFrame( connectFrame );
         String response = receiveFrame();
      }
   }

   /**
    * create a socket for the sender.
    * @param host -
    * @param port -
    * @return -
    * @throws IOException -
    */
   static private Socket createSocket( final String host, final int port ) throws IOException {
      synchronized ( SOCKET_LOCK ) {
         return new Socket( host, port );
      }
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //    Socket Close, Disconnect
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Send the "Disconnect" request and then close the socket.
    * @param id -
    * @throws IOException -
    */
   static private void disconnect( final String id ) throws IOException {
      String disconnectFrame = "DISCONNECT\n" +
                               "receipt:" + id + "\n" +
                               "\n" +
                               END_OF_FRAME;
      synchronized ( SOCKET_LOCK ) {
         if ( _socket == null || !_socket.isConnected() ) {
            return;
         }
         sendFrame( disconnectFrame );
         final String response = receiveFrame();
         closeSocket();
      }
   }

   /**
    * Close the socket for the sender.
    * @throws IOException -
    */
   static private void closeSocket() throws IOException {
      // Lock may be reentrant, but use it just in case.
      synchronized ( SOCKET_LOCK ) {
         _socket.close();
      }
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //    Socket Reconnect.  Convenience methods.
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Can put a pause between disconnect and reconnect.  Testing never shows that it is necessary.  Not used.
    * @param host -
    * @param port -
    * @param id -
    * @param wait between disconnect and connect, in seconds.
    * @throws IOException -
    * @throws InterruptedException -
    */
   private void reconnect( final String host, final int port,
                           final String id, final long wait ) throws IOException,
                                                                     InterruptedException {
      disconnect( id );
      TimeUnit.SECONDS.sleep( wait );
      connect( host, port, id );
   }

   /**
    * Convenience method to disconnect and immediately connect.
    * @param host -
    * @param port -
    * @param id -
    * @throws IOException -
    */
   private void reconnect( final String host,
                           final int port,
                           final String id ) throws IOException {
      disconnect( id );
      connect( host, port, id );
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //    Send frame to Send Socket.  Convenience methods.
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * creates and sends frame.
    * @param queue -
    * @param text -
    * @throws IOException -
    */
   static private void sendText( final String queue,
                                 final String text ) throws IOException {
      String message = "SEND\n" +
                       "destination:" + queue + "\n" +
                       "destination-type:ANYCAST\n" +
//                       Using text/plain actually changes the received message to a BytesMessage instead of a
//                       TextMessage !  As far as I can tell this content type should be declared.
                       "content-type:text/plain\n" +
                       "content-length:" + text.length() + "\n" +
                       "\n" +
                       text +
                       END_OF_FRAME;
      sendFrame( message );
   }

   /**
    * Sends data to the sender socket.
    * @param data -
    * @throws IOException -
    */
   static private void sendFrame( final String data ) throws IOException {
      byte[] bytes = data.getBytes( StandardCharsets.UTF_8 );
      synchronized ( SOCKET_LOCK ) {
         if ( _socket == null || _socket.isClosed() ) {
            try {
               TimeUnit.SECONDS.sleep( 1 );
            } catch ( InterruptedException intE ) {
               intE.printStackTrace();
            }
         }
         final OutputStream outputStream = _socket.getOutputStream();
         outputStream.write( bytes );
         outputStream.flush();
      }
   }

   /**
    * Just in case we want to use an acknowledgement (or error) in the Sender.
    * @return -
    * @throws IOException -
    */
   static private String receiveFrame() throws IOException {
      synchronized ( SOCKET_LOCK ) {
         InputStream inputStream = _socket.getInputStream();
         byte[] buffer = new byte[ 1024 ];
         int size = inputStream.read( buffer );
         byte[] data = new byte[ size ];
         System.arraycopy( buffer, 0, data, 0, size );
         return new String( data, StandardCharsets.UTF_8 );
      }
   }


   /**
    *
    * @param message BytesMessage to consume.  Using the content-type text/plain produces a message with a byte array.
    * @return text from bytes.
    * @throws JMSException -
    */
   static private String readBytesMessage( final BytesMessage message ) throws JMSException {
      final StringBuilder sb = new StringBuilder();
      byte[] buffer = new byte[1024];
      int size = message.readBytes( buffer );
      while ( size > 0 ) {
         sb.append( new String( buffer, 0, size ) );
         size = message.readBytes( buffer );
      }
      return sb.toString();
   }


}
