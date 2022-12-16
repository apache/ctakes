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
package org.apache.ctakes.assertion.cr;

/**
 * Information about a relation argument.
 * 
 * It typically looks something like this in a knowtator xml file:
 * 
 *   <complexSlotMention id="Relations_Sept21_Schema_Set02_Instance_90018">
 *   <mentionSlot id="Related_to" />
 *   <complexSlotMentionValue value="Relations_Sept21_Schema_Instance_30350" />
 *   </complexSlotMention>
 * 
 * This xml is parsed and stored in this class.
 * 
 * @author dmitriy dligach
 *
 */
public class ArgumentInfo {

  public String value;    // value of "value" attribute above
  public String role; // e.g. "Related_to"
  
  ArgumentInfo(String value, String role) {
    this.value = value;
    this.role = role;
  }
}
