/*
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
package org.apache.ctakes.assertion.medfacts.i2b2.api;

import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.FSIntConstraint;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.jcas.JCas;

public class ConstraintConstructorFindContainedWithin extends ConstraintConstructor
{

  public ConstraintConstructorFindContainedWithin()
  {
  }

  public ConstraintConstructorFindContainedWithin(JCas jcas)
  {
    super(jcas);
  }

  @Override
  public FSMatchConstraint constructConstraintByBeginEnd(int problemBegin,
      int problemEnd, ConstraintFactory cf,
      FeaturePath sentenceBeginFeaturePath, FeaturePath sentenceEndFeaturePath)
  {
    return constructContainedByConstraint(problemBegin, problemEnd, cf, sentenceBeginFeaturePath, sentenceEndFeaturePath);
  }

  /**
   * @param problemBegin
   * @param problemEnd
   * @param cf
   * @param sentenceBeginFeaturePath
   * @param sentenceEndFeaturePath
   * @return
   */
  public FSMatchConstraint constructContainedByConstraint(int problemBegin,
      int problemEnd, ConstraintFactory cf,
      FeaturePath sentenceBeginFeaturePath, FeaturePath sentenceEndFeaturePath)
  {
    FSIntConstraint sentenceBeginIntConstraint = cf.createIntConstraint();
    sentenceBeginIntConstraint.geq(problemBegin);
    
    FSIntConstraint sentenceEndIntConstraint = cf.createIntConstraint();
    sentenceEndIntConstraint.leq(problemEnd);
    
    
    FSMatchConstraint begin = cf.embedConstraint(sentenceBeginFeaturePath, sentenceBeginIntConstraint);
    FSMatchConstraint end = cf.embedConstraint(sentenceEndFeaturePath, sentenceEndIntConstraint);
    
    FSMatchConstraint beginAndEnd = cf.and(begin, end);
    return beginAndEnd;
  }


}
