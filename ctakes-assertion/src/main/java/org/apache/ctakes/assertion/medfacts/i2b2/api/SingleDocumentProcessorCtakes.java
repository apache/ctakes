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
package org.apache.ctakes.assertion.medfacts.i2b2.api;

import org.apache.ctakes.assertion.stub.ApiConcept;
import org.apache.ctakes.assertion.stub.LineAndTokenPosition;
import org.apache.ctakes.assertion.stub.SingleDocumentProcessor;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.List;

public class SingleDocumentProcessorCtakes extends SingleDocumentProcessor
{
	
  Logger LOGGER = LoggerFactory.getLogger(SingleDocumentProcessorCtakes.class.getName());
  
  protected JCas jcas;


  public SingleDocumentProcessorCtakes()
  {
    super();
  }

//  public SingleDocumentProcessorCtakes(
//      LineTokenToCharacterOffsetConverter converter)
//  {
//    super(converter);
//  }

  public JCas getJcas()
  {
    return jcas;
  }

  public void setJcas(JCas jcas)
  {
    this.jcas = jcas;
  }
  
  @Override
  protected void preExecutionTest()
  {
    if (converter2 == null)
    {
      converter2 =
          new CharacterOffsetToLineTokenConverterCtakesImpl(jcas);
    }
  }

  public void preprocess()
  {
    String arrayOfArrayOfTokens[][] = null;
    
    ArrayList<ArrayList<String>> returnedObject = construct2DTokenArray(jcas);
    arrayOfArrayOfTokens = new String[returnedObject.size()][];
    String template[] = new String[0];
    for (int i=0; i < returnedObject.size(); i++)
    {
      ArrayList<String> current = returnedObject.get(i);
      String temp[] = current.toArray(template);
      arrayOfArrayOfTokens[i] = temp;
    }
    
    
    this.arrayOfArrayOfTokens = arrayOfArrayOfTokens;
  }
  
  public void postprocess()
  {
    
  }

  public ArrayList<ArrayList<String>> construct2DTokenArray(JCas jcas)
  {
    int sentenceType = Sentence.type;
    AnnotationIndex<Annotation> sentenceAnnotationIndex =
      jcas.getAnnotationIndex(sentenceType);
    ArrayList<ArrayList<String>> arrayOfLines = new ArrayList<ArrayList<String>>();
    
    //ArrayList<ApiConcept> apiConceptList = new ArrayList<ApiConcept>();
    for (Annotation annotation : sentenceAnnotationIndex)
    {
      Sentence sentence = (Sentence)annotation;
      int sentenceBegin = sentence.getBegin();
      int sentenceEnd = sentence.getEnd();
      
      AnnotationIndex<Annotation> tokenAnnotationIndex = jcas.getAnnotationIndex(BaseToken.type);
      FSIterator<Annotation> tokensInThisSentenceIterator = tokenAnnotationIndex.subiterator(sentence);
      ArrayList<String> arrayOfTokens = new ArrayList<String>();
      //for (Annotation baseTokenAnnotationUntyped : tokenAnnotationIndex)
      while (tokensInThisSentenceIterator.hasNext())
      {
        Annotation baseTokenAnnotationUntyped = tokensInThisSentenceIterator.next();
//        // ignore tokens that are outside of the sentence.
//        // there has to be a better way to do this with Constraints, but this
//        // should work for now...
//        if (baseTokenAnnotationUntyped.getBegin() < sentenceBegin ||
//            baseTokenAnnotationUntyped.getEnd() > sentenceEnd)
//        {
//          continue;
//        }
        BaseToken baseToken = (BaseToken)baseTokenAnnotationUntyped;
        if (!(baseToken instanceof NewlineToken))
        {
          String currentTokenText = baseToken.getCoveredText();
          arrayOfTokens.add(currentTokenText);
        }
      }
      arrayOfLines.add(arrayOfTokens);
      
    }
    return arrayOfLines;
  }
  
  /**
   * delegate to converter to determine offset.
   */
  @Override
  public LineAndTokenPosition convertCharacterOffsetToLineToken(int characterOffset)
  {
	  return converter2.convert(characterOffset);  
  }

  @Override
  public List<LineAndTokenPosition> calculateBeginAndEndOfConcept
    (ApiConcept problem)
  {
    return calculateBeginAndEndOfConcept(problem.getBegin(), problem.getEnd());
  }
  
  /**
   * delegate to converter to determine offset.
   */
  public List<LineAndTokenPosition> calculateBeginAndEndOfConcept(
      int problemBegin, int problemEnd)
  {
      return ((CharacterOffsetToLineTokenConverterCtakesImpl) this.converter2)
              .calculateBeginAndEndOfConcept(problemBegin, problemEnd);  
  }

}

