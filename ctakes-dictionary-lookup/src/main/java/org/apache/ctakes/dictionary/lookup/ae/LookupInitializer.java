/**
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
package org.apache.ctakes.dictionary.lookup.ae;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.dictionary.lookup.DictionaryEngine;
import org.apache.ctakes.dictionary.lookup.algorithms.LookupAlgorithm;
import org.apache.ctakes.dictionary.lookup.vo.LookupAnnotation;
import org.apache.ctakes.dictionary.lookup.vo.LookupToken;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;


/**
 * Defines how to initialize the LookupAnnotator.
 * 
 * @author Mayo Clinic
 */
public interface LookupInitializer
{
    /**
     * Gets an iteration of tokens that represent the finest grain used for a
     * lookup operation. These tokens may be as simple as a single word or
     * perhaps an entire phrase.
     * 
     * @param jcas
     *            Use the JCas to pull out pre-existing feature structures to
     *            build the LookupToken objects. Add attributes to the
     *            LookupToken objects as you see fit.
     * @return Iterator over LookupToken objects.
     * @throws AnnotatorInitializationException
     */
    public Iterator<LookupToken> getLookupTokenIterator(JCas jcas) throws AnnotatorInitializationException;

    /**
     * Gets an iteration of windows. A window is used to scope which LookupToken
     * objects are passed to the LookupAlgorithm.
     * 
     * @param jcas
     *            Use the JCas to pull out pre-existing feature structures to
     *            build LookupAnnotation objects.
     * @return Iterator over org.apache.uima.jcas.tcas.Annotation objects, each
     *         representing a window.
     * @throws AnnotatorInitializationException
     */
    public Iterator<Annotation> getLookupWindowIterator(JCas jcas)
            throws AnnotatorInitializationException;

    /**
     * Gets a list of tokens that we promise to return in sorted order that are constrained by the
     * input annotation.  Puts the onus for sorting performance on the implementing methods since a sort is
     * implicitly required at some point.
     * @param jcas
     * @param annotation
     * @return List over tokens that are in the window specified.
     * @throws AnnotatorInitializationException
     */
    public List<LookupToken> getSortedLookupTokens(JCas jcas, Annotation annotation) throws AnnotatorInitializationException;
    
    /**
     * Gets the LookupAlgorithm to be used to perform the lookup operations.
     * Properties specified from the descriptor will be passed in to customize
     * the behavior of the algorithm.
     * 
     * @param dictEngine
     *            DictionaryEngine that will execute lookup operations.
     * @return LookupAlgorithm that will be used for lookup operations.
     * @throws AnnotatorInitializationException
     */
    public LookupAlgorithm getLookupAlgorithm(DictionaryEngine dictEngine)
            throws AnnotatorInitializationException;

    /**
     * Gets context for the specified window.
     * 
     * @param jcas
     * @param windowBegin
     * @param windowEnd
     * @return
     */
    public Map<String,List<LookupAnnotation>> getContextMap(JCas jcas, int windowBegin, int windowEnd)
            throws AnnotatorInitializationException;
}
