package org.apache.ctakes.core.util;

import org.apache.ctakes.core.util.annotation.OntologyConceptUtil;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 11/23/2015
 */
public class OntologyConceptUtilTester {

   static private final Logger LOGGER = Logger.getLogger( "IdentifiedAnnotationUtilTester" );

   static private final String TUI_1 = "T001";
   static private final String TUI_2 = "T002";
   static private final String TUI_3 = "T003";

   static private final String CUI_1 = "C0000111";
   static private final String CUI_2 = "C0002222";
   static private final String CUI_3 = "C0033333";
   static private final String CUI_4 = "C0444444";
   static private final String CUI_5 = "C5555555";

   static private final String SCHEME_A = "SchemeA";
   static private final String SCHEME_B = "SchemeB";
   static private final String SCHEME_C = "SchemeC";

   static private final String CODE_1 = "111";
   static private final String CODE_2 = "222";
   static private final String CODE_3 = "333";
   static private final String CODE_4 = "444";
   static private final String CODE_5 = "555";
   static private final String CODE_6 = "666";
   static private final String CODE_7 = "777";

   static private JCas _testCas;
   static private IdentifiedAnnotation TUI_1_CUI_1;
   static private IdentifiedAnnotation TUI_1_CUI_2_CODE_A1;
   static private IdentifiedAnnotation TUI_1_CUI_2_CODE_A2;
   static private IdentifiedAnnotation TUI_1_CUI_2_CODE_B2;
   static private IdentifiedAnnotation TUI_2_CUI_3_CODE_B3;
   static private IdentifiedAnnotation TUI_2_CUI_3_CODE_B4;
   static private IdentifiedAnnotation TUI_2_CUI_3_CODE_B5_C6_C7;
   static private IdentifiedAnnotation TUI_3_CUI_45;


   @BeforeClass
   static public void setupTestCas() throws UIMAException {
      try {
         _testCas = JCasFactory.createJCas();

         TUI_1_CUI_1 = createAnnotation( _testCas,
               createUmlsConcept( _testCas, TUI_1, CUI_1, null, null ) );

         TUI_1_CUI_2_CODE_A1 = createAnnotation( _testCas,
               createUmlsConcept( _testCas, TUI_1, CUI_2, SCHEME_A, CODE_1 ) );
         TUI_1_CUI_2_CODE_A2 = createAnnotation( _testCas,
               createUmlsConcept( _testCas, TUI_1, CUI_2, SCHEME_A, CODE_2 ) );
         TUI_1_CUI_2_CODE_B2 = createAnnotation( _testCas,
               createUmlsConcept( _testCas, TUI_1, CUI_2, SCHEME_B, CODE_2 ) );

         TUI_2_CUI_3_CODE_B3 = createAnnotation( _testCas,
               createUmlsConcept( _testCas, TUI_2, CUI_3, null, null ),
               createOntologyConcept( _testCas, SCHEME_B, CODE_3 ) );
         TUI_2_CUI_3_CODE_B4 = createAnnotation( _testCas,
               createUmlsConcept( _testCas, TUI_2, CUI_3, null, null ),
               createOntologyConcept( _testCas, SCHEME_B, CODE_4 ) );

         TUI_2_CUI_3_CODE_B5_C6_C7 = createAnnotation( _testCas,
               createUmlsConcept( _testCas, TUI_2, CUI_3, null, null ),
               createOntologyConcept( _testCas, SCHEME_B, CODE_5 ),
               createOntologyConcept( _testCas, SCHEME_C, CODE_6 ),
               createOntologyConcept( _testCas, SCHEME_C, CODE_7 ) );

         TUI_3_CUI_45 = createAnnotation( _testCas,
               createUmlsConcept( _testCas, TUI_3, CUI_4, null, null ),
               createUmlsConcept( _testCas, TUI_3, CUI_5, null, null ) );

      } catch ( UIMAException uimaE ) {
         LOGGER.error( "Could not create test CAS " + uimaE.getMessage() );
         throw uimaE;
      }
   }

   static private IdentifiedAnnotation createAnnotation( final JCas jcas,
                                                         final OntologyConcept... concepts ) {
      final FSArray conceptArray = new FSArray( jcas, concepts.length );
      int arrIdx = 0;
      for ( OntologyConcept concept : concepts ) {
         conceptArray.set( arrIdx, concept );
         arrIdx++;
      }
      final IdentifiedAnnotation annotation = new IdentifiedAnnotation( jcas );
      annotation.setOntologyConceptArr( conceptArray );
      annotation.addToIndexes();
      return annotation;
   }

   static private UmlsConcept createUmlsConcept( final JCas jcas,
                                                 final String tui, final String cui,
                                                 final String codingScheme, final String code ) {
      final UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCui( cui );
      umlsConcept.setTui( tui );
      if ( codingScheme != null ) {
         umlsConcept.setCodingScheme( codingScheme );
      }
      if ( code != null ) {
         umlsConcept.setCode( code );
      }
      return umlsConcept;
   }

   static private OntologyConcept createOntologyConcept( final JCas jcas,
                                                         final String codingScheme, final String code ) {
      final OntologyConcept ontologyConcept = new OntologyConcept( jcas );
      ontologyConcept.setCodingScheme( codingScheme );
      ontologyConcept.setCode( code );
      return ontologyConcept;
   }

   static private boolean assertUmlsConcept( final Collection<UmlsConcept> umlsConcepts,
                                             final String cui, final String tui ) {
      return umlsConcepts.stream()
            .filter( uc -> tui.equals( uc.getTui() ) )
            .anyMatch( uc -> cui.equals( uc.getCui() ) );
   }

   static private boolean assertOntologyConcept( final Collection<OntologyConcept> ontologyConcepts,
                                                 final String code ) {
      return ontologyConcepts.stream()
            .anyMatch( oc -> code.equals( oc.getCode() ) );
   }

//   @Test
//   public void canary() {
//      assertTrue( "Canary should Fail", false );
//      assertTrue( "Canary should Pass", true );
//   }

   @Test
   public void testGetConcepts() {
      assertEquals( "Null IdentifiedAnnotation should have no concepts", 0,
            OntologyConceptUtil.getUmlsConcepts( null ).size() );
      assertConceptCount( TUI_1_CUI_1, 1 );
      assertConceptCount( TUI_1_CUI_2_CODE_A1, 1 );
      assertConceptCount( TUI_2_CUI_3_CODE_B3, 1 );
      assertConceptCount( TUI_2_CUI_3_CODE_B5_C6_C7, 1 );
      assertConceptCount( TUI_3_CUI_45, 2 );
   }

   static private void assertConceptCount( final IdentifiedAnnotation annotation, final int wantedCount ) {
      assertEquals( "Incorrect number of UmlsConcepts", wantedCount,
            OntologyConceptUtil.getUmlsConcepts( annotation ).size() );
   }

   @Test
   public void testGetCuis() {
      assertEquals( "Null IdentifiedAnnotation should have no cuis", 0,
            OntologyConceptUtil.getCuis( (IdentifiedAnnotation)null ).size() );
      assertCui( TUI_1_CUI_1, CUI_1 );
      assertCui( TUI_1_CUI_2_CODE_A1, CUI_2 );
      assertCui( TUI_1_CUI_2_CODE_A2, CUI_2 );
      assertCui( TUI_1_CUI_2_CODE_B2, CUI_2 );
      assertCui( TUI_2_CUI_3_CODE_B3, CUI_3 );
      assertCui( TUI_2_CUI_3_CODE_B4, CUI_3 );
      assertCui( TUI_2_CUI_3_CODE_B5_C6_C7, CUI_3 );
      assertCui( TUI_3_CUI_45, CUI_4, CUI_5 );
   }

   static private void assertCui( final IdentifiedAnnotation annotation, final String... wantedCuis ) {
      final Collection<String> cuis = OntologyConceptUtil.getCuis( annotation );
      assertEquals( "Incorrect number of cuis", wantedCuis.length, cuis.size() );
      for ( String cui : wantedCuis ) {
         assertTrue( "Cui Collection does not contain " + cui, cuis.contains( cui ) );
      }
   }

   @Test
   public void testGetTuis() {
      assertEquals( "Null IdentifiedAnnotation should have no tuis", 0,
            OntologyConceptUtil.getTuis( (IdentifiedAnnotation)null ).size() );
      assertTui( TUI_1_CUI_1, TUI_1 );
      assertTui( TUI_1_CUI_2_CODE_A1, TUI_1 );
      assertTui( TUI_1_CUI_2_CODE_A2, TUI_1 );
      assertTui( TUI_1_CUI_2_CODE_B2, TUI_1 );
      assertTui( TUI_2_CUI_3_CODE_B3, TUI_2 );
      assertTui( TUI_2_CUI_3_CODE_B4, TUI_2 );
      assertTui( TUI_2_CUI_3_CODE_B5_C6_C7, TUI_2 );
      assertTui( TUI_3_CUI_45, TUI_3 );
   }

   static private void assertTui( final IdentifiedAnnotation annotation, final String... wantedTuis ) {
      final Collection<String> tuis = OntologyConceptUtil.getTuis( annotation );
      assertEquals( "Incorrect number of tuis", wantedTuis.length, tuis.size() );
      for ( String tui : wantedTuis ) {
         assertTrue( "Tui Collection does not contain " + tui, tuis.contains( tui ) );
      }
   }

   @Test
   public void testGetSchemeCodes() {
      assertEquals( "Null IdentifiedAnnotation should have no scheme codes", 0,
            OntologyConceptUtil.getSchemeCodes( (IdentifiedAnnotation)null ).size() );
      assertEquals( "Incorrect number of Schemes", 0, OntologyConceptUtil.getSchemeCodes( TUI_1_CUI_1 ).size() );
      assertSchemes( TUI_1_CUI_2_CODE_A1, SCHEME_A );
      assertSchemeCodes( TUI_1_CUI_2_CODE_A1, SCHEME_A, CODE_1 );
      assertSchemes( TUI_1_CUI_2_CODE_A2, SCHEME_A );
      assertSchemeCodes( TUI_1_CUI_2_CODE_A2, SCHEME_A, CODE_2 );
      assertSchemes( TUI_1_CUI_2_CODE_B2, SCHEME_B );
      assertSchemeCodes( TUI_1_CUI_2_CODE_B2, SCHEME_B, CODE_2 );
      assertSchemes( TUI_2_CUI_3_CODE_B3, SCHEME_B );
      assertSchemeCodes( TUI_2_CUI_3_CODE_B3, SCHEME_B, CODE_3 );
      assertSchemes( TUI_2_CUI_3_CODE_B4, SCHEME_B );
      assertSchemeCodes( TUI_2_CUI_3_CODE_B4, SCHEME_B, CODE_4 );
      assertSchemes( TUI_2_CUI_3_CODE_B5_C6_C7, SCHEME_B, SCHEME_C );
      assertSchemeCodes( TUI_2_CUI_3_CODE_B5_C6_C7, SCHEME_B, CODE_5 );
      assertSchemeCodes( TUI_2_CUI_3_CODE_B5_C6_C7, SCHEME_C, CODE_6, CODE_7 );
      assertEquals( "Incorrect number of Schemes", 0, OntologyConceptUtil.getSchemeCodes( TUI_3_CUI_45 ).size() );
   }

   static private void assertSchemes( final IdentifiedAnnotation annotation, final String... wantedSchemes ) {
      final Map<String, Collection<String>> schemeCodeMap = OntologyConceptUtil.getSchemeCodes( annotation );
      assertEquals( "Incorrect number of Schemes", wantedSchemes.length, schemeCodeMap.size() );
      for ( String scheme : wantedSchemes ) {
         assertTrue( "Scheme keyset does not contain " + scheme, schemeCodeMap.containsKey( scheme ) );
      }
   }

   static private void assertSchemeCodes( final IdentifiedAnnotation annotation,
                                          final String wantedScheme, final String... wantedCodes ) {
      final Map<String, Collection<String>> schemeCodeMap = OntologyConceptUtil.getSchemeCodes( annotation );
      assertTrue( "Scheme keyset does not contain " + wantedScheme, schemeCodeMap.containsKey( wantedScheme ) );
      final Collection<String> codes = schemeCodeMap.get( wantedScheme );
      assertEquals( "Incorrect number of code values", wantedCodes.length, codes.size() );
      for ( String code : wantedCodes ) {
         assertTrue( "Code valueset does not contain " + code, codes.contains( code ) );
      }
   }

   @Test
   public void testGetCodes() {
      assertEquals( "Null IdentifiedAnnotation should have no codes", 0,
            OntologyConceptUtil.getCodes( (IdentifiedAnnotation)null ).size() );
      assertEquals( "Incorrect number of Codes", 0, OntologyConceptUtil.getCodes( TUI_1_CUI_1 ).size() );
      assertCodes( TUI_1_CUI_2_CODE_A1, CODE_1 );
      assertCodes( TUI_1_CUI_2_CODE_A2, CODE_2 );
      assertCodes( TUI_1_CUI_2_CODE_B2, CODE_2 );
      assertCodes( TUI_2_CUI_3_CODE_B3, CODE_3 );
      assertCodes( TUI_2_CUI_3_CODE_B4, CODE_4 );
      assertCodes( TUI_2_CUI_3_CODE_B5_C6_C7, CODE_5, CODE_6, CODE_7 );
      assertEquals( "Incorrect number of Codes", 0, OntologyConceptUtil.getCodes( TUI_3_CUI_45 ).size() );
   }

   static private void assertCodes( final IdentifiedAnnotation annotation, final String... wantedCodes ) {
      final Collection<String> codes = OntologyConceptUtil.getCodes( annotation );
      assertEquals( "Incorrect number of Codes", wantedCodes.length, codes.size() );
      for ( String code : wantedCodes ) {
         assertTrue( "Code valueset does not contain " + code, codes.contains( code ) );
      }
   }

   @Test
   public void testGetCodesByScheme() {
      assertEquals( "Null IdentifiedAnnotation should have no codes", 0,
            OntologyConceptUtil.getCodes( (IdentifiedAnnotation)null, SCHEME_A ).size() );
      assertEquals( "Incorrect number of Codes", 0, OntologyConceptUtil.getCodes( TUI_1_CUI_1, SCHEME_A ).size() );
      assertCodesByScheme( TUI_1_CUI_2_CODE_A1, SCHEME_A, CODE_1 );
      assertCodesByScheme( TUI_1_CUI_2_CODE_A2, SCHEME_A, CODE_2 );
      assertCodesByScheme( TUI_1_CUI_2_CODE_B2, SCHEME_B, CODE_2 );
      assertCodesByScheme( TUI_2_CUI_3_CODE_B3, SCHEME_B, CODE_3 );
      assertCodesByScheme( TUI_2_CUI_3_CODE_B4, SCHEME_B, CODE_4 );
      assertCodesByScheme( TUI_2_CUI_3_CODE_B5_C6_C7, SCHEME_B, CODE_5 );
      assertCodesByScheme( TUI_2_CUI_3_CODE_B5_C6_C7, SCHEME_C, CODE_6, CODE_7 );
      assertEquals( "Incorrect number of Codes", 0, OntologyConceptUtil.getCodes( TUI_3_CUI_45 ).size() );
   }

   static private void assertCodesByScheme( final IdentifiedAnnotation annotation,
                                            final String wantedScheme, final String... wantedCodes ) {
      final Collection<String> codes = OntologyConceptUtil.getCodes( annotation, wantedScheme );
      assertEquals( "Incorrect number of Codes", wantedCodes.length, codes.size() );
      for ( String code : wantedCodes ) {
         assertTrue( "Codes does not contain " + code, codes.contains( code ) );
      }
   }

   @Test
   public void testGetCuisInCas() {
      final Collection<String> cuis = OntologyConceptUtil.getCuis( _testCas );
      assertEquals( "Incorrect number of cuis", 5, cuis.size() );
      assertTrue( "Missing cui " + CUI_1, cuis.contains( CUI_1 ) );
      assertTrue( "Missing cui " + CUI_2, cuis.contains( CUI_2 ) );
      assertTrue( "Missing cui " + CUI_3, cuis.contains( CUI_3 ) );
      assertTrue( "Missing cui " + CUI_4, cuis.contains( CUI_4 ) );
      assertTrue( "Missing cui " + CUI_5, cuis.contains( CUI_5 ) );
   }

   @Test
   public void testGetTuisInCas() {
      final Collection<String> tuis = OntologyConceptUtil.getTuis( _testCas );
      assertEquals( "Incorrect number of tuis", 3, tuis.size() );
      assertTrue( "Missing tui " + TUI_1, tuis.contains( TUI_1 ) );
      assertTrue( "Missing tui " + TUI_2, tuis.contains( TUI_2 ) );
      assertTrue( "Missing tui " + TUI_3, tuis.contains( TUI_3 ) );
   }

   @Test
   public void testGetSchemeCodesInCas() {
      final Map<String, Collection<String>> schemeCodeMap = OntologyConceptUtil.getSchemeCodes( _testCas );
      assertEquals( "Incorrect number of Schemes", 3, schemeCodeMap.keySet().size() );
      assertSchemeCodes( schemeCodeMap, SCHEME_A, CODE_1, CODE_2 );
      assertSchemeCodes( schemeCodeMap, SCHEME_B, CODE_2, CODE_3, CODE_4, CODE_5 );
      assertSchemeCodes( schemeCodeMap, SCHEME_C, CODE_6, CODE_7 );
   }

   static private void assertSchemeCodes( final Map<String, Collection<String>> schemeCodeMap,
                                          final String wantedScheme, final String... wantedCodes ) {
      assertTrue( "Scheme keyset does not contain " + wantedScheme, schemeCodeMap.containsKey( wantedScheme ) );
      final Collection<String> codes = schemeCodeMap.get( wantedScheme );
      assertEquals( "Incorrect number of code values", wantedCodes.length, codes.size() );
      for ( String code : wantedCodes ) {
         assertTrue( "Code valueset does not contain " + code, codes.contains( code ) );
      }
   }

   @Test
   public void getCodesInCas() {
      final Collection<String> codes = OntologyConceptUtil.getCodes( _testCas );
      assertEquals( "Incorrect number of Codes", 7, codes.size() );
      assertTrue( "Missing code " + CODE_1, codes.contains( CODE_1 ) );
      assertTrue( "Missing code " + CODE_2, codes.contains( CODE_2 ) );
      assertTrue( "Missing code " + CODE_3, codes.contains( CODE_3 ) );
      assertTrue( "Missing code " + CODE_4, codes.contains( CODE_4 ) );
      assertTrue( "Missing code " + CODE_5, codes.contains( CODE_5 ) );
      assertTrue( "Missing code " + CODE_6, codes.contains( CODE_6 ) );
      assertTrue( "Missing code " + CODE_7, codes.contains( CODE_7 ) );
   }

   @Test
   public void getCodesBySchemeInCas() {
      assertCodesByScheme( _testCas, SCHEME_A, CODE_1, CODE_2 );
      assertCodesByScheme( _testCas, SCHEME_B, CODE_2, CODE_3, CODE_4, CODE_5 );
      assertCodesByScheme( _testCas, SCHEME_C, CODE_6, CODE_7 );
   }

   static private void assertCodesByScheme( final JCas jcas, final String wantedScheme, final String... wantedCodes ) {
      final Collection<String> codes = OntologyConceptUtil.getCodes( jcas, wantedScheme );
      assertEquals( "Incorrect number of Codes", wantedCodes.length, codes.size() );
      for ( String code : wantedCodes ) {
         assertTrue( "Codes does not contain " + code, codes.contains( code ) );
      }
   }

   @Test
   public void testGetAnnotationsByCui() {
      assertAnnotationsByCui( _testCas, CUI_1, TUI_1_CUI_1 );
      assertAnnotationsByCui( _testCas, CUI_2, TUI_1_CUI_2_CODE_A1, TUI_1_CUI_2_CODE_A2, TUI_1_CUI_2_CODE_B2 );
      assertAnnotationsByCui( _testCas, CUI_3, TUI_2_CUI_3_CODE_B3, TUI_2_CUI_3_CODE_B4, TUI_2_CUI_3_CODE_B5_C6_C7 );
      assertAnnotationsByCui( _testCas, CUI_4, TUI_3_CUI_45 );
      assertAnnotationsByCui( _testCas, CUI_5, TUI_3_CUI_45 );
   }

   static private void assertAnnotationsByCui( final JCas jcas, final String cui,
                                               final IdentifiedAnnotation... wantedAnnotations ) {
      final Collection<IdentifiedAnnotation> annotations = OntologyConceptUtil.getAnnotationsByCui( jcas, cui );
      assertEquals( "Incorrect number of annotations", wantedAnnotations.length, annotations.size() );
      for ( IdentifiedAnnotation annotation : wantedAnnotations ) {
         assertTrue( "Codes does not contain " + annotation, annotations.contains( annotation ) );
      }
   }

   @Test
   public void testGetAnnotationsByTui() {
      assertAnnotationsByTui( _testCas, TUI_1,
            TUI_1_CUI_1, TUI_1_CUI_2_CODE_A1, TUI_1_CUI_2_CODE_A2, TUI_1_CUI_2_CODE_B2 );
      assertAnnotationsByTui( _testCas, TUI_2, TUI_2_CUI_3_CODE_B3, TUI_2_CUI_3_CODE_B4, TUI_2_CUI_3_CODE_B5_C6_C7 );
      assertAnnotationsByTui( _testCas, TUI_3, TUI_3_CUI_45 );
   }

   static private void assertAnnotationsByTui( final JCas jcas, final String tui,
                                               final IdentifiedAnnotation... wantedAnnotations ) {
      final Collection<IdentifiedAnnotation> annotations = OntologyConceptUtil.getAnnotationsByTui( jcas, tui );
      assertEquals( "Incorrect number of annotations", wantedAnnotations.length, annotations.size() );
      for ( IdentifiedAnnotation annotation : wantedAnnotations ) {
         assertTrue( "Codes does not contain " + annotation, annotations.contains( annotation ) );
      }
   }

   @Test
   public void testGetAnnotationsByCode() {
      assertAnnotationsByCode( _testCas, CODE_1, TUI_1_CUI_2_CODE_A1 );
      assertAnnotationsByCode( _testCas, CODE_2, TUI_1_CUI_2_CODE_A2, TUI_1_CUI_2_CODE_B2 );
      assertAnnotationsByCode( _testCas, CODE_3, TUI_2_CUI_3_CODE_B3 );
      assertAnnotationsByCode( _testCas, CODE_4, TUI_2_CUI_3_CODE_B4 );
      assertAnnotationsByCode( _testCas, CODE_5, TUI_2_CUI_3_CODE_B5_C6_C7 );
      assertAnnotationsByCode( _testCas, CODE_6, TUI_2_CUI_3_CODE_B5_C6_C7 );
      assertAnnotationsByCode( _testCas, CODE_7, TUI_2_CUI_3_CODE_B5_C6_C7 );
   }

   static private void assertAnnotationsByCode( final JCas jcas, final String code,
                                                final IdentifiedAnnotation... wantedAnnotations ) {
      final Collection<IdentifiedAnnotation> annotations = OntologyConceptUtil.getAnnotationsByCode( jcas, code );
      assertEquals( "Incorrect number of annotations", wantedAnnotations.length, annotations.size() );
      for ( IdentifiedAnnotation annotation : wantedAnnotations ) {
         assertTrue( "Codes does not contain " + annotation, annotations.contains( annotation ) );
      }
   }


}
