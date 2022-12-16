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
package org.apache.ctakes.relationextractor.eval;

import java.util.Arrays;

import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.DataWriter;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Holds a set of parameters for a relation extraction model
 */
public class ParameterSettings {

  public Class<? extends DataWriter<String>> dataWriterClass;

  public Object[] configurationParameters;

  public String[] trainingArguments;

  public AnnotationStatistics<String> stats;

  public ParameterSettings(
      Class<? extends DataWriter<String>> dataWriterClass,
      Object[] additionalConfigurationParameters,
      String[] trainingArguments) {
    super();
    this.dataWriterClass = dataWriterClass;
    this.configurationParameters = additionalConfigurationParameters;
    this.trainingArguments = trainingArguments;
  }

  public ParameterSettings(
      Class<? extends DataWriter<String>> dataWriterClass,
      String[] trainingArguments) {
    this(dataWriterClass, new Object[0], trainingArguments);
  }

  @Override
  public String toString() {
    ToStringHelper helper = Objects.toStringHelper(this);
    helper.add("dataWriterClass", this.dataWriterClass.getName());
    helper.add("configurationParameters", Arrays.asList(this.configurationParameters));
    helper.add("trainingArguments", Arrays.asList(this.trainingArguments));
    return helper.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        this.dataWriterClass,
        Arrays.deepHashCode(this.configurationParameters),
        Arrays.hashCode(this.trainingArguments));
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ParameterSettings)) {
      return false;
    }
    ParameterSettings that = (ParameterSettings) obj;
    return Objects.equal(this.dataWriterClass, that.dataWriterClass)
        && Arrays.equals(this.configurationParameters, that.configurationParameters)
        && Arrays.equals(this.trainingArguments, that.trainingArguments);
  }
}