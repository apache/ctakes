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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.core.resource.JdbcConnectionResource;
import org.apache.ctakes.core.resource.LuceneIndexReaderResource;
import org.apache.ctakes.dictionary.lookup.Dictionary;
import org.apache.ctakes.dictionary.lookup.DictionaryEngine;
import org.apache.ctakes.dictionary.lookup.algorithms.LookupAlgorithm;
import org.apache.ctakes.dictionary.lookup.filter.StringPreLookupFilterImpl;
import org.apache.ctakes.dictionary.lookup.jdbc.JdbcDictionaryImpl;
import org.apache.ctakes.dictionary.lookup.lucene.LuceneDictionaryImpl;
import org.apache.ctakes.dictionary.lookup.strtable.StringTable;
import org.apache.ctakes.dictionary.lookup.strtable.StringTableDictionaryImpl;
import org.apache.ctakes.dictionary.lookup.strtable.StringTableFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * @author Mayo Clinic
 */
public class LookupParseUtilities
{
	//returns a set of LookupSpec objects
	public static Set<LookupSpec> parseDescriptor(File descFile, UimaContext aContext, int maxListSize)
			throws JDOMException, IOException, Exception
	{
		SAXBuilder saxBuilder = new SAXBuilder();
		Document doc = saxBuilder.build(descFile);
		maxSizeList = maxListSize;	//ohnlp-Bugs-3296301 fixes limit the search results to fixed 100 records.
		Map<String, DictionaryEngine> dictMap = parseDictionaries(aContext, doc.getRootElement().getChild(
				"dictionaries"));
		//ohnlp-Bugs-3296301
		return parseLookupBindingXml(aContext, dictMap, doc.getRootElement().getChild("lookupBindings"));
	}

	public static Set<LookupSpec> parseDescriptor(File descFile, UimaContext aContext)
	throws JDOMException, IOException, Exception
	{
		SAXBuilder saxBuilder = new SAXBuilder();
		Document doc = saxBuilder.build(descFile);
		Map<String, DictionaryEngine> dictMap = parseDictionaries(aContext, doc.getRootElement().getChild(
		"dictionaries"));
		//ohnlp-Bugs-3296301
		return parseLookupBindingXml(aContext, dictMap, doc.getRootElement().getChild("lookupBindings"));
	}
	private static Map<String, DictionaryEngine> parseDictionaries(UimaContext aContext,
			Element dictetteersEl) throws AnnotatorContextException, Exception
	{
		Map<String, DictionaryEngine> m = new HashMap<>();
		Iterator<?> dictItr = dictetteersEl.getChildren().iterator();
		while (dictItr.hasNext())
		{
			Element dictEl = (Element) dictItr.next();
			String id = dictEl.getAttributeValue("id");
			DictionaryEngine dictEngine = LookupParseUtilities.parseDictionaryXml(
					aContext,
					dictEl);
			m.put(id, dictEngine);
		}
		return m;
	}

	private static DictionaryEngine parseDictionaryXml(UimaContext annotCtx,
			Element rootDictEl) throws AnnotatorContextException, Exception
	{
		String extResrcKey = rootDictEl.getAttributeValue("externalResourceKey");
		Boolean keepCase = new Boolean(rootDictEl.getAttributeValue("caseSensitive"));
		Object extResrc = annotCtx.getResourceObject(extResrcKey);
		if (extResrc == null)
		{
			throw new Exception("Unable to find external resource with key:"
					+ extResrcKey);
		}

		Element lookupFieldEl = rootDictEl.getChild("lookupField");
		String lookupFieldName = lookupFieldEl.getAttributeValue("fieldName");

		Dictionary dict;

		Element implEl = (Element) rootDictEl.getChild("implementation")
				.getChildren()
				.get(0);
		String implType = implEl.getName();
		if (implType.equals("luceneImpl"))
		{
			if (!(extResrc instanceof LuceneIndexReaderResource))
			{
				throw new Exception("Expected external resource to be:"
						+ LuceneIndexReaderResource.class);
			}
			IndexReader indexReader = ((LuceneIndexReaderResource) extResrc).getIndexReader();
			IndexSearcher indexSearcher = new IndexSearcher(indexReader);
			// Added 'MaxListSize' ohnlp-Bugs-3296301
			dict = new LuceneDictionaryImpl(indexSearcher, lookupFieldName, maxSizeList);
		}
		else if (implType.equals("jdbcImpl"))
		{
			String tableName = implEl.getAttributeValue("tableName");
			if (!(extResrc instanceof JdbcConnectionResource))
			{
				throw new Exception("Expected external resource to be:"
						+ JdbcConnectionResource.class);
			}
			Connection conn = ((JdbcConnectionResource) extResrc).getConnection();
			dict = new JdbcDictionaryImpl(conn, tableName, lookupFieldName);
		}
        else if (implType.equals("csvImpl"))
        {
            String fieldDelimiter = implEl.getAttributeValue("delimiter");            
            if (!(extResrc instanceof FileResource))
            {
                throw new Exception("Expected external resource to be:"
                        + FileResource.class);
            }

            String idxFieldNameStr = implEl.getAttributeValue("indexedFieldNames");
            StringTokenizer st = new StringTokenizer(idxFieldNameStr, ",");
            int arrIdx = 0;
            String[] idxFieldNameArr = new String[st.countTokens()];
            while (st.hasMoreTokens())
            {
                idxFieldNameArr[arrIdx++] = st.nextToken().trim();
            }
            
            File csvFile = ((FileResource) extResrc).getFile();
            try(FileReader fr = new FileReader(csvFile)){
              StringTable strTable = StringTableFactory.build(
                  fr,
                  fieldDelimiter,
                  idxFieldNameArr,
                  true);
              dict = new StringTableDictionaryImpl(strTable, lookupFieldName);
            }
        }
		else
		{
			throw new Exception("Unsupported impl type:" + implType);
		}

		Iterator<?> metaFieldItr = rootDictEl.getChild("metaFields")
				.getChildren()
				.iterator();
		while (metaFieldItr.hasNext())
		{
			Element metaFieldEl = (Element) metaFieldItr.next();
			String metaFieldName = metaFieldEl.getAttributeValue("fieldName");
			dict.retainMetaData(metaFieldName);
		}

		DictionaryEngine dictEngine = new DictionaryEngine(dict, keepCase.booleanValue()); 

	    Element excludeList = rootDictEl.getChild("excludeList");
	    
	    if (excludeList != null && excludeList.getChildren() != null && excludeList.getChildren().size() > 0) {
	    	addExcludeList(dictEngine, excludeList.getChildren().iterator());
	    }

		return dictEngine;
	}

	
	/*
	 * Word(s) not to look up
	 * TODO Consider adding common words as possible performance improvement
	 */
	private static void addExcludeList(DictionaryEngine ge, Iterator<?> itr) {

		HashSet<String> hs = new HashSet<>();
	    
		while(itr.hasNext()) {
			Element item = (Element) itr.next();
			String s = item.getAttributeValue("value");
			System.out.println("Adding exclude value["+s+"]"); // TODO - use logger      
			hs.add(s);
	    }
	    
	    StringPreLookupFilterImpl plf = new StringPreLookupFilterImpl(hs);
	    ge.addPreLookupFilter(plf);
	}

	
	private static Set<LookupSpec> parseLookupBindingXml(UimaContext annotCtx,
			Map<String, DictionaryEngine> dictMap, Element lookupBindingsEl) throws Exception {

		Set<LookupSpec> lsSet = new HashSet<>();
		Iterator<?> itr = lookupBindingsEl.getChildren().iterator();
		while (itr.hasNext())
		{
			Element bindingEl = (Element) itr.next();

			Element dictEl = bindingEl.getChild("dictionaryRef");
			String dictID = dictEl.getAttributeValue("idRef");
			DictionaryEngine dictEngine = dictMap.get(dictID);
			if (dictEngine == null)
			{
				throw new Exception("Dictionary undefined: " + dictID);
			}

			Class<?>[] constrArgs = { UimaContext.class, Properties.class };
			Class<?>[] constrArgsConsum = { UimaContext.class, Properties.class, int.class };//ohnlp-Bugs-3296301
			Class<?>[] constrArgsConsumB = { UimaContext.class, Properties.class };

			Element lookupInitEl = bindingEl.getChild("lookupInitializer");
			String liClassName = lookupInitEl.getAttributeValue("className");
			Element liPropertiesEl = lookupInitEl.getChild("properties");
			Properties liProps = parsePropertiesXml(liPropertiesEl);
			Class<?> liClass = Class.forName(liClassName);
			Constructor<?> liConstr = liClass.getConstructor(constrArgs);
			Object[] liArgs = { annotCtx, liProps };
			LookupInitializer li = (LookupInitializer) liConstr.newInstance(liArgs);

			Element lookupConsumerEl = bindingEl.getChild("lookupConsumer");
			String lcClassName = lookupConsumerEl.getAttributeValue("className");
			Element lcPropertiesEl = lookupConsumerEl.getChild("properties");
			Properties lcProps = parsePropertiesXml(lcPropertiesEl);
			Class<?> lcClass = Class.forName(lcClassName);
			Constructor<?>[] consts = lcClass.getConstructors();
			Constructor<?> lcConstr = null;
			Object[] lcArgs = null;
			for(int i=0;i<consts.length;i++)
			{
			lcConstr = consts[i];
				if (Arrays.equals(constrArgsConsum,lcConstr.getParameterTypes()) )
				{
					lcConstr = lcClass.getConstructor(constrArgsConsum);
					lcArgs = new Object[]{ annotCtx, lcProps, maxSizeList };//ohnlp-Bugs-3296301					
				}
				else if (Arrays.equals(constrArgsConsumB,lcConstr.getParameterTypes()) )
				{
					lcConstr = lcClass.getConstructor(constrArgsConsumB);
					lcArgs = new Object[]{ annotCtx, lcProps };
				}				
			}

			LookupConsumer lc = (LookupConsumer) lcConstr.newInstance(lcArgs);
			LookupAlgorithm la = li.getLookupAlgorithm(dictEngine);

			LookupSpec ls = new LookupSpec(la, li, lc);

			lsSet.add(ls);
		}
		return lsSet;
	}
	/**
	 * Get the maximum list size to be returned from a lucene index
	 * @return maxSizeList
	 */
	public static int getMaxSizeList () {
		return maxSizeList;
	}
	/**
	 * Set the maximum list size to be returned from a lucene index
	 * @return maxSizeList
	 */
	public static void setMaxSizeList (int maxListSize) {
		maxSizeList = maxListSize;
	}
	
	private static Properties parsePropertiesXml(Element propsEl)
	{
		Properties props = new Properties();
		Iterator<?> itr = propsEl.getChildren().iterator();
		while (itr.hasNext())
		{
			Element propEl = (Element) itr.next();
			String key = propEl.getAttributeValue("key");
			String value = propEl.getAttributeValue("value");
			props.put(key, value);
		}
		return props;
	}
	// Added 'maxListSize'.  Size equals max int by default 
	private static int  maxSizeList = Integer.MAX_VALUE; //ohnlp-Bugs-3296301

}