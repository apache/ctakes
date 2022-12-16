package org.apache.ctakes.fhir.cc;


import org.apache.ctakes.core.util.annotation.EssentialAnnotationUtil;
import org.apache.ctakes.fhir.element.FhirElementFactory;
import org.apache.ctakes.fhir.resource.*;
import org.apache.ctakes.fhir.util.FhirNoteSpecs;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.hl7.fhir.dstu3.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates a complete fhir bundle for a note.
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/15/2017
 */
final public class FhirDocComposer {

   static private final Logger LOGGER = Logger.getLogger( "FhirDocComposer" );

   static private final String SIMPLE_SECTION = "SIMPLE_SEGMENT";

   private FhirDocComposer() {
   }

   /**
    * @param jCas         ye olde ...
    * @param practitioner fhir practitioner.  Usually the default "ctakes" practitioner.
    * @param writeNlpFhir write all nlp information (paragraph, sentence, base annotations) to fhir.
    * @return a complete fhir bundle for a note.
    */
   static public Bundle composeDocFhir( final JCas jCas, final FhirPractitioner practitioner,
                                        final boolean writeNlpFhir ) {
      final FhirNoteSpecs noteSpecs = new FhirNoteSpecs( jCas );
      // creators
      final CompositionCreator compositionCreator = new CompositionCreator();
      final FhirResourceCreator<Segment, Basic> sectionCreator = new SectionCreator();
      final FhirResourceCreator<IdentifiedAnnotation, Basic> iaCreator = new IdentifiedAnnotationCreator();
      final FhirResourceCreator<Annotation, Basic> aCreator = new AnnotationCreator();
      // essential types
      final Map<Segment, Collection<IdentifiedAnnotation>> sectionAnnotationMap
            = JCasUtil.indexCovered( jCas, Segment.class, IdentifiedAnnotation.class );
      final Collection<Basic> sections = new ArrayList<>( sectionAnnotationMap.size() );
      final Map<IdentifiedAnnotation, Collection<Integer>> markableCorefs = EssentialAnnotationUtil.createMarkableCorefs(
            jCas );
      final Collection<IdentifiedAnnotation> requiredAnnotations = EssentialAnnotationUtil.getRequiredAnnotations( jCas,
            markableCorefs );
      // Create map of annotations to Fhir Basics.
      final Map<IdentifiedAnnotation, Basic> annotationBasics = new HashMap<>();
      for ( Map.Entry<Segment, Collection<IdentifiedAnnotation>> sectionAnnotations : sectionAnnotationMap.entrySet() ) {
         final Segment segment = sectionAnnotations.getKey();
         final String segmentId = segment.getId();
         final Basic section = sectionCreator.createResource( jCas, sectionAnnotations.getKey(), practitioner,
               noteSpecs );
         if ( writeNlpFhir || (!segmentId.isEmpty() && !segmentId.equals( SIMPLE_SECTION )) ) {
            sections.add( section );
         }
         final Reference sectionRef = new Reference( section );
         for ( IdentifiedAnnotation annotation : sectionAnnotations.getValue() ) {
            if ( !requiredAnnotations.contains( annotation ) ) {
               continue;
            }
            final Basic basic = iaCreator.createResource( jCas, annotation, practitioner, noteSpecs );
            if ( !segmentId.isEmpty() && !segmentId.equals( SIMPLE_SECTION ) ) {
               basic.addExtension( FhirElementFactory.createSectionExtension( sectionRef ) );
            }
            final Collection<Integer> corefs = markableCorefs.get( annotation );
            if ( corefs != null ) {
               corefs.stream()
                     .map( FhirElementFactory::createCorefIndex )
                     .forEach( basic::addExtension );
            }
            annotationBasics.put( annotation, basic );
         }
      }
      // Add relations as reference extensions.
      final Map<Annotation, Basic> simpleAnnotationBasics = new HashMap<>();

      if ( writeNlpFhir ) {
         final BaseTokenCreator baseTokenCreator = new BaseTokenCreator();
         JCasUtil.select( jCas, BaseToken.class )
                 .forEach( b -> simpleAnnotationBasics.put( b,
                       baseTokenCreator.createResource( jCas, b, practitioner, noteSpecs ) ) );
      }

      addRelations( jCas, practitioner, noteSpecs, aCreator, annotationBasics, simpleAnnotationBasics );
      // Create a Bundle
      final Composition composition = compositionCreator.createResource( jCas, null, practitioner, noteSpecs );
      final Bundle bundle = new BundleCreator().createResource( jCas, null, practitioner, noteSpecs );
      bundle.addEntry( new Bundle.BundleEntryComponent().setResource( composition ) );
      bundle.addEntry( new Bundle.BundleEntryComponent().setResource( practitioner.getPractitioner() ) );
      addBundleResources( bundle, noteSpecs.getSubjects() );
      addBundleResources( bundle, sections );
      addBundleResources( bundle, annotationBasics.values() );
      addBundleResources( bundle, simpleAnnotationBasics.values() );

      if ( writeNlpFhir ) {
         final ParagraphCreator paragraphCreator = new ParagraphCreator();
         final Collection<Basic> paragraphs
               = JCasUtil.select( jCas, Paragraph.class ).stream()
                         .map( p -> paragraphCreator.createResource( jCas, p, practitioner, noteSpecs ) )
                         .collect( Collectors.toList() );
         addBundleResources( bundle, paragraphs );

         final SentenceCreator sentenceCreator = new SentenceCreator();
         final Collection<Basic> sentences
               = JCasUtil.select( jCas, Sentence.class ).stream()
                         .map( s -> sentenceCreator.createResource( jCas, s, practitioner, noteSpecs ) )
                         .collect( Collectors.toList() );
         addBundleResources( bundle, sentences );
      }

      return bundle;
   }

   /**
    * Add all of the given resources to the given bundle.
    *
    * @param bundle    -
    * @param resources -
    */
   static private void addBundleResources( final Bundle bundle, final Collection<? extends Resource> resources ) {
      resources.stream()
            .map( r -> new Bundle.BundleEntryComponent().setResource( r ) )
            .forEach( bundle::addEntry );
   }

   /**
    * Link fhir object ids according to ctakes relations.
    * @param jCas ye olde ...
    * @param practitioner -
    * @param noteSpecs -
    * @param aCreator -
    * @param annotationBasics -
    * @param simpleBasics -
    */
   static private void addRelations( final JCas jCas,
                                     final FhirPractitioner practitioner,
                                     final FhirNoteSpecs noteSpecs,
                                     final FhirResourceCreator<Annotation, Basic> aCreator,
                                     final Map<IdentifiedAnnotation, Basic> annotationBasics,
                                     final Map<Annotation, Basic> simpleBasics ) {
      final Collection<BinaryTextRelation> relations = JCasUtil.select( jCas, BinaryTextRelation.class );
      for ( BinaryTextRelation relation : relations ) {
         final RelationArgument arg1 = relation.getArg1();
         final Annotation source = arg1.getArgument();
         Basic basicSource;
         if ( source instanceof IdentifiedAnnotation ) {
            basicSource = annotationBasics.get( (IdentifiedAnnotation)source );
         } else {
            basicSource = getSimpleBasic( jCas, source, practitioner, noteSpecs, aCreator, simpleBasics );
         }
         final RelationArgument arg2 = relation.getArg2();
         final Annotation target = arg2.getArgument();
         Basic basicTarget;
         if ( target instanceof IdentifiedAnnotation ) {
            basicTarget = annotationBasics.get( (IdentifiedAnnotation)target );
         } else {
            basicTarget = getSimpleBasic( jCas, target, practitioner, noteSpecs, aCreator, simpleBasics );
         }
         final String type = relation.getCategory();
         basicSource.addExtension( FhirElementFactory.createRelation( type, basicTarget ) );
      }
   }

   /**
    *
    * @param jCas ye olde ...
    * @param annotation -
    * @param practitioner -
    * @param noteSpecs -
    * @param aCreator -
    * @param simpleBasics -
    * @return fhir Basic object for the given annotation.
    */
   static private Basic getSimpleBasic( final JCas jCas,
                                        final Annotation annotation,
                                        final FhirPractitioner practitioner,
                                        final FhirNoteSpecs noteSpecs,
                                        final FhirResourceCreator<Annotation, Basic> aCreator,
                                        final Map<Annotation, Basic> simpleBasics ) {
      final Basic basic = simpleBasics.get( annotation );
      if ( basic != null ) {
         return basic;
      }
      final Basic newBasic = aCreator.createResource( jCas, annotation, practitioner, noteSpecs );
      simpleBasics.put( annotation, newBasic );
      return newBasic;
   }


}
