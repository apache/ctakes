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
package org.apache.ctakes.core.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.*;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;

import java.io.File;

/**
 * Oct 2010 - convert to lucene 3.0.2
 * @author Mayo Clinic
 */
public class LuceneIndexReaderResourceImpl
        implements LuceneIndexReaderResource, SharedResourceObject {
	
    // LOG4J logger based on class name
    static private final Logger LOGGER = LoggerFactory.getLogger( "LuceneIndexReaderResourceImpl" );

    private IndexReader iv_indexReader;

    /**
     * Loads a Lucene index for reading.
     */
    public void load(DataResource dr) throws ResourceInitializationException {

        ConfigurationParameterSettings cps = dr.getMetaData().getConfigurationParameterSettings();
        Boolean useMemoryIndex = (Boolean) cps.getParameterValue("UseMemoryIndex");

        String indexDirStr = (String) cps.getParameterValue("IndexDirectory");
        try {

           File indexDir = FileLocator.getFile( indexDirStr );

            if(!indexDir.exists())
               LOGGER.info("indexDir="+indexDirStr+"  does not exist!");
            else
               LOGGER.info("indexDir="+indexDirStr+"  exists.");
            
            if (useMemoryIndex.booleanValue()) {

               LOGGER.info("Loading Lucene Index into memory: " + indexDir);
//                FSDirectory fsd = FSDirectory.open(indexDir.toPath());
//                Directory d = new RAMDirectory(fsd, IOContext.DEFAULT);
//                iv_indexReader = IndexReader.open(d);
               Directory d = new MMapDirectory( indexDir.toPath() );
               iv_indexReader = DirectoryReader.open( d );
            }
            else {
               LOGGER.info("Loading Lucene Index: " + indexDir);
                FSDirectory fsd = FSDirectory.open(indexDir.toPath());
//                iv_indexReader = IndexReader.open(fsd);
               iv_indexReader = DirectoryReader.open( fsd );
            }
           LOGGER.info("Loaded Lucene Index, # docs=" + iv_indexReader.numDocs());
        }
        catch (Exception e) {
        	throw new ResourceInitializationException(e);
        }
    }

    public IndexReader getIndexReader() {
        return iv_indexReader;
    }
}