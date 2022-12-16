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
package org.apache.ctakes.core.ci;

/**
 *  Defines a generic interface for modifying text.
 */
public interface TextModifier
{
    /**
     * Generates modifications for the specified text.
     * 
     * @param text
     *            Original document text.
     * @return Array of TextModification objects that describe the
     *         modifications. Offset values are relative to the String object.
     */
    public TextModification[] modify(String text) throws Exception;
}