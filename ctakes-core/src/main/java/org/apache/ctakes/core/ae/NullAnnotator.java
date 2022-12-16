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
package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

/**
 * This annotator does nothing.  The process method is overridden but is empty.
 * None of the other methods are overriden.  This annotator may be useful if
 * you are using the CPE GUI and you are required to specify an analysis engine
 * but you don't actually want to specify one.  
 * 
 * @author Mayo Clinic
 *
 */
@PipeBitInfo(
      name = "Null Annotator",
      description = "Does absolutely nothing.",
      role = PipeBitInfo.Role.SPECIAL
)
public class NullAnnotator extends JCasAnnotator_ImplBase
{

   @Override
   public void process( JCas jcas )
       throws AnalysisEngineProcessException
       {}
}