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
package org.apache.ctakes.core.cc;

import org.apache.uima.collection.CasConsumer;

/**
 * Extends the CasConsumer interface to provide a method for getting
 * output in XML form.  A regular CasConsumer is "terminal" in the sense
 * that its output does not get returned to object that initiated the
 * CasConsumer.  This interface allows for "non-terminal" behavior so that
 * the output can be returned.
 *  
 * @author Mayo Clinic
 */
public interface NonTerminalConsumer extends CasConsumer
{
    /**
     * Gets the generated output from a CasConsumer in XML form.
     * @return Output xml in String form.
     */
    String getOutputXml();
}