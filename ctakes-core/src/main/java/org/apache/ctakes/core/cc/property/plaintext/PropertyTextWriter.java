package org.apache.ctakes.core.cc.property.plaintext;


import org.apache.ctakes.core.cc.pretty.SemanticGroup;
import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.typesystem.type.refsem.*;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Writes Document event and anatomic information to file.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 10/15/2015
 */
public class PropertyTextWriter {

   static private final Logger LOGGER = Logger.getLogger( "PropertyTextWriter" );
   static private final String FILE_EXTENSION = ".properties.txt";

   private String _outputDirPath;

   // TODO Abstract common methods for PropertyTextWriter and PrettyTextWriter

   /**
    * @param outputDirectoryPath may be empty or null, in which case the current working directory is used
    * @throws IllegalArgumentException if the provided path points to a File and not a Directory
    * @throws SecurityException        if the File System has issues
    */
   public void setOutputDirectory( final String outputDirectoryPath ) throws IllegalArgumentException,
                                                                             SecurityException {
      // If no outputDir is specified (null or empty) the current working directory will be used.  Else check path.
      if ( outputDirectoryPath == null || outputDirectoryPath.isEmpty() ) {
         _outputDirPath = "";
         LOGGER.debug( "No Output Directory Path specified, using current working directory "
                       + System.getProperty( "user.dir" ) );
         return;
      }
      final File outputDir = new File( outputDirectoryPath );
      if ( !outputDir.exists() ) {
         outputDir.mkdirs();
      }
      if ( !outputDir.isDirectory() ) {
         throw new IllegalArgumentException( outputDirectoryPath + " is not a valid directory path" );
      }
      _outputDirPath = outputDirectoryPath;
      LOGGER.debug( "Output Directory Path set to " + _outputDirPath );
   }

   /**
    * Process the jcas and write sentence property lists to file.
    * Filename is based upon the document id stored in the cas
    *
    * @param jcas ye olde ...
    */
   public void process( final JCas jcas ) {
      LOGGER.info( "Starting processing" );
      final String docId = DocIdUtil.getDocumentIdForFile( jcas );
      File outputFile;
      if ( _outputDirPath == null || _outputDirPath.isEmpty() ) {
         outputFile = new File( docId + FILE_EXTENSION );
      } else {
         outputFile = new File( _outputDirPath, docId + FILE_EXTENSION );
      }
      try ( final BufferedWriter writer = new BufferedWriter( new FileWriter( outputFile ) ) ) {
         final Collection<Sentence> sentences = JCasUtil.select( jcas, Sentence.class );
         for ( Sentence sentence : sentences ) {
            writeSentence( jcas, sentence, writer );
         }
      } catch ( IOException ioE ) {
         LOGGER.error( "Could not not write pretty property file " + outputFile.getPath() );
         LOGGER.error( ioE.getMessage() );
      }
      LOGGER.info( "Finished processing" );
   }

   /**
    * Write a sentence and list of event and anatomical site properties from the document text
    *
    * @param jcas     ye olde ...
    * @param sentence annotation containing the sentence
    * @param writer   writer to which property lists for the sentence should be written
    * @throws IOException if the writer has issues
    */
   static public void writeSentence( final JCas jcas,
                                     final AnnotationFS sentence,
                                     final BufferedWriter writer ) throws IOException {
      final String sentenceText = sentence.getCoveredText().trim();
      if ( sentenceText.isEmpty() ) {
         return;
      }
      final Collection<IdentifiedAnnotation> identifiedAnnotations
            = JCasUtil.selectCovered( jcas, IdentifiedAnnotation.class, sentence );
      writer.write( sentenceText );
      writer.newLine();
      for ( IdentifiedAnnotation annotation : identifiedAnnotations ) {
         final Map<String, Collection<UmlsConcept>> semanticConcepts = getSemanticConcepts( annotation );
         if ( semanticConcepts.isEmpty() ) {
            continue;
         }
         // write line with actual text, polarity,
         writer.write( "\"" + annotation.getCoveredText() + "\"" + getAnnotationProperties( annotation ) );
         if ( annotation instanceof EventMention ) {
            writer.write( getEventProperties( (EventMention)annotation ) );
         } else if ( annotation instanceof AnatomicalSiteMention ) {
            writer.write( getAnatomicalProperties( (AnatomicalSiteMention)annotation ) );
         }
         writer.newLine();
         // Write listing of UmlsConcept info, grouped by ctakes semantic group
         for ( Map.Entry<String, Collection<UmlsConcept>> umlsConcepts : semanticConcepts.entrySet() ) {
            writer.write( " \t" + umlsConcepts.getKey() );
            writer.newLine();
            for ( UmlsConcept umlsConcept : umlsConcepts.getValue() ) {
               final String preferredText = umlsConcept.getPreferredText();
               writer.write( " \t \t" + umlsConcept.getCui() + (preferredText != null ? " " + preferredText : "") );
               writer.newLine();
            }
         }
         final Collection<String> relations = getRelations( jcas, annotation );
         for ( String relation : relations ) {
            writer.write( " \t" + relation );
            writer.newLine();
         }
      }
      writer.newLine();
   }


   /**
    * @param identifiedAnnotation an annotation of interest
    * @return map of semantic type names and umls concepts within those types as they apply to the annotation
    */
   static private Map<String, Collection<UmlsConcept>> getSemanticConcepts(
         final IdentifiedAnnotation identifiedAnnotation ) {
      final Collection<UmlsConcept> umlsConcepts = OntologyConceptUtil.getUmlsConcepts( identifiedAnnotation );
      if ( umlsConcepts == null || umlsConcepts.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<String, Collection<UmlsConcept>> semanticConcepts = new HashMap<>();
      final Collection<String> usedCuis = new HashSet<>();
      for ( UmlsConcept umlsConcept : umlsConcepts ) {
         if ( usedCuis.contains( umlsConcept.getCui() ) ) {
            continue;
         }
         usedCuis.add( umlsConcept.getCui() );
         final String semanticName = SemanticGroup.getSemanticName( identifiedAnnotation, umlsConcept );
         semanticConcepts.putIfAbsent( semanticName, new HashSet<>() );
         semanticConcepts.get( semanticName ).add( umlsConcept );
      }
      return semanticConcepts;
   }

   /**
    * @param annotation -
    * @return a line of text with doctimerel, modality, aspect and permanence ; if available
    */
   static private String getAnnotationProperties( final IdentifiedAnnotation annotation ) {
      final StringBuilder sb = new StringBuilder();
      if ( annotation.getPolarity() < 0 ) {
         sb.append( " negated" );
      }
      if ( annotation.getUncertainty() == 1 ) {
         sb.append( " uncertain" );
      }
      if ( annotation.getGeneric() ) {
         sb.append( " generic" );
      }
      if ( annotation.getConditional() ) {
         sb.append( " conditional" );
      }
      if ( annotation.getHistoryOf() == 1 ) {
         sb.append( " in history" );
      }
      if ( annotation.getSubject() != null && !annotation.getSubject().isEmpty() ) {
         sb.append( " for " ).append( annotation.getSubject() );
      }
      return sb.toString();
   }

   /**
    * @param eventMention -
    * @return a line of text with doctimerel, modality, aspect and permanence ; if available
    */
   static private String getEventProperties( final EventMention eventMention ) {
      final Event event = eventMention.getEvent();
      if ( event == null ) {
         return "";
      }
      final EventProperties eventProperties = event.getProperties();
      if ( eventProperties == null ) {
         return "";
      }
      final StringBuilder sb = new StringBuilder();
      sb.append( " occurred " );
      sb.append( eventProperties.getDocTimeRel().toLowerCase() );
      sb.append( " document time" );
      // modality is: Actual, hypothetical, hedged, generic
      final String modality = eventProperties.getContextualModality();
      if ( modality != null && !modality.isEmpty() ) {
         sb.append( ", " );
         sb.append( modality.toLowerCase() );
      }
      // Aspect is: Intermittent (or not)
      final String aspect = eventProperties.getContextualAspect();
      if ( aspect != null && !aspect.isEmpty() ) {
         sb.append( ", " );
         sb.append( aspect.toLowerCase() );
      }
      // Permanence is: Finite or permanent
      final String permanence = eventProperties.getPermanence();
      if ( permanence != null && !permanence.isEmpty() ) {
         sb.append( ", " );
         sb.append( permanence.toLowerCase() );
      }
      return sb.toString();
   }

   /**
    * @param anatomicalSite -
    * @return a line of text with body laterality and side ; if available
    */
   static private String getAnatomicalProperties( final AnatomicalSiteMention anatomicalSite ) {
      StringBuilder sb = new StringBuilder();
      final BodyLateralityModifier laterality = anatomicalSite.getBodyLaterality();
      if ( laterality != null ) {
         final Attribute normalized = laterality.getNormalizedForm();
         if ( normalized != null && normalized instanceof BodyLaterality ) {
            sb.append( ", " );
            sb.append( ((BodyLaterality)normalized).getValue() );
         }
      }
      final BodySideModifier bodySide = anatomicalSite.getBodySide();
      if ( bodySide != null ) {
         final Attribute normalized = bodySide.getNormalizedForm();
         if ( normalized != null && normalized instanceof BodySide ) {
            sb.append( ", " );
            sb.append( ((BodySide)normalized).getValue() );
         }
      }
      return sb.toString();
   }

   /**
    * @param jcas       ye olde ...
    * @param annotation of interest
    * @return all relations with the given annotation as the first or second argument
    */
   static private Collection<String> getRelations( final JCas jcas, final IdentifiedAnnotation annotation ) {
      final Collection<BinaryTextRelation> relations = JCasUtil.select( jcas, BinaryTextRelation.class );
      if ( relations == null || relations.isEmpty() ) {
         return Collections.emptyList();
      }
      final Collection<String> relationTexts = new ArrayList<>();
      for ( BinaryTextRelation relation : relations ) {
         final Annotation argument1 = relation.getArg1().getArgument();
         final Annotation argument2 = relation.getArg2().getArgument();
         if ( annotation.equals( argument1 ) || annotation.equals( argument2 ) ) {
            relationTexts.add( argument1.getCoveredText()
                               + " " + relation.getCategory().toLowerCase()
                               + " " + argument2.getCoveredText() );
         }
      }
      return relationTexts;
   }


}
