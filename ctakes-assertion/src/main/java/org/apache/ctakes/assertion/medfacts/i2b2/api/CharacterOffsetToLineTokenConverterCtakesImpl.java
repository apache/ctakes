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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.ctakes.assertion.stub.ApiConcept;
import org.apache.ctakes.assertion.stub.CharacterOffsetToLineTokenConverter;
import org.apache.ctakes.assertion.stub.LineAndTokenPosition;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.syntax.NewlineToken;
import org.apache.ctakes.typesystem.type.textspan.Sentence;

public class CharacterOffsetToLineTokenConverterCtakesImpl implements CharacterOffsetToLineTokenConverter {
  protected Logger logger = Logger.getLogger(CharacterOffsetToLineTokenConverterCtakesImpl.class.getName());
  protected JCas jcas;

  protected TreeMap<Integer, Sentence> beginTreeMap;
  protected TreeSet<Integer> tokenBeginEndTreeSet;
  protected Map<Sentence, List<BaseToken>> sentenceToTokenNumberMap;

  public CharacterOffsetToLineTokenConverterCtakesImpl() {

  }

  public CharacterOffsetToLineTokenConverterCtakesImpl(JCas jcas) {
    this.jcas = jcas;
    buildSentenceBoundaryMap();
    buildTokenBoundaryMap();
    buildSentenceToTokenNumberMap();
  }

  public void buildSentenceBoundaryMap() {
    beginTreeMap = new TreeMap<Integer, Sentence>();

    AnnotationIndex<Annotation> annotationIndex = jcas.getAnnotationIndex(Sentence.type);
    for (Annotation current : annotationIndex)
    {
      Sentence currentSentence = (Sentence)current;

      int begin = currentSentence.getBegin();
      beginTreeMap.put(begin, currentSentence);
    }
  }

  public void buildTokenBoundaryMap() {
    tokenBeginEndTreeSet = new TreeSet<Integer>();

    AnnotationIndex<Annotation> annotationIndex = jcas.getAnnotationIndex(BaseToken.type);
    for (Annotation current : annotationIndex)
    {
      BaseToken bt = (BaseToken)current;
      // filter out NewlineToken
      if (!(bt instanceof NewlineToken)) {
        int begin = bt.getBegin();
        int end = bt.getEnd();
        tokenBeginEndTreeSet.add(begin);
        tokenBeginEndTreeSet.add(end);
      }
    }
  }

  protected void buildSentenceToTokenNumberMap() {
    sentenceToTokenNumberMap = new HashMap<Sentence, List<BaseToken>>();
    for (Sentence s : beginTreeMap.values()) {
      FSIterator<Annotation> tokensInSentenceIterator = jcas
          .getAnnotationIndex(BaseToken.type).subiterator(s);
      List<BaseToken> btList = new ArrayList<BaseToken>();
      BaseToken bt = null;
      while ((bt = this.getNextNonEOLToken(tokensInSentenceIterator)) != null) {
        btList.add(bt);
      }
      sentenceToTokenNumberMap.put(s, btList);
    }
  }

  public Sentence findPreviousOrCurrentSentence(int characterOffset) {
    Integer floorKey = beginTreeMap.floorKey(characterOffset);
    if (floorKey == null)
    {
      return null;
    }
    Sentence floorEntry = beginTreeMap.get(floorKey);

    return floorEntry;
  }

  public LineAndTokenPosition convert(int characterOffset) {
    return convertCharacterOffsetToLineToken(characterOffset);
  }

  public int adjustOffsetToBestMatch(int original) {
    logger.debug("inside adjustOffsetToBestMatch");
    Integer newValue = tokenBeginEndTreeSet.floor(original);

    if (newValue == null)
    {
      logger.debug("no previous token begin or end found. using begin of first token.");
      newValue = tokenBeginEndTreeSet.first();
    } else
    {
      if (original == newValue)
        logger.debug("value not adjusted: " + original);
      else
        logger.debug("found previous token boundary. original: " + original + "; new value: " + newValue);
    }

    if (newValue == null)
    {
      logger.info("no previous and no first token found!!");
    }

    logger.debug("end adjustOffsetToBestMatch");

    return newValue;
  }

  public LineAndTokenPosition convertCharacterOffsetToLineToken(int characterOffset) {
    logger.debug("entering CharacterOffsetToLineTokenConverterCtakesImpl.convertCharacterOffsetToLineToken() with a characterOffset of: " + characterOffset);

    logger.debug("before adjusting input character offset...");
    characterOffset = adjustOffsetToBestMatch(characterOffset);
    logger.debug("after adjusting input character offset.");
    int baseTokenTypeId = BaseToken.type;

    ConstraintConstructorFindContainedBy constraintConstructorFindContainedBy = new ConstraintConstructorFindContainedBy(jcas);
    ConstraintConstructorFindContainedWithin constraintConstructorFindContainedWithin = new ConstraintConstructorFindContainedWithin(jcas);

    Type sentenceType = jcas.getTypeSystem().getType(Sentence.class.getName());
    Type baseTokenType = jcas.getTypeSystem().getType(BaseToken.class.getName());

    //    FSIterator<Annotation> filteredIterator =
    //        constraintConstructorFindContainedBy.createFilteredIterator(
    //          characterOffset, characterOffset, sentenceType);
    //
    //    if (!filteredIterator.hasNext())
    //    {
    //      throw new RuntimeException("Surrounding sentence annotation not found[" + characterOffset + "]!!");
    //    }
    //    Annotation sentenceAnnotation = filteredIterator.next();
    //    Sentence sentence = (Sentence)sentenceAnnotation;

    logger.debug("finding current or previous sentence for character offset " + characterOffset);
    Sentence sentence = findPreviousOrCurrentSentence(characterOffset);
    if (sentence == null)
    {
      logger.info("current or previous sentence IS NULL!");
    } else
    {
      logger.debug("current or previous sentence -- id: " + sentence.getAddress() +
          "; begin: " + sentence.getBegin() + 
          "; end: " + sentence.getEnd());
    }

    int lineNumber = sentence.getSentenceNumber() + 1;


    FSIterator<Annotation> beginTokenInSentenceIterator = constraintConstructorFindContainedBy
        .createFilteredIterator(characterOffset, characterOffset,
            baseTokenType);
    BaseToken beginToken = this
        .getNextNonEOLToken(beginTokenInSentenceIterator);
    int beginTokenWordNumber = this.sentenceToTokenNumberMap.get(sentence)
        .indexOf(beginToken);
    LineAndTokenPosition b = new LineAndTokenPosition();
    b.setLine(lineNumber);
    b.setTokenOffset(beginTokenWordNumber);

    return b;
  }

  public BaseToken getNextNonEOLToken(
      FSIterator<Annotation> tokensInSentenceIterator) {
    while (tokensInSentenceIterator.hasNext()) {
      BaseToken bt = (BaseToken) tokensInSentenceIterator.next();
      if (!(bt instanceof NewlineToken)) {
        return bt;
      }
    }
    return null;
  }

  public List<LineAndTokenPosition> calculateBeginAndEndOfConcept
  (ApiConcept problem)
  {
    return calculateBeginAndEndOfConcept(problem.getBegin(), problem.getEnd());
  }

  public List<LineAndTokenPosition> calculateBeginAndEndOfConcept(
      int problemBegin, int problemEnd) {
    // int externalId = problem.getExternalId();
    // int sentenceTypeId = Sentence.type;
    int baseTokenTypeId = BaseToken.type;
    // jcas.getAnnotationIndex(sentenceTypeId);

    ConstraintConstructorFindContainedBy constraintConstructorFindContainedBy = new ConstraintConstructorFindContainedBy(
        jcas);
    ConstraintConstructorFindContainedWithin constraintConstructorFindContainedWithin = new ConstraintConstructorFindContainedWithin(
        jcas);

    // AnnotationIndex<Annotation> sentenceAnnotationIndex =
    // jcas.getAnnotationIndex(sentenceTypeId);
    Type sentenceType = jcas.getTypeSystem().getType(
        Sentence.class.getName());
    Type baseTokenType = jcas.getTypeSystem().getType(
        BaseToken.class.getName());
    // /
    FSIterator<Annotation> filteredIterator = constraintConstructorFindContainedBy
        .createFilteredIterator(problemBegin, problemEnd, sentenceType);
    // /
    ArrayList<LineAndTokenPosition> list = new ArrayList<LineAndTokenPosition>();
    if (filteredIterator.hasNext()) {
      Annotation sentenceAnnotation = filteredIterator.next();
      Sentence sentence = (Sentence) sentenceAnnotation;
      int lineNumber = sentence.getSentenceNumber() + 1;

      // FSIterator<Annotation> tokensInSentenceIterator = jcas
      // .getAnnotationIndex(baseTokenTypeId).subiterator(sentence);
      //
      // if (!tokensInSentenceIterator.hasNext()) {
      // throw new RuntimeException("First token in sentence not found!!");
      // }
      // Annotation firstTokenAnnotation = tokensInSentenceIterator.next();
      // BaseToken firstToken = (BaseToken) firstTokenAnnotation;
      // int firstTokenInSentenceNumber = firstToken.getTokenNumber();

      FSIterator<Annotation> beginTokenInSentenceIterator = constraintConstructorFindContainedWithin
          .createFilteredIterator(problemBegin, problemEnd, baseTokenType);

      // if (!beginTokenInSentenceIterator.hasNext()) {
      // throw new RuntimeException("First token in sentence not found!!");
      // }
      // Annotation beginTokenAnnotation =
      // beginTokenInSentenceIterator.next();
      // BaseToken beginToken = (BaseToken) beginTokenAnnotation;
      // int beginTokenNumber = beginToken.getTokenNumber();
      // int beginTokenWordNumber = beginTokenNumber
      // - firstTokenInSentenceNumber;
      BaseToken beginToken = this
          .getNextNonEOLToken(beginTokenInSentenceIterator);
      int beginTokenWordNumber = this.sentenceToTokenNumberMap.get(sentence)
          .indexOf(beginToken);

      beginTokenInSentenceIterator.moveToLast();
      if (!beginTokenInSentenceIterator.hasNext()) {
        throw new RuntimeException("First token in sentence not found!!");
      }
      Annotation endTokenAnnotation = beginTokenInSentenceIterator.next();
      BaseToken endToken = (BaseToken) endTokenAnnotation;
      // int endTokenNumber = endToken.getTokenNumber();
      // int endTokenWordNumber = endTokenNumber - firstTokenInSentenceNumber;
      int endTokenWordNumber = this.sentenceToTokenNumberMap.get(sentence)
          .indexOf(endToken);

      LineAndTokenPosition b = new LineAndTokenPosition();
      b.setLine(lineNumber);
      b.setTokenOffset(beginTokenWordNumber);
      list.add(b);
      LineAndTokenPosition e = new LineAndTokenPosition();
      e.setLine(lineNumber);
      e.setTokenOffset(endTokenWordNumber);
      list.add(e);
    }
    return list;
  }
}








