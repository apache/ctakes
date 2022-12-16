package org.apache.ctakes.core.pipeline;


import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.relation.*;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.structured.DocumentIdPrefix;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.syntax.TerminalTreebankNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.Markable;
import org.apache.ctakes.typesystem.type.textsem.SemanticRoleRelation;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.jcas.cas.TOP;

import java.lang.annotation.*;

/**
 * Annotation that should be used for Collection Readers, Annotators, and Cas Consumers (Writers).
 * It may be useful for pipeline builder UIs and other human-pipeline interaction.
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/22/2016
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Inherited
public @interface PipeBitInfo {
   enum Role {
      READER, ANNOTATOR, WRITER, SPECIAL
   }

   String NO_PARAMETERS = "No Parameters.";
   TypeProduct[] NO_TYPE_PRODUCTS = new TypeProduct[ 0 ];


   /**
    * Using an enum allows us to use a TypeSystem equivalent in the annotation
    */
   enum TypeProduct {
      TOP( TOP.class ),
      DOCUMENT_ID( DocumentID.class ),
      DOCUMENT_ID_PREFIX( DocumentIdPrefix.class ),
      SECTION( Segment.class ),
      SENTENCE( Sentence.class ),
      PARAGRAPH( Paragraph.class ),
      LIST( org.apache.ctakes.typesystem.type.textspan.List.class ),
      BASE_TOKEN( BaseToken.class ),
      CHUNK( Chunk.class ),
      IDENTIFIED_ANNOTATION( IdentifiedAnnotation.class ),
      EVENT( Event.class ),
      TIMEX( TimeMention.class ),
      GENERIC_RELATION( BinaryTextRelation.class ),
      SEMANTIC_RELATION( SemanticRoleRelation.class ),
      LOCATION_RELATION( LocationOfTextRelation.class ),
      DEGREE_RELATION( DegreeOfTextRelation.class ),
      TEMPORAL_RELATION( TemporalTextRelation.class ),
      DEPENDENCY_NODE( ConllDependencyNode.class ),
      TREE_NODE( TerminalTreebankNode.class ),
      MARKABLE( Markable.class ),
      COREFERENCE_RELATION( CoreferenceRelation.class );

      final Class<? extends TOP> _classType;

      TypeProduct( final Class<? extends TOP> classType ) {
         _classType = classType;
      }

      @Override
      public String toString() {
         return _classType.getSimpleName();
      }

      static public TypeProduct getForClass( Class<? extends TOP> classType ) {
         for ( TypeProduct typeProduct : TypeProduct.values() ) {
            if ( typeProduct._classType.equals( classType ) ) {
               return typeProduct;
            }
         }
         return TOP;
      }
   }

   /**
    * @return Human-readable name of the Reader, Annotator, or Writer
    */
   String name();

   /**
    * @return Role played within a pipeline
    */
   Role role() default Role.ANNOTATOR;

   /**
    * @return Human-readable description of the purpose of the Reader, Annotator, or Writer
    */
   String description();

   /**
    * @return Human-readable names of Configuration Parameters
    */
   String[] parameters() default { NO_PARAMETERS };

   /**
    * @return array of typesystem type dependencies of the Reader, Annotator, or Writer
    */
   TypeProduct[] dependencies() default {};

   /**
    * @return array of usable but not required typesystem types of the Reader, Annotator, or Writer
    */
   TypeProduct[] usables() default {};

   /**
    * @return array of typesystem type products of the Reader, Annotator, or Writer
    */
   TypeProduct[] products() default {};


}
