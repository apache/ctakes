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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import org.apache.ctakes.core.resource.LuceneIndexReaderResource;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.temporary.assertion.AssertionCuePhraseAnnotation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

/**
 * Implementation that takes Rxnorm dictionary lookup hits and stores only the
 * ones that are also present in the Orange Book.
 */
public class AssertionCuePhraseConsumerImpl extends BaseLookupConsumerImpl
		implements LookupConsumer
{
  public static final String CUE_PHRASE_FIELD_NAME = "cuePhrase";
  public static final String CUE_PHRASE_CATEGORY_FIELD_NAME = "cuePhraseCategory";
  public static final String CUE_PHRASE_FAMILY_FIELD_NAME = "cuePhraseFamily";
  public static final String CUE_PHRASE_FIRST_WORD_FIELD_NAME = "cuePhraseFirstWord";

	// LOG4J logger based on class name
	private Logger iv_logger = Logger.getLogger(getClass().getName());

	private final String CODE_MF_PRP_KEY = "codeMetaField";

	private final String CODING_SCHEME_PRP_KEY = "codingScheme";

	private final String LUCENE_FILTER_RESRC_KEY_PRP_KEY = "luceneFilterExtResrcKey";

	private Properties iv_props;

	private IndexSearcher iv_searcher;
	//ohnlp-Bugs-3296301 limits the search results to fixed 100 records.
	// Added 'MaxListSize'
	private int iv_maxHits;

	public AssertionCuePhraseConsumerImpl(UimaContext aCtx, Properties props, int maxListSize)
			throws Exception
	{
		// TODO property validation could be done here
		iv_props = props;
		iv_maxHits = maxListSize;
		String resrcName = iv_props.getProperty(LUCENE_FILTER_RESRC_KEY_PRP_KEY);
		LuceneIndexReaderResource resrc = (LuceneIndexReaderResource) aCtx.getResourceObject(resrcName);
		iv_searcher = new IndexSearcher(resrc.getIndexReader());
	}
	public AssertionCuePhraseConsumerImpl(UimaContext aCtx, Properties props)
	throws Exception
	{
		// TODO property validation could be done here
		iv_props = props;
		String resrcName = iv_props.getProperty(LUCENE_FILTER_RESRC_KEY_PRP_KEY);
		LuceneIndexReaderResource resrc = (LuceneIndexReaderResource) aCtx.getResourceObject(resrcName);
		iv_searcher = new IndexSearcher(resrc.getIndexReader());
		iv_maxHits = Integer.MAX_VALUE;
	}
	public void consumeHits(JCas jcas, Iterator lhItr)
			throws AnalysisEngineProcessException
	{
		Iterator hitsByOffsetItr = organizeByOffset(lhItr);
		while (hitsByOffsetItr.hasNext())
		{
			Collection hitsAtOffsetCol = (Collection) hitsByOffsetItr.next();

			// iterate over the LookupHit objects
			// code is only valid if the covered text is also present in the
			// filter
			Iterator lhAtOffsetItr = hitsAtOffsetCol.iterator();
			int neBegin = -1;
			int neEnd = -1;
			Collection validCodeCol = new HashSet();
			while (lhAtOffsetItr.hasNext())
			{
				LookupHit lh = (LookupHit) lhAtOffsetItr.next();
				neBegin = lh.getStartOffset();
				neEnd = lh.getEndOffset();

				String text = jcas.getDocumentText().substring(
						lh.getStartOffset(),
						lh.getEndOffset());
				text = text.trim().toLowerCase();

				MetaDataHit mdh = lh.getDictMetaDataHit();
				String cuePhrase = mdh.getMetaFieldValue(AssertionCuePhraseConsumerImpl.CUE_PHRASE_FIELD_NAME);
				String cuePhraseFirstWord = mdh.getMetaFieldValue(AssertionCuePhraseConsumerImpl.CUE_PHRASE_FIRST_WORD_FIELD_NAME);
				String cuePhraseCategory = mdh.getMetaFieldValue(AssertionCuePhraseConsumerImpl.CUE_PHRASE_CATEGORY_FIELD_NAME);
				String cuePhraseFamily = mdh.getMetaFieldValue(AssertionCuePhraseConsumerImpl.CUE_PHRASE_FAMILY_FIELD_NAME);
				//String cuePhraseAssertionFamily = mdh.getMetaFieldValue(AssertionCuePhraseConsumerImpl.CUE_PHRASE_ASSERTION_FAMILY_FIELD_NAME);
				
//				String code = mdh.getMetaFieldValue(iv_props.getProperty(CODE_MF_PRP_KEY));
//
//				if (isValid("trade_name", text) || isValid("ingredient", text))
//				{
//					validCodeCol.add(code);
//				}
//				else
//				{
//					iv_logger.warn("Filtered out: "+text);
//				}

	      AssertionCuePhraseAnnotation cuePhraseAnnotation = new AssertionCuePhraseAnnotation(jcas);
	      cuePhraseAnnotation.setBegin(neBegin);
	      cuePhraseAnnotation.setEnd(neEnd);
	      
	      cuePhraseAnnotation.setCuePhrase(cuePhrase);
	      cuePhraseAnnotation.setCuePhraseFirstWord(cuePhraseFirstWord);
	      
	      cuePhraseAnnotation.setCuePhraseCategory(cuePhraseCategory);
	      cuePhraseAnnotation.setCuePhraseAssertionFamily(cuePhraseFamily);
	      
	      cuePhraseAnnotation.addToIndexes();

			}
			


//			if (validCodeCol.size() > 0)
//			{
//				FSArray ocArr = createOntologyConceptArr(jcas, validCodeCol);
//				IdentifiedAnnotation neAnnot = new MedicationMention(jcas); // medication NEs are EventMention
//				neAnnot.setTypeID(CONST.NE_TYPE_ID_DRUG);
//				neAnnot.setBegin(neBegin);
//				neAnnot.setEnd(neEnd);
//				neAnnot.setDiscoveryTechnique(CONST.NE_DISCOVERY_TECH_DICT_LOOKUP);
//				neAnnot.setOntologyConceptArr(ocArr);
//				neAnnot.addToIndexes();
//			}
		}
	}

	/**
	 * For each valid code, a corresponding JCas OntologyConcept object is
	 * created and stored in a FSArray.
	 * 
	 * @param jcas
	 * @param validCodeCol
	 * @return
	 */
	private FSArray createOntologyConceptArr(JCas jcas, Collection validCodeCol)
	{
		FSArray ocArr = new FSArray(jcas, validCodeCol.size());
		int ocArrIdx = 0;
		Iterator validCodeItr = validCodeCol.iterator();
		while (validCodeItr.hasNext())
		{
			String validCode = (String) validCodeItr.next();
			OntologyConcept oc = new OntologyConcept(jcas);
			oc.setCode(validCode);
			oc.setCodingScheme(iv_props.getProperty(CODING_SCHEME_PRP_KEY));

			ocArr.set(ocArrIdx, oc);
			ocArrIdx++;
		}
		return ocArr;
	}

	private boolean isValid(String fieldName, String str)
			throws AnalysisEngineProcessException
	{
		try
		{
			Query q = new TermQuery(new Term(fieldName, str));

            TopDocs topDoc = iv_searcher.search(q, iv_maxHits);
            ScoreDoc[] hits = topDoc.scoreDocs;
            if ((hits != null) && (hits.length > 0))
            {
                return true;
            }
            else
            {
                return false;
            }
		}
		catch (Exception e)
		{
			throw new AnalysisEngineProcessException(e);
		}
	}
}