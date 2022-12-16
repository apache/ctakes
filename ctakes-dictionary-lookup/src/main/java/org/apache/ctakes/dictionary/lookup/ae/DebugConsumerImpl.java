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
import java.util.Properties;

import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.log4j.Logger;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;


/**
 * LookupConsumer implementation that outputs debug info to the log about each LookupHit.
 * 
 * @author Mayo Clinic 
 * 
 */
public class DebugConsumerImpl implements LookupConsumer
{
    // LOG4J logger based on class name
    private Logger iv_logger = Logger.getLogger(getClass().getName());

    public DebugConsumerImpl(UimaContext aCtx, Properties props)
    {        
    }
    
    public void consumeHits(JCas jcas, Iterator lookupHitItr)
            throws AnalysisEngineProcessException
    {
        while (lookupHitItr.hasNext())
        {
            LookupHit lh = (LookupHit) lookupHitItr.next();
            int begin = lh.getStartOffset();
            int end = lh.getEndOffset();
            String coveredText = jcas.getDocumentText().substring(begin, end);            
            iv_logger.info("LookupHit offsets=("+begin+","+end+")\tcoveredText="+coveredText);
            
            MetaDataHit mdh = lh.getDictMetaDataHit();
            Iterator nameItr = mdh.getMetaFieldNames().iterator();
            while (nameItr.hasNext())
            {
                String mfName = (String)nameItr.next();
                String mfValue = mdh.getMetaFieldValue(mfName);
                iv_logger.info("\tmetafield="+mfName+"\tvalue="+mfValue);
            }
        }
    }

}
