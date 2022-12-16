package org.apache.ctakes.core.util.annotation;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.uima.jcas.JCas;


/**
 * @author SPF , chip-nlp
 * @since {1/4/2022}
 */
final public class ConceptBuilder {

   private String _schema = "";
   private String _code = "";
   private double _score = Double.MIN_VALUE;
   private boolean _disambiguated = false;


   // A value in any of these will create a UmlsConcept
   private SemanticTui _type = SemanticTui.UNKNOWN;
   private String _cui = "C0000000";
   private String _prefText = "Unknown UMLS Concept";
   private boolean _isUmls = false;


   /**
    * @param schema name of an encoding schema.  e.g. SNOMEDCT_US
    * @return this builder
    */
   public ConceptBuilder schema( final String schema ) {
      _schema = schema;
      return this;
   }

   /**
    * @param code code for this annotation in that schema
    * @return this builder
    */
   public ConceptBuilder code( final String code ) {
      _code = code;
      return this;
   }

   /**
    * @param score score for something like confidence
    * @return this builder
    */
   public ConceptBuilder score( final double score ) {
      _score = score;
      return this;
   }

   /**
    * set disambiguated = true
    *
    * @return this builder
    */
   public ConceptBuilder disambiguated() {
      return disambiguated( true );
   }

   /**
    * @param disambiguated true if this concept has been disambiguated
    * @return this builder
    */
   public ConceptBuilder disambiguated( final boolean disambiguated ) {
      _disambiguated = disambiguated;
      return this;
   }


   // If any of these is set then a UmlsConcept is created

   /**
    * @param semanticType for the annotation
    * @return this builder
    */
   public ConceptBuilder type( final SemanticTui semanticType ) {
      _type = semanticType;
      _isUmls = true;
      return this;
   }

   /**
    * @param semanticType name for the annotation
    * @return this builder
    */
   public ConceptBuilder type( final String semanticType ) {
      return type( SemanticTui.getTui( semanticType ) );
   }

   /**
    * @param tui representing the primary semantic type.
    * @return this builder
    */
   public ConceptBuilder tui( final int tui ) {
      return type( SemanticTui.getTui( tui ) );
   }

   /**
    * @param tui representing the primary semantic type.
    * @return this builder
    */
   public ConceptBuilder tui( final String tui ) {
      return type( SemanticTui.getTui( tui ) );
   }

   /**
    * @param cui concept unique identifier
    * @return this builder
    */
   public ConceptBuilder cui( final String cui ) {
      _cui = cui;
      _isUmls = true;
      return this;
   }

   /**
    * @param text preferred
    * @return this builder
    */
   public ConceptBuilder preferredText( final String text ) {
      _prefText = text;
      _isUmls = true;
      return this;
   }

   private boolean isUmls() {
      return _isUmls;
   }

   /**
    * @param jcas ye olde ...
    * @return a UmlsConcept
    */
   public OntologyConcept build( final JCas jcas ) {
      OntologyConcept concept;
      if ( isUmls() ) {
         concept = new UmlsConcept( jcas );
         ( (UmlsConcept) concept ).setCui( _cui );
         if ( _type != SemanticTui.UNKNOWN ) {
            ( (UmlsConcept) concept ).setTui( _type.name() );
         }
         if ( !_prefText.isEmpty() ) {
            ( (UmlsConcept) concept ).setPreferredText( _prefText );
         }
      } else {
         concept = new OntologyConcept( jcas );
      }
      if ( !_schema.isEmpty() ) {
         concept.setCodingScheme( _schema );
         concept.setCode( _code );
         if ( _score != Double.MIN_VALUE ) {
            concept.setScore( _score );
         }
         if ( _disambiguated ) {
            concept.setDisambiguated( true );
         }
      }
      return concept;
   }


}
