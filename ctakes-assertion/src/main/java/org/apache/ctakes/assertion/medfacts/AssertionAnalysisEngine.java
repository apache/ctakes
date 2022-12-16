/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.assertion.medfacts;

import org.apache.ctakes.assertion.medfacts.i2b2.api.CharacterOffsetToLineTokenConverterCtakesImpl;
import org.apache.ctakes.assertion.medfacts.i2b2.api.SingleDocumentProcessorCtakes;
import org.apache.ctakes.assertion.medfacts.types.Concept;
import org.apache.ctakes.assertion.stub.*;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

//import org.jfree.util.Log;

@PipeBitInfo(
      name = "Assertion Engine",
      description = "Adds Negation, Uncertainty, Conditional and Subject to annotations.",
      role = PipeBitInfo.Role.ANNOTATOR,
      dependencies = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION }
)
public class AssertionAnalysisEngine extends JCasAnnotator_ImplBase
{
  private static Logger logger = Logger.getLogger(AssertionAnalysisEngine.class.getName());
  
  AssertionDecoderConfiguration assertionDecoderConfiguration;

  public AssertionAnalysisEngine()
  {
  }
  
  @Override
  public void initialize(UimaContext uimaContext)
    throws ResourceInitializationException
  {
      super.initialize(uimaContext);
    
      // byte assertionModelContents[];
      String scopeModelFilePath;
      String cueModelFilePath;
      String posModelFilePath;
      File enabledFeaturesFile;

      File assertionModelFile = null;
      try
      {
        String assertionModelResourceKey = "assertionModelResource";
        String assertionModelFilePath = getContext().getResourceFilePath(
            assertionModelResourceKey);
        assertionModelFile = new File(assertionModelFilePath);
        // assertionModelContents = StringHandling
        // .readEntireContentsBinary(assertionModelFile);
        String scopeModelResourceKey = "scopeModelResource";
        scopeModelFilePath = getContext().getResourceFilePath(
            scopeModelResourceKey);
        String cueModelResourceKey = "cueModelResource";
        cueModelFilePath = getContext().getResourceFilePath(cueModelResourceKey);

        String posModelResourceKey = "posModelResource";
        posModelFilePath = getContext().getResourceFilePath(posModelResourceKey);

        String enabledFeaturesResourceKey = "enabledFeaturesResource";
        String enabledFeaturesFilePath = getContext().getResourceFilePath(
            enabledFeaturesResourceKey);
        enabledFeaturesFile = new File(enabledFeaturesFilePath);
      } catch (ResourceAccessException e)
      {
        String message = String.format("problem accessing resource");
        throw new RuntimeException(message, e);
      }

      AssertionDecoderConfiguration assertionDecoderConfiguration = new AssertionDecoderConfiguration();

      logger.info(String.format("scope model file: %s", scopeModelFilePath));
      logger.info(String.format("cue model file: %s", cueModelFilePath));
      ScopeParser scopeParser = new ScopeParser(scopeModelFilePath,
          cueModelFilePath);
      assertionDecoderConfiguration.setScopeParser(scopeParser);

      logger.info(String.format("pos model file: %s", posModelFilePath));
      PartOfSpeechTagger posTagger = new PartOfSpeechTagger(posModelFilePath);
      assertionDecoderConfiguration.setPosTagger(posTagger);

      Set<String> enabledFeatureIdSet = null;
      enabledFeatureIdSet = BatchRunner
          .loadEnabledFeaturesFromFile(enabledFeaturesFile);
      assertionDecoderConfiguration.setEnabledFeatureIdSet(enabledFeatureIdSet);

      JarafeMEDecoder assertionDecoder = null;
      assertionDecoder = new JarafeMEDecoder(assertionModelFile);
      assertionDecoderConfiguration.setAssertionDecoder(assertionDecoder);

      this.assertionDecoderConfiguration = assertionDecoderConfiguration;
  }

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException
  {
    logger.debug("(logging statement) AssertionAnalysisEngine.process() BEGIN");
    String contents = jcas.getDocumentText();

    // String tokenizedContents = tokenizeCasDocumentText(jcas);

    int conceptType = Concept.type;
    AnnotationIndex<Annotation> conceptAnnotationIndex = jcas
        .getAnnotationIndex(conceptType);

    ArrayList<ApiConcept> apiConceptList = new ArrayList<ApiConcept>();
    for (Annotation annotation : conceptAnnotationIndex)
    {
      Concept conceptAnnotation = (Concept) annotation;

      ApiConcept apiConcept = new ApiConcept();
      int begin = conceptAnnotation.getBegin();
      int end = conceptAnnotation.getEnd();
      String conceptText = contents.substring(begin, end);

      apiConcept.setBegin(begin);
      apiConcept.setEnd(end);
      apiConcept.setText(conceptText);
      apiConcept.setType(conceptAnnotation.getConceptType());
      apiConcept.setExternalId(conceptAnnotation.getAddress());

      apiConceptList.add(apiConcept);
    }

    // String conceptFilePath =
    // currentTextFile.getAbsolutePath().replaceFirst("\\.txt$", ".con");
    // File conceptFile = new File(conceptFilePath);
    // logger.info(String.format("    - using concept file \"%s\"...",
    // conceptFile.getName()));
    // String conceptFileContents =
    // StringHandling.readEntireContents(conceptFile);
    // //List<Concept> parseConceptFileContents(conceptFileContents);
    //
    // LineTokenToCharacterOffsetConverter converter =
    // new LineTokenToCharacterOffsetConverter(contents);
    //
    // List<ApiConcept> apiConceptList = parseConceptFile(conceptFile, contents,
    // converter);

    // LineTokenToCharacterOffsetConverter converter =
    // new LineTokenToCharacterOffsetConverter(contents);


    // SingleDocumentProcessor p = new SingleDocumentProcessor();
    SingleDocumentProcessorCtakes p = new SingleDocumentProcessorCtakes();
    p.setJcas(jcas);
    p.setAssertionDecoderConfiguration(assertionDecoderConfiguration);
    // p.setContents(tokenizedContents);
    p.setContents(contents);
    CharacterOffsetToLineTokenConverter converter = new CharacterOffsetToLineTokenConverterCtakesImpl(
        jcas);
    p.setConverter2(converter);
    for (ApiConcept apiConcept : apiConceptList)
    {
      //logger
      //    .info(String.format("dir loader concept: %s", apiConcept.toString()));
      p.addConcept(apiConcept);
    }

    logger
        .debug("(logging statement) AssertionAnalysisEngine.process() BEFORE CALLING p.processSingleDocument()");

    p.processSingleDocument();

    logger
        .debug("(logging statement) AssertionAnalysisEngine.process() AFTER CALLING p.processSingleDocument()");

    Map<Integer, String> assertionTypeMap = p.getAssertionTypeMap();
    //logger.info(String.format("    - done processing ..\"."));

    // Map<Integer, Annotation> annotationMap = generateAnnotationMap(jcas,
    // Concept.type);
    CasIndexer<Annotation> indexer = new CasIndexer<Annotation>(jcas, null);

    //logger.info("assertionTypeMap loop OUTSIDE BEFORE...");
    for (Entry<Integer, String> current : assertionTypeMap.entrySet())
    {
      //logger.info("    assertionTypeMap loop INSIDE BEGIN");
      String currentAssertionType = current.getValue();
      //logger.info(String.format("  currentAssertionType: %s",
      //    currentAssertionType));
      Integer currentIndex = current.getKey();
      ApiConcept originalConcept = apiConceptList.get(currentIndex);

      Concept associatedConcept = (Concept) indexer
          .lookupByAddress(originalConcept.getExternalId());
      int entityAddress = associatedConcept.getOriginalEntityExternalId();
      IdentifiedAnnotation annotation = (IdentifiedAnnotation) indexer
          .lookupByAddress(entityAddress);

      // possible values for currentAssertionType:
      // present
      // absent
      // associated_with_someone_else
      // conditional
      // hypothetical
      // possible

//      logger.info(String.format("removed entityMention (%s) from indexes",
//          entityMention.toString()));
//      entityMention.removeFromIndexes();
      mapI2B2AssertionValueToCtakes(currentAssertionType, annotation);
      
//      // Overwrite mastif's generic attribute with Mayo's generic attribute
//      Boolean generic = GenericAttributeClassifier.getGeneric(jcas,entityMention);
//      Boolean oldgeneric = entityMention.getGeneric();
//      entityMention.setGeneric(generic);
//      System.out.println("overwrote mastif's generic="+oldgeneric+" for "+entityMention.getCoveredText()+" with "+generic);
//
//      // Overwrite mastif's subject attribute with Mayo subject attribute. 
//      // SHARP annotation guidelines have subject=NULL whenever generic=true
//      String subject = null; 
//      String oldsubj = entityMention.getSubject();
//      if (entityMention.getGeneric()==false) {
//          subject = SubjectAttributeClassifier.getSubject(jcas,entityMention);
//      }
//	    entityMention.setSubject(subject);
//      System.out.println("overwrote mastif's subject="+oldsubj+" for "+entityMention.getCoveredText()+" with "+subject);

//      entityMention.addToIndexes();
//      logger.info(String.format("added back entityMention (%s) to indexes",
//          entityMention.toString()));

      // Assertion assertion = new Assertion(jcas, originalConcept.getBegin(),
      // originalConcept.getEnd());
      // assertion.setAssertionType(currentAssertionType);
      // Concept associatedConcept = (Concept)
      // annotationMap.get(originalConcept.getExternalId());
      // assertion.setAssociatedConcept(associatedConcept);
      // assertion.addToIndexes();

      //logger.info("    assertionTypeMap loop INSIDE END");
    }
    //logger.info("assertionTypeMap loop OUTSIDE AFTER!!");
    logger.debug("(logging statement) AssertionAnalysisEngine.process() END");
  }

  public static void mapI2B2AssertionValueToCtakes(String assertionType,
      IdentifiedAnnotation annotation) throws AnalysisEngineProcessException
  {
    if (assertionType == null)
    {
      String message = "current assertion type is null; this is a problem!!";
      System.err.println(message);
      logger.log(Level.ERROR,message);
      // Exception runtimeException = new RuntimeException(message);
      // throw new AnalysisEngineProcessException(runtimeException);
    
      // ALL OBVIOUS ERROR VALUES!!
      annotation.setSubject("skipped");
      annotation.setPolarity(-2);
      annotation.setConfidence(-2.0f);
      annotation.setUncertainty(-2);
      annotation.setConditional(false);
      annotation.setGeneric(false);

    } else if (assertionType.equals("present"))
    // PRESENT (mastif value)
    {
      //debugAnnotationsInCas(jcas, entityMention, "=== BEFORE setting entity mention properties (PRESENT)... ===");
      // ALL DEFAULT VALUES!! (since this is present)
      annotation.setSubject(CONST.ATTR_SUBJECT_PATIENT);
      annotation.setPolarity(1);
      annotation.setConfidence(1.0f);
      annotation.setUncertainty(0);
      annotation.setConditional(false);
      annotation.setGeneric(false);

      //debugAnnotationsInCas(jcas, entityMention, "=== AFTER setting entity mention properties (PRESENT)... ===");
    } else if (assertionType.equals("absent"))
    // ABSENT (mastif value)
    {
      annotation.setSubject(CONST.ATTR_SUBJECT_PATIENT);
      annotation.setPolarity(-1); // NOT DEFAULT VALUE
      annotation.setConfidence(1.0f);
      annotation.setUncertainty(0);
      annotation.setConditional(false);
      annotation.setGeneric(false);

    } else if (assertionType.equals("associated_with_someone_else"))
    // ASSOCIATED WITH SOMEONE ELSE (mastif value)
    {
      annotation.setSubject("CONST.ATTR_SUBJECT_FAMILY_MEMBER"); // NOT DEFAULT VALUE
      annotation.setPolarity(1);
      annotation.setConfidence(1.0f);
      annotation.setUncertainty(0);
      annotation.setConditional(false);
      annotation.setGeneric(false);

    } else if (assertionType.equals("conditional"))
    // CONDITIONAL (mastif value)
    {
      // currently no mapping to sharp type...all sharp properties are defaults!
      annotation.setSubject(CONST.ATTR_SUBJECT_PATIENT);
      annotation.setPolarity(1);
      annotation.setConfidence(1.0f);
      annotation.setUncertainty(0);
      annotation.setConditional(false);
      annotation.setGeneric(false);

    } else if (assertionType.equals("hypothetical"))
    // HYPOTHETICAL (mastif value)
    {
      annotation.setSubject(CONST.ATTR_SUBJECT_PATIENT);
      annotation.setPolarity(1);
      annotation.setConfidence(1.0f);
      annotation.setUncertainty(0);
      annotation.setConditional(true); // NOT DEFAULT VALUE
      annotation.setGeneric(false);

    } else if (assertionType.equals("possible"))
    // POSSIBLE (mastif value)
    {
      annotation.setSubject(CONST.ATTR_SUBJECT_PATIENT);
      annotation.setPolarity(1);
      annotation.setConfidence(1.0f);
      annotation.setUncertainty(1); // NOT DEFAULT VALUE
      annotation.setConditional(false);
      annotation.setGeneric(false);
    } else
    {
      String message = String.format(
          "unexpected assertion value returned!! \"%s\"",
          assertionType);
      logger.log(Level.ERROR,message);
//      System.err.println(message);
      Exception runtimeException = new RuntimeException(message);
      throw new AnalysisEngineProcessException(runtimeException);
    }
  }

  public void debugAnnotationsInCas(JCas jcas, IdentifiedAnnotation annotation,
      String label)
  {
    CasIndexer<IdentifiedAnnotation> i = new CasIndexer<IdentifiedAnnotation>(jcas, annotation.getType());
    
    StringBuilder b = new StringBuilder();
    b.append(String.format("<<<<<%n### TARGET ###%nclass: %s%naddress: %d%nvalue: %s%n### END TARGET ###%n>>>>>%n%n", annotation.getClass().getName(), annotation.getAddress(), annotation.toString()));
    
    String debugOutput = i.convertToDebugOutput(label, annotation);
    
    b.append(debugOutput);
    
    logger.debug(b.toString());
    
  }

  public Map<Integer, Annotation> generateAnnotationMap(JCas jcas)
  {
    return generateAnnotationMap(jcas, null);
  }

  public Map<Integer, Annotation> generateAnnotationMap(JCas jcas,
      Integer typeId)
  {
    Map<Integer, Annotation> annotationMap = new HashMap<Integer, Annotation>();

    AnnotationIndex<Annotation> index = null;
    if (typeId == null)
    {
      index = jcas.getAnnotationIndex();
    } else
    {
      index = jcas.getAnnotationIndex(typeId);
    }
    FSIterator<Annotation> iterator = index.iterator();
    while (iterator.hasNext())
    {
      Annotation current = iterator.next();
      int address = current.getAddress();
      annotationMap.put(address, current);
    }

    return annotationMap;
  }

  // public String tokenizeCasDocumentText(JCas jcas)
  // {
  // ArrayList<ArrayList<String>> arrayOfLines = construct2DTokenArray(jcas);
  //
  // String spaceSeparatedTokensInput = convert2DTokenArrayToText(arrayOfLines);
  //
  // return spaceSeparatedTokensInput;
  // }
  //
  // public ArrayList<ArrayList<String>> construct2DTokenArray(JCas jcas)
  // {
  // int sentenceType = Sentence.type;
  // AnnotationIndex<Annotation> sentenceAnnotationIndex =
  // jcas.getAnnotationIndex(sentenceType);
  // ArrayList<ArrayList<String>> arrayOfLines = new
  // ArrayList<ArrayList<String>>();
  //
  // //ArrayList<ApiConcept> apiConceptList = new ArrayList<ApiConcept>();
  // for (Annotation annotation : sentenceAnnotationIndex)
  // {
  // Sentence sentence = (Sentence)annotation;
  // int sentenceBegin = sentence.getBegin();
  // int sentenceEnd = sentence.getEnd();
  //
  // AnnotationIndex<Annotation> tokenAnnotationIndex =
  // jcas.getAnnotationIndex(BaseToken.type);
  // ArrayList<String> arrayOfTokens = new ArrayList<String>();
  // for (Annotation baseTokenAnnotationUntyped : tokenAnnotationIndex)
  // {
  // // ignore tokens that are outside of the sentence.
  // // there has to be a better way to do this with Constraints, but this
  // // should work for now...
  // if (baseTokenAnnotationUntyped.getBegin() < sentenceBegin ||
  // baseTokenAnnotationUntyped.getEnd() > sentenceEnd)
  // {
  // continue;
  // }
  // BaseToken baseToken = (BaseToken)baseTokenAnnotationUntyped;
  // if (baseToken instanceof WordToken ||
  // baseToken instanceof PunctuationToken)
  // {
  // String currentTokenText = baseToken.getCoveredText();
  // arrayOfTokens.add(currentTokenText);
  // }
  // }
  // arrayOfLines.add(arrayOfTokens);
  //
  // }
  // return arrayOfLines;
  // }
  //
  public String convert2DTokenArrayToText(
      ArrayList<ArrayList<String>> arrayOfLines)
  {
    final String DELIM = " ";
    StringWriter writer = new StringWriter();
    PrintWriter printer = new PrintWriter(writer);

    boolean isFirstLine = true;
    for (ArrayList<String> line : arrayOfLines)
    {
      if (!isFirstLine)
      {
        printer.println();
      }

      boolean isFirstTokenOnLine = true;
      for (String currentToken : line)
      {
        if (!isFirstTokenOnLine)
        {
          printer.print(DELIM);
        }
        printer.print(currentToken);
        isFirstTokenOnLine = false;
      }

      isFirstLine = false;
    }

    printer.close();

    String output = writer.toString();
    return output;
  }

}
