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
 * Value object class that describes a modification of document text. This
 * object tracks the original text and the new replacement text.
 */
public class TextModification
{
    private int iv_origStartOffset;
    private int iv_origEndOffset;

    private int iv_newStartOffset;
    private int iv_newEndOffset;
    private String iv_newText;
 
    public TextModification(int origStartOffset, int origEndOffset,
            int newStartOffset, int newEndOffset, String newText)
    {
        iv_origStartOffset = origStartOffset;
        iv_origEndOffset = origEndOffset;
        iv_newStartOffset = newStartOffset;
        iv_newEndOffset = newEndOffset;
        iv_newText = newText;
    }

    public int getNewEndOffset()
    {
        return iv_newEndOffset;
    }

    public int getNewStartOffset()
    {
        return iv_newStartOffset;
    }

    public String getNewText()
    {
        return iv_newText;
    }

    public int getOrigEndOffset()
    {
        return iv_origEndOffset;
    }

    public int getOrigStartOffset()
    {
        return iv_origStartOffset;
    }
}