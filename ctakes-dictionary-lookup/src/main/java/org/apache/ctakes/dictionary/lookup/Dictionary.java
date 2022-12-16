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
package org.apache.ctakes.dictionary.lookup;

import java.util.Collection;

/**
 *
 * @author Mayo Clinic
 */
public interface Dictionary
{
    /**
     * Tells the Dictionary to retain specific bits of metadata for each
     * entry in the Dictionary.
     * @param metaFieldName Name specific to Dictionary implementation.
     */
    public void retainMetaData(String metaFieldName);

    /**
     * Determines whether the Dictionary contains the specified input text.
     * @param text The input text.
     * @return true if Dictionary contains input text, false otherwise.
     * @throws DictionaryException
     */
    public boolean contains(String text) throws DictionaryException;

    /**
     * Gets any meta data entries associated with the specified input text.
     * @param text The input text.
     * @return Collection of MetaDataHit objects.
     * @throws DictionaryException
     */
    public Collection<MetaDataHit> getEntries(String text) throws DictionaryException;
}
