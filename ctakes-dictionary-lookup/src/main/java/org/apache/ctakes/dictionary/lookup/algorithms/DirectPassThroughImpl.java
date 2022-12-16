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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.dictionary.lookup.DictionaryEngine;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.phrasebuilder.PhraseBuilder;
import org.apache.ctakes.dictionary.lookup.vo.LookupAnnotation;
import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.ctakes.dictionary.lookup.vo.LookupToken;


/**
 * Each individual LookupToken is referenced against a Dictionary.  
 * 
 * @author Mayo Clinic
 */
public class DirectPassThroughImpl implements LookupAlgorithm
{
    private DictionaryEngine iv_dictEngine;
    private PhraseBuilder iv_phrBuilder;

    /**
     * Constructor
     * @param dictEngine
     * @param phraseBuilder
     */
    public DirectPassThroughImpl(DictionaryEngine dictEngine,
            PhraseBuilder phraseBuilder)
    {
        iv_dictEngine = dictEngine;
        iv_phrBuilder = phraseBuilder;
    }

   /**
    * {@inheritDoc}
    */
   @Override
    public Collection<LookupHit> lookup(final List<LookupToken> lookupTokenList,
                                        final Map<String,List<LookupAnnotation>> contextMap) throws Exception
    {
        List<LookupHit> lhList = new ArrayList<>();
        for (int tokenIdx = 0; tokenIdx < lookupTokenList.size(); tokenIdx++)
        {
            LookupToken lt = lookupTokenList.get(tokenIdx);

            List<LookupToken> singleLtList = new ArrayList<>();
            singleLtList.add(lt);

            String[] strArr = iv_phrBuilder.getPhrases(singleLtList);
            Collection<MetaDataHit> mdhCol = getHits(strArr);

            if ((mdhCol != null) && (mdhCol.size() > 0))
            {
                Iterator<MetaDataHit> mdhMatchItr = mdhCol.iterator();
                while (mdhMatchItr.hasNext())
                {
                    MetaDataHit mdh = mdhMatchItr.next();
                    LookupHit lh = new LookupHit(mdh, lt.getStartOffset(), lt
                            .getEndOffset());
                    lhList.add(lh);
                }
            }
        }
        return lhList;
    }

    private Collection<MetaDataHit> getHits(String[] phrases) throws Exception
    {
        Collection<MetaDataHit> mdhCol = new ArrayList<>();
        for (int i = 0; i < phrases.length; i++)
        {
            Collection<MetaDataHit> curMdhCol = iv_dictEngine.metaLookup(phrases[i]);
            if (curMdhCol.size() > 0)
            {
                mdhCol.addAll(curMdhCol);
            }
        }
        return mdhCol;
    }

}
