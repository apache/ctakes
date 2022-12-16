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
package org.apache.ctakes.dictionary.lookup.algorithms;

import org.apache.ctakes.dictionary.lookup.vo.LookupAnnotation;
import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.ctakes.dictionary.lookup.vo.LookupToken;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Generic interface to specific lookup algorithm implementations.
 * 
 * @author Mayo Clinic
 */
public interface LookupAlgorithm
{
    /**
     * Lookup the given text specified via LookupToken objects. Any hits will be
     * returned as a collection of LookupHit objects.
     * 
     * @param lookupTokenList
     *            List of LookupTokens, must be sorted.
     * @param contextMap
     *            Map where key=Impl specific String object and value=List of
     *            LookupAnnotation objects
     * @return Collection of LookupHits.
     * @throws Exception
     */
    public Collection<LookupHit> lookup( List<LookupToken> lookupTokenList,
                                         Map<String,List<LookupAnnotation>> contextMap) throws Exception;
}
