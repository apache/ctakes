package org.apache.ctakes.pbj.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author DJ , chip-nlp
 * @since {2/10/2023}
 */
@PipeBitInfo(
      name = "PbjStompSender",
      description = "Sends jcas to Artemis Queue using Stomp",
      role = PipeBitInfo.Role.SPECIAL
)
public class PbjStompSender extends PbjSender {

   static private final Logger LOGGER = Logger.getLogger( "PbjStompSender" );

   private static final String END_OF_FRAME = "\u0000";
   static private final Object SOCKET_LOCK = new Object();
//   private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool( 1 );
   private Socket _socket = null;



   ////////////////////////////////////////////////////////////////////////////////////////
   //    Socket Create, Connect
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * create a socket for the sender.
    * @param host -
    * @param port -
    * @return -
    * @throws IOException -
    */
   static private Socket createSocket( final String host, final int port ) throws IOException {
      synchronized ( SOCKET_LOCK ) {
         try {
            return new Socket( host, port );
         } catch ( ConnectException cE ) {
            LOGGER.error( "Cannot connect to Artemis.  It is possible that Artemis is not running on " + host + "." );
            LOGGER.error( "Cannot connect to Artemis.  It is possible that port " + port + " is in use." );
            LOGGER.error( "Please check the Artemis log file, in the output directory by default." );
            throw cE;
         }
      }
   }

   /**
    * Close the socket for the sender.
    * @throws IOException -
    */
   private void closeSocket() throws IOException {
      // Lock may be reentrant, but use it just in case.
      synchronized ( SOCKET_LOCK ) {
         _socket.close();
      }
   }



   ////////////////////////////////////////////////////////////////////////////////////////
   //    Socket Close, Disconnect
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * {@inheritDoc}
    */
   @Override
   protected void connect() throws ResourceInitializationException {
      final String connectFrame = "CONNECT\n" +
                                  "accept-version:1.2\n" +
                                  "host:" + _host + "\n" +
                                  "request-id:" + _id + "\n" +
                                  "\n" +
                                  END_OF_FRAME;
      synchronized ( SOCKET_LOCK ) {
         LOGGER.info( "Connecting PBJ Sender on " + _host + " " + _queue + " ..." );
         try {
            _socket = createSocket( _host, _port );
            sendFrame( connectFrame );
            final String response = receiveFrame();
//            LOGGER.info( "Response from Connect: " + response );
         } catch ( IOException ioE ) {
            throw new ResourceInitializationException( ioE );
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void disconnect() throws AnalysisEngineProcessException {
      String disconnectFrame = "DISCONNECT\n" +
                               "receipt:" + _id + "\n" +
                               "\n" +
                               END_OF_FRAME;
      synchronized ( SOCKET_LOCK ) {
         if ( _socket == null || !_socket.isConnected() ) {
            return;
         }
         try {
            sendFrame( disconnectFrame );
            final String response = receiveFrame();
//            LOGGER.info( "Response from Disconnect: " + response );
            closeSocket();
            LOGGER.info( "Disconnected PBJ Sender on " + _host + " " + _queue + " ..." );
         } catch ( IOException ioE ) {
            throw new AnalysisEngineProcessException( ioE );
         }
      }
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //
   //    Sender stuff
   //
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * {@inheritDoc}
    */
   @Override
   protected void  waitOnQueue() {}

   /**
    * {@inheritDoc}
    */
   @Override
   protected void sendText( final String text ) throws AnalysisEngineProcessException {
      String message = "SEND\n" +
                       "destination:" + _queue + "\n" +
                       "destination-type:ANYCAST\n" +
//                       Using text/plain actually changes the received message to a BytesMessage instead of a
//                       TextMessage !  As far as I can tell this content type should be declared.
                       "content-type:text/plain\n" +
                       "content-length:" + text.length() + "\n" +
                       "\n" +
                       text +
                       END_OF_FRAME;
      try {
         sendFrame( message );
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }


   ////////////////////////////////////////////////////////////////////////////////////////
   //    Send frame to Send Socket.  Convenience methods.
   ////////////////////////////////////////////////////////////////////////////////////////

   /**
    * Sends data to the sender socket.
    * @param data -
    * @throws IOException -
    */
   private void sendFrame( final String data ) throws IOException {
      byte[] bytes = data.getBytes( StandardCharsets.UTF_8 );
      synchronized ( SOCKET_LOCK ) {
         if ( _socket == null || _socket.isClosed() ) {
            try {
               connect();
            } catch ( ResourceInitializationException riE ) {
               throw new IOException( riE );
            }
         }
         final OutputStream outputStream = _socket.getOutputStream();
         outputStream.write( bytes );
         outputStream.flush();
         outputStream.close();
      }
   }

   /**
    * Just in case we want to use an acknowledgement (or error) in the Sender.
    * @return -
    * @throws IOException -
    */
   private String receiveFrame() throws IOException {
      synchronized ( SOCKET_LOCK ) {
         final InputStream inputStream = _socket.getInputStream();
         final byte[] buffer = new byte[ 1024 ];
         final int size = inputStream.read( buffer );
         final byte[] data = new byte[ size ];
         System.arraycopy( buffer, 0, data, 0, size );
         inputStream.close();
         return new String( data, StandardCharsets.UTF_8 );
      }
   }




   ////////////////////////////////////////////////////////////////////////////////////////
   //    TimeoutLoop     Will disconnect and reconnect the sender socket every 30 seconds.
   ////////////////////////////////////////////////////////////////////////////////////////

//   /**
//    * Starts up a background thread to constantly disconnect and reconnect the sender socket.
//    *
//    * @param wait timeout / reconnection interval in seconds
//    * @throws IOException -
//    */
//   private void startTimeOutLoop( final long wait ) throws IOException {
//      final Reconnecter reconnecter = new Reconnecter();
//      _executor.scheduleAtFixedRate( reconnecter, 0, wait, TimeUnit.SECONDS );
//   }
//
//   /**
//    * Stop the loop that reconnects to the artemis broker.
//    * @throws IOException -
//    */
//   private void stopTimeOutLoop() throws IOException {
//      _executor.shutdownNow();
//   }
//
//   /**
//    * The Artemis implementation using Stomp times out if no information has been sent for some period of time.
//    * This will reconnect to the broker.
//    */
//   private class Reconnecter implements Runnable {
//      public void run() {
//         synchronized ( SOCKET_LOCK ) {
//            if ( _socket != null && _socket.isConnected() ) {
//               return;
//            }
//         }
//         try {
//            reconnect();
//         } catch ( IOException ioE ) {
//            ioE.printStackTrace();
//         }
//      }
//   }


}
