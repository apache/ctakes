//package org.apache.ctakes.dictionary.lookup2.ae;
//
//import org.apache.ctakes.dictionary.lookup2.util.UmlsUserTester;
//import org.apache.log4j.Logger;
//import org.apache.uima.UIMAException;
//import org.apache.uima.analysis_engine.AnalysisEngineDescription;
//import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
//import org.apache.uima.fit.factory.JCasFactory;
//import org.apache.uima.fit.pipeline.SimplePipeline;
//import org.apache.uima.jcas.JCas;
//import org.apache.uima.resource.ResourceInitializationException;
//import org.junit.Test;
//
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.fail;
//
//
//final public class TestDictionaryLoadResources {
//
//   static private final Logger LOGGER = Logger.getLogger( "TestDictionaryLoadResources" );
//
//
//   static private final String TEST_TEXT
//         = "The quick red fox jumped over cTAKES.  Allie had a little lamb; little lamb.";
//
//   static private final String TEST_CUSTOM_DESC = "org/apache/ctakes/dictionary/lookup/fast/TestcTakesHsql.xml";
//
//   /**
//    *
//    */
//   @Test
//   public void testDefaultLookupDescription() {
//      if ( !UmlsUserTester.canTestUmlsUser() ) {
//         LOGGER.warn( "No UMLS User or Pass specified, cannot test Default Lookup Description" );
//         return;
//      }
//      final JCas jcas = createTestJCas();
//      try {
//         //Test had to use custom test config otherwise we'll have to save our umls credentials.
//         final AnalysisEngineDescription aed
//               = DictionaryLookupFactory.createDefaultDictionaryLookupDescription();
//         SimplePipeline.runPipeline( jcas, aed );
//      } catch ( AnalysisEngineProcessException | ResourceInitializationException multE ) {
//         // Since this is a Test, use a fail instead of throwing an exception
//         fail( multE.getMessage() );
//      }
//   }
//
//   @Test
//   public void testCustomLookupDescription() {
//      final JCas jcas = createTestJCas();
//      try {
//         //Test had to use custom test config otherwise we'll have to save our umls credentials.
//         final AnalysisEngineDescription aed
//               = DictionaryLookupFactory.createCustomDictionaryLookupDescription( TEST_CUSTOM_DESC );
//         SimplePipeline.runPipeline( jcas, aed );
//      } catch ( AnalysisEngineProcessException | ResourceInitializationException multE ) {
//         fail( multE.getMessage() );
//      }
//   }
//
//   @Test
//   public void testOverlapLookupDescription() {
//      if ( !UmlsUserTester.canTestUmlsUser() ) {
//         LOGGER.warn( "No UMLS User or Pass specified, cannot test Overlap Lookup Description" );
//         return;
//      }
//      final JCas jcas = createTestJCas();
//      try {
//         //Test had to use custom test config otherwise we'll have to save our umls credentials.
//         final AnalysisEngineDescription aed
//               = DictionaryLookupFactory.createOverlapDictionaryLookupDescription();
//         SimplePipeline.runPipeline( jcas, aed );
//      } catch ( AnalysisEngineProcessException | ResourceInitializationException multE ) {
//         fail( multE.getMessage() );
//      }
//   }
//
//   @Test
//   public void testCustomOverlapLookupDescription() {
//      final JCas jcas = createTestJCas();
//      try {
//         //Test had to use custom test config otherwise we'll have to save our umls credentials.
//         final AnalysisEngineDescription aed
//               = DictionaryLookupFactory.createCustomOverlapDictionaryLookupDescription( TEST_CUSTOM_DESC );
//         SimplePipeline.runPipeline( jcas, aed );
//      } catch ( AnalysisEngineProcessException | ResourceInitializationException multE ) {
//         fail( multE.getMessage() );
//      }
//   }
//
//
//   static private JCas createTestJCas() {
////      TypeSystemDescription typeSystem = TypeSystemDescriptionFactory.createTypeSystemDescription();
//      JCas jcas = null;
//      try {
//         jcas = JCasFactory.createJCas();
//         jcas.setDocumentText( TEST_TEXT );
//      } catch ( UIMAException uimaE ) {
//         fail( uimaE.getMessage() );
//      }
//      assertNotNull( "JCas could not be created", jcas );
//      return jcas;
//   }
//
//}
