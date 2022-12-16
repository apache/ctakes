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
package org.apache.ctakes.dictionary.lookup.strtable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Mayo Clinic
 */
final public class StringTableRow {

   final private Map<String, String> iv_fieldMap = new HashMap<>();

   public Set<String> getNames() {
      return iv_fieldMap.keySet();
   }

   public Collection<String> getValues() {
      return iv_fieldMap.values();
   }

   public void addField( final String fieldName, final String fieldValue ) {
      iv_fieldMap.put( fieldName, fieldValue );
   }

   public String getFieldValue( final String fieldName ) {
      return iv_fieldMap.get( fieldName );
   }
}
