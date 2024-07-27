package org.apache.ctakes.core.cc;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.RelationArgumentUtil;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.apache.ctakes.core.pipeline.PipeBitInfo.TypeProduct.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 2/14/2018
 */
@PipeBitInfo(
      name = "CUI List Writer",
      description = "Writes a list of CUIs, covered text and preferred text to files.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { DOCUMENT_ID, SENTENCE, BASE_TOKEN },
      usables = { DOCUMENT_ID_PREFIX, IDENTIFIED_ANNOTATION, EVENT, TIMEX, TEMPORAL_RELATION }
)
public class CuiListFileWriter extends AbstractJCasFileWriter {

   static private final Logger LOGGER = LoggerFactory.getLogger( "CuiListFileWriter" );

   /**
    * {@inheritDoc}
    */
//   @Override
   public void writeFile1( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      final File file = new File( outputDir, documentId + "_cuis.txt" );
      final Collection<IdentifiedAnnotation> annotations = JCasUtil.select( jCas, IdentifiedAnnotation.class );
      LOGGER.info( "Writing CUI list to " + file.getPath() + " ..." );
      final StringBuilder sb = new StringBuilder();
      for ( IdentifiedAnnotation annotation : annotations ) {
         final String coveredText = annotation.getCoveredText();
         OntologyConceptUtil.getUmlsConceptStream( annotation )
                            .map( c -> c.getCui() + " , " + coveredText
                                       + (c.getPreferredText() != null ? " , " + c.getPreferredText() : "")
                                       + "\r\n" )
                            .forEach( sb::append );
      }
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( sb.toString() );
      }
      LOGGER.info( "Finished Writing" );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void writeFile( final JCas jCas,
                          final String outputDir,
                          final String documentId,
                          final String fileName ) throws IOException {
      final File file = new File( outputDir, documentId + "_annotationInfo.bsv" );
      LOGGER.info( "Writing Annotation Information to " + file.getPath() + " ..." );
      final Collection<IdentifiedAnnotation> annotations = JCasUtil.select( jCas, IdentifiedAnnotation.class );
      final Collection<LocationOfTextRelation> locations = JCasUtil.select( jCas, LocationOfTextRelation.class );
      try ( Writer writer = new BufferedWriter( new FileWriter( file ) ) ) {
         writer.write( "CUI | Concept | Semantic Group | Negated | Family History | Location | Document Text\n" );
         for ( IdentifiedAnnotation annotation : annotations ) {
            writer.write( createBsvLines( annotation, locations ) );
         }
      }
      LOGGER.info( "Finished Writing" );
   }

   static private String createBsvLines( final IdentifiedAnnotation annotation,
                                         final Collection<LocationOfTextRelation> locationRelations ) {
      final String locationCuis
            = RelationArgumentUtil.getAllRelated( locationRelations, annotation ).stream()
                                  .map( OntologyConceptUtil::getCuis )
                                  .flatMap( Collection::stream )
                                  .collect( Collectors.joining( ";" ) );
      return OntologyConceptUtil.getUmlsConceptStream( annotation )
                                .map( c -> createBsvLine( annotation, c, locationCuis ) )
                                .collect( Collectors.joining( "\n" ) );
   }

   static private String createBsvLine( final IdentifiedAnnotation annotation,
                                        final UmlsConcept concept,
                                        final String locationCuis ) {
      final StringBuilder sb = new StringBuilder();
      sb.append( concept.getCode() ).append( '|' )
        .append( concept.getPreferredText() ).append( '|' )
        .append( SemanticTui.getTuiFromCode( concept.getTui() ).getGroupName() ).append( '|' )
        .append( annotation.getPolarity() == CONST.NE_POLARITY_NEGATION_PRESENT ? "true" : "false" ).append( '|' )
        .append( annotation.getSubject().equals( CONST.ATTR_SUBJECT_FAMILY_MEMBER ) ? "true" : "false" ).append( '|' )
        .append( locationCuis ).append( '|' )
        .append( annotation.getCoveredText() );
      return sb.toString();
   }

}

