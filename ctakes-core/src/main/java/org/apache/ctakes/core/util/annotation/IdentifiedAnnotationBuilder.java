package org.apache.ctakes.core.util.annotation;


import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/26/2020
 */
final public class IdentifiedAnnotationBuilder {

   static private final Logger LOGGER = LoggerFactory.getLogger( "IdentifiedAnnotationBuilder" );
   static private final Pair<Integer> NULL_SPAN = new Pair<>( -1, -1 );

   private Pair<Integer> _textSpan = NULL_SPAN;
   private Function<JCas, ? extends IdentifiedAnnotation> _creator;
   private SemanticGroup _group = SemanticGroup.UNKNOWN;
   private SemanticTui _type = SemanticTui.UNKNOWN;
   private final Collection<OntologyConcept> _concepts = new HashSet<>();
   private boolean _negated = false;
   private boolean _uncertain = false;
   private boolean _generic = false;
   private boolean _conditional = false;
   private boolean _historic = false;
   private String _subject = CONST.ATTR_SUBJECT_PATIENT;
   private int _discoveredBy = 0;
   private float _confidence = Float.MIN_VALUE;


   // TODO Make a similar abstract builder for BinaryTextRelation.  abstract createRelation( JCas )  called by build()

   /**
    * @param begin text span character index
    * @param end   text span character index
    * @return this builder
    */
   public IdentifiedAnnotationBuilder span( final int begin, final int end ) {
      return span( new Pair<>( begin, end ) );
   }

   /**
    * @param textSpan text span character indices
    * @return this builder
    */
   public IdentifiedAnnotationBuilder span( final Pair<Integer> textSpan ) {
      _textSpan = textSpan;
      return this;
   }

   /**
    * Allow for creation of IdentifiedAnnotations outside those created/determined by Semantic Group
    * @param creator some IdentifiedAnnotation creation function, e.g.TimeAnnotation::new
    * @return this builder
    */
   public IdentifiedAnnotationBuilder creator( final Function<JCas, ? extends IdentifiedAnnotation> creator ) {
      _creator = creator;
      return this;
   }

   /**
    * @param semanticGroup for the annotation
    * @return this builder
    */
   public IdentifiedAnnotationBuilder group( final SemanticGroup semanticGroup ) {
      _group = semanticGroup;
      return this;
   }

   /**
    * @param semanticGroup name for the annotation
    * @return this builder
    */
   public IdentifiedAnnotationBuilder group( final String semanticGroup ) {
      return group( SemanticGroup.getGroup( semanticGroup ) );
   }

   /**
    * @param semanticType for the annotation
    * @return this builder
    */
   public IdentifiedAnnotationBuilder type( final SemanticTui semanticType ) {
      _type = semanticType;
      return this;
   }

   /**
    * @param semanticType name for the annotation
    * @return this builder
    */
   public IdentifiedAnnotationBuilder type( final String semanticType ) {
      return type( SemanticTui.getTui( semanticType ) );
   }

   public IdentifiedAnnotationBuilder concept( final OntologyConcept concept ) {
      _concepts.add( concept );
      return this;
   }

   public IdentifiedAnnotationBuilder concept( final JCas jCas, final String cui ) {
      return concept( jCas, cui, _type );
   }

   public IdentifiedAnnotationBuilder concept( final JCas jCas, final String cui, final SemanticTui type ) {
      final OntologyConcept concept = new ConceptBuilder().cui( cui )
                                                          .type( type )
                                                          .build( jCas );
      return concept( concept );
   }

   /**
    * @param tui representing the primary semantic type.
    * @return this builder
    */
   public IdentifiedAnnotationBuilder tui( final int tui ) {
      return type( SemanticTui.getTui( tui ) );
   }

   /**
    * @param tui representing the primary semantic type.
    * @return this builder
    */
   public IdentifiedAnnotationBuilder tui( final String tui ) {
      return type( SemanticTui.getTui( tui ) );
   }

   public IdentifiedAnnotationBuilder negated() {
      _negated = true;
      return this;
   }

   public IdentifiedAnnotationBuilder uncertain() {
      _uncertain = true;
      return this;
   }

   public IdentifiedAnnotationBuilder generic() {
      _generic = true;
      return this;
   }

   public IdentifiedAnnotationBuilder conditional() {
      _conditional = true;
      return this;
   }

   public IdentifiedAnnotationBuilder historic() {
      _historic = true;
      return this;
   }

   public IdentifiedAnnotationBuilder subject( final String subject ) {
      _subject = subject;
      return this;
   }


   public IdentifiedAnnotationBuilder discoveredBy( final int discoveryTechnique ) {
      _discoveredBy = discoveryTechnique;
      return this;
   }

   public IdentifiedAnnotationBuilder confidence( final float confidence ) {
      _confidence = confidence;
      return this;
   }

   private SemanticGroup getGroup() {
      if ( _group != SemanticGroup.UNKNOWN ) {
         return _group;
      }
      if ( _type != SemanticTui.UNKNOWN ) {
         return _type.getGroup();
      }
      if ( !_concepts.isEmpty() ) {
         final Collection<SemanticGroup> groups
               = _concepts.stream()
                          .filter( UmlsConcept.class::isInstance )
                          .map( c -> ( (UmlsConcept) c ).getTui() )
                          .filter( Objects::nonNull )
                          .filter( t -> !t.isEmpty() )
                          .map( SemanticTui::getTui )
                          .map( SemanticTui::getGroup )
                          .collect( Collectors.toSet() );
         if ( !groups.isEmpty() ) {
            if ( groups.size() == 1 ) {
               return new ArrayList<>( groups ).get( 0 );
            }
         }
         return SemanticGroup.getBestGroup( groups );
      }
      return _group;
   }

   public Function<JCas, ? extends IdentifiedAnnotation> getCreator( final SemanticGroup group ) {
      if ( _creator != null ) {
         return _creator;
      }
      return group.getCreator();
   }

   private void addConcepts( final JCas jCas, final IdentifiedAnnotation annotation ) {
      if ( !_concepts.isEmpty() ) {
         final FSArray conceptArr = new FSArray( jCas, _concepts.size() );
         conceptArr.addToIndexes();
         int arrIdx = 0;
         for ( OntologyConcept concept : _concepts ) {
            conceptArr.set( arrIdx, concept );
            arrIdx++;
         }
         annotation.setOntologyConceptArr( conceptArr );
      }
   }


   /**
    * @param cui concept unique identifier
    * @return this builder
    * @deprecated use ConceptBuilder
    */
   @Deprecated
   public IdentifiedAnnotationBuilder cui( final String cui ) {
      throw new UnsupportedOperationException();
   }

   /**
    * @param text preferred
    * @return this builder
    * @deprecated use ConceptBuilder
    */
   @Deprecated
   public IdentifiedAnnotationBuilder preferredText( final String text ) {
      throw new UnsupportedOperationException();
   }

   /**
    * Can be used multiple times
    *
    * @param schema name of an encoding schema.  e.g. SNOMEDCT_US
    * @param code   code for this annotation in that schema
    * @return this builder
    * @deprecated use ConceptBuilder
    */
   @Deprecated
   public IdentifiedAnnotationBuilder addSchemaCode( final String schema, final String code ) {
      throw new UnsupportedOperationException();
   }


   /**
    * @param textSpan  -
    * @param docLength -
    * @return false if the span is unspecified, reversed, or outside the document text.
    */
   static private boolean isSpanValid( final Pair<Integer> textSpan, final int docLength ) {
      if ( textSpan.equals( NULL_SPAN ) ) {
         LOGGER.error( "A Text Span must be specified to build an IdentifiedAnnotation." );
         return false;
      }
      if ( textSpan.getValue1() >= textSpan.getValue2() ) {
         LOGGER.error( "The Text Span ("
                       + textSpan.getValue1() + "," + textSpan.getValue2()
                       + ") is poorly formed.  A valid text span is required to build an IdentifiedAnnotation." );
         return false;
      }
      if ( textSpan.getValue1() < 0 || textSpan.getValue2() >= docLength ) {
         LOGGER.error( "The Text Span ("
                       + textSpan.getValue1() + "," + textSpan.getValue2()
                       + ") must be within the document text bounds (0," + docLength
                       + ") to build an IdentifiedAnnotation." );
         return false;
      }
      return true;
   }


   /**
    * Builds the IdentifiedAnnotation and stores it in the jCas.  The same as .build( jcas )
    *
    * @param jcas ye olde ...
    * @return an IdentifiedAnnotation with properties specified or null if the cui or span are illegal.
    */
   public IdentifiedAnnotation put( final JCas jcas ) {
      return build( jcas );
   }

   /**
    * Builds the IdentifiedAnnotation and stores it in the jCas.  The same as .put( jcas )
    *
    * @param jcas ye olde ...
    * @return an IdentifiedAnnotation with properties specified or null if the cui or span are illegal.
    */
   public IdentifiedAnnotation build( final JCas jcas ) {
      if ( !isSpanValid( _textSpan, jcas.getDocumentText()
                                        .length() ) ) {
         LOGGER.error( "Invalid text span " + _textSpan.getValue1() + "," + _textSpan.getValue2() + " in document of "
                       + "length " + jcas.getDocumentText()
                                         .length() );
         return null;
      }
      final SemanticGroup group = getGroup();
      final IdentifiedAnnotation annotation = getCreator( group )
                                                   .apply( jcas );
      annotation.setTypeID( group.getCode() );
      annotation.setBegin( _textSpan.getValue1() );
      annotation.setEnd( _textSpan.getValue2() );
      if ( _negated ) {
         annotation.setPolarity( CONST.NE_POLARITY_NEGATION_PRESENT );
      }
      if ( _uncertain ) {
         annotation.setUncertainty( CONST.NE_UNCERTAINTY_PRESENT );
      }
      if ( _generic ) {
         annotation.setGeneric( true );
      }
      if ( _conditional ) {
         annotation.setConditional( true );
      }
      if ( _historic ) {
         annotation.setHistoryOf( CONST.NE_HISTORY_OF_PRESENT );
      }
      if ( !_subject.isEmpty() ) {
         annotation.setSubject( _subject );
      }
      annotation.setDiscoveryTechnique( _discoveredBy );
      annotation.setConfidence( _confidence );
      addConcepts( jcas, annotation );
      annotation.addToIndexes();
      return annotation;
   }


}
