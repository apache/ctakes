package org.apache.ctakes.temporal.cc;

import org.apache.ctakes.core.cc.AbstractJCasFileWriter;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.BASE_TOKEN;
import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.DOCUMENT_ID_PREFIX;

/**
 * @author SPF , chip-nlp
 * @since {3/2/2023}
 */
@PipeBitInfo(
      name = "Event Time Anafora Writer",
      description = "Writes Temporal Events and Times in Anafora format.",
      role = PipeBitInfo.Role.WRITER,
      usables = { DOCUMENT_ID_PREFIX, BASE_TOKEN }
)
final public class EventTimeAnaforaWriter extends AbstractJCasFileWriter {

   /**
    * Sometimes you want a file extension that specifies specifics about the corpus, creator and phase.
    *  e.g.    ".UmlsDeepPhe.dave.completed.xml"
    */
   static public final String PARAM_FILE_EXTENSION = "FileExtension";
   @ConfigurationParameter(
         name = PARAM_FILE_EXTENSION,
         description = "The extension for the written files. Default is .EventTime.ctakes.completed.xml",
         defaultValue = ".EventTime.ctakes.completed.xml",
         mandatory = false
   )
   private String _fileExtension;

   /**
    * Sometimes you want a file extension that specifies specifics about the corpus, creator and phase.
    *  e.g.    ".UmlsDeepPhe.dave.completed.xml"
    */
   static public final String PARAM_ONLY_TIME_EVENTS = "OnlyTemporalEvents";
   @ConfigurationParameter(
         name = PARAM_ONLY_TIME_EVENTS,
         description = "Only use temporal events, not those created by dictionary lookup. Default is yes.",
         defaultValue = "yes",
         mandatory = false
   )
   private String _onlyTemporalEvents;


   static private final String SAVE_TIME_PATTERN = "yyyy-MMdd-HH:mm";
   static private final SimpleDateFormat SAVE_TIME_FORMAT = new SimpleDateFormat( SAVE_TIME_PATTERN);


   private boolean onlyTemporalEvents() {
      return _onlyTemporalEvents.equalsIgnoreCase( "yes" )
             || _onlyTemporalEvents.equalsIgnoreCase( "true" );
   }

   /**
    * Writes some document metadata and discovered event information.
    */
   @Override
   public void writeFile( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      final File file = new File( outputDir, fileName + _fileExtension );
      try {
         final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
         final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
         final Document doc = docBuilder.newDocument();

         final Element rootElement = doc.createElement( "data" );
         rootElement.appendChild( createInfoElement( doc  ) );
         rootElement.appendChild( createSchemaElement( doc ) );
         rootElement.appendChild( createAnnotationsElement( jCas, documentId, doc ) );
         doc.appendChild( rootElement );

         // boilerplate xml-writing code:
         final TransformerFactory transformerFactory = TransformerFactory.newInstance();
         final Transformer transformer = transformerFactory.newTransformer();
         transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
         transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
         final DOMSource source = new DOMSource( doc );
         final StreamResult result = new StreamResult( file );
         transformer.transform( source, result );
      } catch ( ParserConfigurationException | TransformerException multE ) {
         throw new IOException( multE );
      }
   }


   static private Element createInfoElement( final Document doc ) {
      final Element info = doc.createElement( "info" );
      final Element saveTime = doc.createElement( "savetime" );
      final String saveTimeText = SAVE_TIME_FORMAT.format( new Date() );
      saveTime.setTextContent( saveTimeText );
      final Element progress = doc.createElement( "progress" );
      progress.setTextContent( "completed" );
      info.appendChild( saveTime );
      info.appendChild( progress );
      return info;
   }

   static private Element createSchemaElement( final Document doc ) {
      final Element schema = doc.createElement( "schema" );
      schema.setAttribute( "path", "./" );
      schema.setAttribute( "protocol", "file" );
      schema.setTextContent( "temporal-schema.xml" );
      return schema;
   }

   private Element createAnnotationsElement( final JCas jCas,
                                             final String documentId,
                                             final Document doc ) {
      final Element annotations = doc.createElement( "annotations" );
      int nextIdNumber = addEventElements( jCas, documentId, 1, annotations, doc );
      nextIdNumber = addTimeElements( jCas, documentId, nextIdNumber, annotations, doc );
      return annotations;
   }

   private int addEventElements( final JCas jCas,
                                  final String documentId,
                                   final int startId,
                                   final Element annotations,
                                   final Document doc ) {
      final List<EventMention> eventMentions = new ArrayList<>( JCasUtil.select( jCas, EventMention.class ) );
      eventMentions.sort( Comparator.comparingInt( EventMention::getBegin )
                                    .thenComparingInt( EventMention::getEnd ) );
      final boolean onlyTemporalEvents = onlyTemporalEvents();
      int idNumber = startId;
      for ( EventMention eventMention : eventMentions ) {
         // this ensures we are only looking at THYME events and not ctakes-dictionary-lookup events
         if ( onlyTemporalEvents && !eventMention.getClass().equals( EventMention.class ) ) {
            continue;
         }
         annotations.appendChild( createEventElement( eventMention, documentId, idNumber, doc ) );
         idNumber++;
      }
      return idNumber + 1;
   }

   static private Element createEventElement( final EventMention eventMention,
                                              final String documentId,
                                              final int idNumber,
                                              final Document doc ) {

      final Element event = createBaseElement( eventMention, "EVENT", documentId, idNumber, doc );
      event.appendChild( createEventPropertiesElement( eventMention, doc ) );
      return event;
   }

   static private Element createEventPropertiesElement( final EventMention eventMention,
                                                        final Document doc ) {
      final Event event = eventMention.getEvent();
      if ( event == null ) {
         return createNullEventProperties( IdentifiedAnnotationUtil.isNegated( eventMention ) , doc );
      }
      final Element properties = doc.createElement( "properties" );
      final EventProperties eventProperties = event.getProperties();
      Element docTimeRel = doc.createElement( "DocTimeRel" );
      final String dtrContent = eventProperties.getDocTimeRel();
      docTimeRel.setTextContent( dtrContent );
      final Element eventType = doc.createElement( "Type" );
      eventType.setTextContent( "N/A" );
      final Element degree = doc.createElement( "Degree" );
      degree.setTextContent( "N/A" );
      final Element polarity = doc.createElement( "Polarity" );
      final String polarityValue = IdentifiedAnnotationUtil.isNegated( eventMention ) ? "NEG" : "POS";
      polarity.setTextContent( polarityValue );
      final Element contextMode = doc.createElement( "ContextualModality" );
      contextMode.setTextContent( eventProperties.getContextualModality() );
      final Element contextAspect = doc.createElement( "ContextualAspect" );
      contextAspect.setTextContent( eventProperties.getContextualAspect() );
      final Element Permanence = doc.createElement( "Permanence" );
      Permanence.setTextContent( "UNDETERMINED" );
      properties.appendChild( docTimeRel );
      properties.appendChild( polarity );
      properties.appendChild( degree );
      properties.appendChild( eventType );
      properties.appendChild( contextMode );
      properties.appendChild( contextAspect );
      properties.appendChild( Permanence );
      return properties;
   }

   static private Element createNullEventProperties( final boolean isNegated, final Document doc ) {
      final Element properties = doc.createElement( "properties" );
      Element docTimeRel = doc.createElement( "DocTimeRel" );
      docTimeRel.setTextContent( "Overlap" );
      final Element eventType = doc.createElement( "Type" );
      eventType.setTextContent( "N/A" );
      final Element degree = doc.createElement( "Degree" );
      degree.setTextContent( "N/A" );
      final Element polarity = doc.createElement( "Polarity" );
      final String polarityValue = isNegated ? "NEG" : "POS";
      polarity.setTextContent( polarityValue );
      final Element contextMode = doc.createElement( "ContextualModality" );
      contextMode.setTextContent( "UNDETERMINED" );
      final Element contextAspect = doc.createElement( "ContextualAspect" );
      contextAspect.setTextContent( "UNDETERMINED" );
      final Element Permanence = doc.createElement( "Permanence" );
      Permanence.setTextContent( "UNDETERMINED" );
      properties.appendChild( docTimeRel );
      properties.appendChild( polarity );
      properties.appendChild( degree );
      properties.appendChild( eventType );
      properties.appendChild( contextMode );
      properties.appendChild( contextAspect );
      properties.appendChild( Permanence );
      return properties;
   }


   private int addTimeElements( final JCas jCas,
                                 final String documentId,
                                 final int startId,
                                 final Element annotations,
                                 final Document doc ) {
      final List<TimeMention> timeMentions = new ArrayList<>( JCasUtil.select( jCas, TimeMention.class ) );
      timeMentions.sort( Comparator.comparingInt( TimeMention::getBegin )
                                    .thenComparingInt( TimeMention::getEnd ) );
      int idNumber = startId;
      for ( TimeMention timeMention : timeMentions ) {
         annotations.appendChild( createTimeElement( timeMention, documentId, idNumber, doc ) );
         idNumber++;
      }
      return idNumber + 1;
   }

   static private Element createTimeElement( final TimeMention timeMention,
                                             final String documentId,
                                             final int idNumber,
                                             final Document doc ) {
      final Element properties = doc.createElement( "properties" );
      String typeName = "";
      final String timeClass = timeMention.getTimeClass();
      if ( timeClass != null && (timeClass.equals( "DOCTIME" ) || timeClass.equals( "SECTIONTIME" ) ) ) {
         typeName = timeClass;
         properties.setTextContent( "" );
      } else {
         typeName = "TIMEX3";
         final Element classE = doc.createElement( "Class" );
         classE.setTextContent( timeClass );
         properties.appendChild( classE );
      }
      final Element time = createBaseElement( timeMention, typeName, documentId, idNumber, doc );
      time.appendChild( properties );
      return time;
   }


   static private Element createBaseElement( final IdentifiedAnnotation annotation,
                                              final String typeName,
                                              final String documentId,
                                              final int idNumber,
                                              final Document doc ) {

      final Element base = doc.createElement( "entity" );
      final String eventID = idNumber + "@e@" + documentId + "@system";
      final Element id = doc.createElement( "id" );
      id.setTextContent( eventID );
      final Element span = doc.createElement( "span" );
      span.setTextContent( annotation.getBegin() + "," + annotation.getEnd() );
      final Element type = doc.createElement( "type" );
      type.setTextContent( typeName );
      final Element parentsType = doc.createElement( "parentsType" );
      parentsType.setTextContent( "TemporalEntities" );
      base.appendChild( id );
      base.appendChild( span );
      base.appendChild( type );
      base.appendChild( parentsType );
      return base;
   }


}
