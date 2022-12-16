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
package org.apache.ctakes.temporal.eval;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLine {

  public static class IntegerRanges {

    private List<Integer> items = new ArrayList<Integer>();

    public List<Integer> getList() {
      return this.items;
    }

    public IntegerRanges(String string) {
      for (String part : string.split("\\s*,\\s*")) {
        Matcher matcher = Pattern.compile("(\\d+)-(\\d+)").matcher(part);
        if (matcher.matches()) {
          int begin = Integer.parseInt(matcher.group(1));
          int end = Integer.parseInt(matcher.group(2));
          for (int i = begin; i <= end; ++i) {
            this.items.add(i);
          }
        } else {
          this.items.add(Integer.parseInt(part));
        }
      }
    }
  }
}
