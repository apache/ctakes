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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

/**
 * 
 * @author Mayo Clinic
 */
public class StringTableFactory
{
    /**
     * Reads from a character stream. Each line is treated as row in the
     * StringTable.
     * 
     * @param charReader
     * @param delimiter
     * @return
     * @throws IOException
     */
    public static StringTable build(
            Reader charReader,
            String delimiter,
            String indexedFieldName,
            boolean ignoreCase) throws IOException
    {
        return build(charReader, delimiter, indexedFieldName, null, ignoreCase);
    }

    /**
     * Reads from a character stream. Each line is treated as row in the
     * StringTable.
     * 
     * @param charReader
     * @param delimiter
     * @return
     * @throws IOException
     */
    public static StringTable build(
            Reader charReader,
            String delimiter,
            String indexedFieldName,
            FieldConstraint constraint,
            boolean ignoreCase) throws IOException
    {
        String[] indexedFieldNames = new String[1];
        indexedFieldNames[0] = indexedFieldName;
        return build(
                charReader,
                delimiter,
                indexedFieldNames,
                constraint,
                ignoreCase);
    }

    /**
     * Reads from a character stream. Each line is treated as row in the
     * StringTable.
     * 
     * @param charReader
     * @param delimiter
     * @return
     * @throws IOException
     */
    public static StringTable build(
            Reader charReader,
            String delimiter,
            String[] indexedFieldNames,
            boolean ignoreCase) throws IOException
    {
        return build(charReader, delimiter, indexedFieldNames, null, ignoreCase);
    }

    /**
     * Reads from a character stream. Each line is treated as row in the
     * StringTable.
     * 
     * @param charReader
     * @param delimiter
     * @return
     * @throws IOException
     */
    public static StringTable build(
            Reader charReader,
            String delimiter,
            String[] indexedFieldNames,
            FieldConstraint constraint,
            boolean ignoreCase) throws IOException
    {
        StringTable strTable = new StringTable(indexedFieldNames);
        try(BufferedReader br = new BufferedReader(charReader)){
          String line = br.readLine();
          while (line != null)
          {
            StringTableRow strTableRow = new StringTableRow();
            StringTokenizer st = new StringTokenizer(line, delimiter);
            int fieldCnt = 0;
            boolean isConstrained = false;
            while (st.hasMoreTokens())
            {
              String fieldName = String.valueOf(fieldCnt);
              String fieldValue = st.nextToken();
              if (ignoreCase)
              {
                fieldValue = fieldValue.toLowerCase();
              }
              strTableRow.addField(fieldName, fieldValue);
              fieldCnt++;

              if ((constraint != null)
                  && constraint.isConstrained(fieldName, fieldValue))
              {
                isConstrained = true;
              }
            }

            if (!isConstrained)
            {
              strTable.addRow(strTableRow);
            }
            line = br.readLine();
          }
        }

        return strTable;
    }
}