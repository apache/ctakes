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
package org.apache.ctakes.dictionary.lookup.phrasebuilder;

import java.util.List;

import org.apache.ctakes.dictionary.lookup.vo.LookupToken;

/**
 * Interface for building phrases from LookupToken objects.
 * 
 * @author Mayo Clinic
 */
public interface PhraseBuilder
{
    /**
     * Given a list of LookupTokens, one or more phrases are built.
     * 
     * @param lookupTokenList List of LookupTokens.
     * @return One or more String phrases.
     */
    public String[] getPhrases(List<LookupToken> lookupTokenList);
}
