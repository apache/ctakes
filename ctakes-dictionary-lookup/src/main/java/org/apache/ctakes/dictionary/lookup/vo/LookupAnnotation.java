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
package org.apache.ctakes.dictionary.lookup.vo;

/**
 * Value object that models a text annotation.
 * 
 * @author Mayo Clinic
 */
public interface LookupAnnotation
{
    /**
     * Gets the start offset.
     * 
     * @return
     */
    public int getStartOffset();

    /**
     * Gets the end offset.
     * 
     * @return
     */
    public int getEndOffset();

    /**
     * Gets the length of this annotation based on offsets.
     * 
     * @return
     */
    public int getLength();

    /**
     * Gets the text.
     * 
     * @return
     */
    public String getText();
    
    /**
     * Adds an attribute that may be used for filtering.
     * 
     * @param attrKey
     * @param attrVal
     */
    public void addStringAttribute(String attrKey, String attrVal);

    /**
     * Gets an attribute.
     * 
     * @param attrKey
     * @return
     */
    public String getStringAttribute(String attrKey);    
}